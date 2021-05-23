package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GitLabMergeRequestApiWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabMergeRequestListItemWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabMergeRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.graphql.generated.gitlab.CloseMergeRequest
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAuthoredPullRequests
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAuthoredPullRequests.Result
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetCurrentUser
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetForkRepos
import com.github.jensim.megamanipulator.graphql.generated.gitlab.SingleRepoQuery
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitLabSettings
import com.intellij.util.containers.isNullOrEmpty
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Suppress("debtUnusedPrivateMember", "TooManyFunctions", "LoopWithTooManyJumpStatements", "UnusedPrivateMember")
class GitLabClient(
        private val httpClientProvider: HttpClientProvider,
        private val json: Json,
        private val graphQLClientKotlinxSerializer: GraphQLClientKotlinxSerializer,
        private val localRepoOperator: LocalRepoOperator,
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    // https://gitlab.com/-/graphql-explorer
    // https://docs.gitlab.com/ee/api/graphql/reference/index.html#repository

    private fun getClient(searchHost: String, codeHost: String, settings: GitLabSettings) = GraphQLKtorClient(
        url = URL("${settings.baseUrl}/api/graphql"),
        httpClient = httpClientProvider.getClient(searchHost, codeHost, settings),
        serializer = graphQLClientKotlinxSerializer,
    )

    suspend fun getRepo(searchResult: SearchResult, settings: GitLabSettings): GitLabRepoWrapping {
        val client = getClient(searchResult.searchHostName, searchResult.codeHostName, settings)
        val variables = SingleRepoQuery.Variables("${searchResult.project}/${searchResult.repo}")
        val repo: GraphQLClientResponse<SingleRepoQuery.Result> = client.execute(SingleRepoQuery(variables))
        repo.errors?.let {
            log.warn("Errors returned from gitlab query: {}", it)
        }
        return GitLabRepoWrapping(
                searchHost = searchResult.searchHostName,
                codeHost = searchResult.codeHostName,
                repo = repo.data?.project!!
        )
    }

    suspend fun commentPR(comment: String, pullRequest: GitLabMergeRequestListItemWrapper, settings: GitLabSettings) {
        // https://docs.gitlab.com/ee/api/notes.html#create-new-issue-note
        // POST /api/v4/projects/:id/merge_requests/:merge_request_iid/notes
        val client: HttpClient = httpClientProvider.getClient(searchHostName = pullRequest.searchHost, codeHostName = pullRequest.codeHost, settings = settings)
        val project: String = pullRequest.mergeRequest.targetProject.fullPath.encodeURLPath()
        val iid: String = pullRequest.mergeRequest.iid
        client.post<HttpResponse>("${settings.baseUrl}/api/v4/projects/$project/merge_requests/$iid/notes") {
            body = mapOf("body" to comment)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun validateAccess(searchHost: String, codeHost: String, settings: GitLabSettings): String {
        val client = getClient(searchHost, codeHost, settings)
        val user: GraphQLClientResponse<GetCurrentUser.Result> = client.execute(GetCurrentUser())
        return when {
            !user.errors.isNullOrEmpty() -> {
                log.warn("Error from gitlab {}", user.errors)
                "ERROR"
            }
            user.data?.currentUser?.username?.isBlank() == false -> "OK"
            else -> "NO USER DATA"
        }
    }

    suspend fun deletePrivateRepo(fork: GitLabRepoWrapping, settings: GitLabSettings) {
        // https://docs.gitlab.com/ee/api/projects.html#delete-project
        // DELETE /api/v4/projects/:id
        if (fork.repo.path != settings.username){
            throw IllegalArgumentException("Stopped deletion attempt for repo that was not a private fork")
        }
        val client: HttpClient = httpClientProvider.getClient(fork.getSearchHost(), fork.getCodeHost(), settings)
        val projectId = fork.repo.id.encodeURLPath()
        val response:HttpResponse = client.delete("${settings.baseUrl}/api/v4/projects/${projectId}")
        if (response.status != HttpStatusCode.OK) {
            log.warn("Failed deleting Gitlab repo ${fork.repo.fullPath} http status code ${response.status}")
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String, settings: GitLabSettings): List<GitLabRepoWrapping> {
        log.warn("Feature not yet supported. Get Private fork repos without prs")
        val client: GraphQLKtorClient = getClient(searchHost, codeHost, settings)
        val forksAccumulator:MutableList<GitLabRepoWrapping> = ArrayList()
        var lastCursor: String? = null
        while (true) {
            val vars = GetForkRepos.Variables(cursor = lastCursor)
            val resp: GraphQLClientResponse<GetForkRepos.Result> = client.execute(GetForkRepos(vars))
            when {
                !resp.errors.isNullOrEmpty() -> {
                    log.warn("Error received from gitlab {}", resp.errors)
                    break
                }
                !resp.data?.currentUser?.projectMemberships?.nodes.isNullOrEmpty() ->
                    resp.data?.currentUser?.projectMemberships?.nodes?.mapNotNull { member ->
                        member?.project?.let { project ->
                            val repoParts = project.fullPath.split("/")
                            val repo = getRepo(SearchResult(project = repoParts[0], repo = repoParts[1], codeHostName = codeHost, searchHostName = searchHost),settings)
                            forksAccumulator.add(repo)
                        }
                    }
                else -> {
                    log.warn("No errors no data...?")
                    break
                }
            }
            if (resp.data?.currentUser?.projectMemberships?.pageInfo?.hasNextPage == true &&
                    resp.data?.currentUser?.projectMemberships?.pageInfo?.endCursor != null
            ) {
                lastCursor = resp.data?.currentUser?.projectMemberships?.pageInfo?.endCursor
            } else {
                break
            }
        }
        return forksAccumulator
    }

    suspend fun closePr(dropFork: Boolean, dropBranch: Boolean, settings: GitLabSettings, pullRequest: GitLabMergeRequestListItemWrapper) {
        // https://docs.gitlab.com/ee/api/merge_requests.html#delete-a-merge-request
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
        val graphQLClient = GraphQLKtorClient(url = URL("${settings.baseUrl}/api/graphql"), httpClient = client)
        val response = graphQLClient.execute(
                CloseMergeRequest(
                        CloseMergeRequest.Variables(
                                projectPath = pullRequest.mergeRequest.targetProject.fullPath,
                                iid = pullRequest.mergeRequest.iid
                        )
                )
        )
        if (response.errors.isNullOrEmpty() && response.data?.mergeRequestUpdate?.errors.isNullOrEmpty()) {
            if (dropFork && pullRequest.isFork() &&
                pullRequest.mergeRequest.sourceProject?.fullPath?.takeWhile { it != '/' } == settings.username
            ) {
                val projectParts = pullRequest.mergeRequest.sourceProject.fullPath.split("/")
                val repo = getRepo(SearchResult(project = projectParts[0], repo = projectParts[1], codeHostName = pullRequest.codeHost, searchHostName = pullRequest.searchHost), settings)
                deletePrivateRepo(repo, settings)
            } else if (dropBranch && !pullRequest.isFork()) {
                // https://docs.gitlab.com/ee/api/branches.html#delete-repository-branch
                // DELETE /api/v4/projects/:id/repository/branches/:branch
                val sourceProjectId = pullRequest.mergeRequest.sourceProject?.id?.encodeURLPath()
                val sourceBranch = pullRequest.mergeRequest.sourceBranch.encodeURLPath()
                val branchResponse: HttpResponse = client.delete("${settings.baseUrl}/api/v4/projects/${sourceProjectId}/repository/branches/${sourceBranch}")
                if (branchResponse.status != HttpStatusCode.OK) {
                    log.warn("Failed deleting branch '$sourceBranch' for '${pullRequest.asPathString()}'")
                }
            }
        }
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String, settings: GitLabSettings): List<GitLabMergeRequestListItemWrapper> {
        val client: GraphQLKtorClient = getClient(searchHost, codeHost, settings)
        val accumulator: MutableList<GitLabMergeRequestListItemWrapper> = ArrayList()
        var lastCursor: String? = null
        while (true) {
            val vars = GetAuthoredPullRequests.Variables(cursor = lastCursor)
            val resp: GraphQLClientResponse<Result> = client.execute(GetAuthoredPullRequests(vars))
            when {
                !resp.errors.isNullOrEmpty() -> {
                    log.warn("Error received from gitlab {}", resp.errors)
                    break
                }
                !resp.data?.currentUser?.authoredMergeRequests?.nodes.isNullOrEmpty() ->
                    resp.data?.currentUser?.authoredMergeRequests?.nodes?.mapNotNull {
                        it?.let {
                            val raw = json.encodeToString(it)
                            accumulator.add(GitLabMergeRequestListItemWrapper(searchHost = searchHost, codeHost = codeHost, mergeRequest = it, raw = raw))
                        }
                    }
                else -> {
                    log.warn("This must have been a ")
                    break
                }
            }
            if (resp.data?.currentUser?.authoredMergeRequests?.pageInfo?.hasNextPage == true &&
                    resp.data?.currentUser?.authoredMergeRequests?.pageInfo?.endCursor != null
            ) {
                lastCursor = resp.data?.currentUser?.authoredMergeRequests?.pageInfo?.endCursor
            } else {
                break
            }
        }
        return accumulator
    }

    suspend fun updatePr(newTitle: String, newDescription: String, settings: GitLabSettings, pullRequest: GitLabMergeRequestWrapper): GitLabMergeRequestApiWrapper {
        // https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
        // PUT /api/v4/projects/:id/merge_requests/:merge_request_iid
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val projectId = pullRequest.targetProjectId.encodeURLPath()
        val mergeRequestId = pullRequest.mergeRequestId.encodeURLPath()
        val response: HttpResponse = client.put("${settings.baseUrl}/api/v4/projects/$projectId/merge_requests/$mergeRequestId") {
            body = mapOf("description" to newDescription, "title" to newTitle)
        }
        if (response.status != HttpStatusCode.OK) {
            log.warn("Failed updating MergeRequest ") // TODO improve visibility to user
        }
        val content = response.readText()
        val mergeRequest: GitLabMergeRequest = json.decodeFromString(GitLabMergeRequest.serializer(), content)
        return GitLabMergeRequestApiWrapper(
                searchHost = pullRequest.searchHostName(),
                codeHost = pullRequest.codeHostName(),
                mergeRequest = mergeRequest,
                cloneable = pullRequest,
                raw = content
        )
    }

    suspend fun createFork(settings: GitLabSettings, repo: SearchResult): String? {
        // https://docs.gitlab.com/ee/api/projects.html#fork-project
        try {
            getRepo(searchResult = repo.copy(project = settings.username), settings = settings).let {
                return it.getCloneUrl(settings.cloneType)
            }
        } catch (e: NullPointerException) {
            log.info("No fork available for repo $repo")
        }
        val groupRepo: GitLabRepoWrapping = getRepo(searchResult = repo, settings = settings)
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        client.post<Void>("${settings.baseUrl}/api/v4/projects/${groupRepo.repo.id.encodeURLPath()}/fork") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = mapOf<String, String>()
        }
        val counter = AtomicInteger()
        while (counter.getAndIncrement() < 30) {
            delay(1000)
            try {
                getRepo(searchResult = repo.copy(project = settings.username), settings = settings).let {
                    return it.getCloneUrl(settings.cloneType)
                }
            } catch (e: NullPointerException) {
                log.debug("No fork available for repo $repo")
            }
        }
        log.warn("No fork available for repo after 30 seconds $repo")
        return null
    }

    suspend fun createPr(title: String, description: String, settings: GitLabSettings, repo: SearchResult): GitLabMergeRequestWrapper {
        // https://docs.gitlab.com/ee/api/merge_requests.html#create-mr
        // POST /api/v4/projects/:id/merge_requests
        val gitlabTargetRepo: GitLabRepoWrapping = getRepo(repo, settings)
        val localBranch: String = localRepoOperator.getBranch(repo)!!
        val fork: Pair<String, String>? = localRepoOperator.getForkProject(repo)
        val fromProject = fork?.first ?: repo.project
        val fromRepo = fork?.second ?: repo.repo
        val gitlabSourceRepo = getRepo(SearchResult(fromProject, fromRepo, repo.codeHostName, repo.searchHostName), settings)
        val sourceProjectId = gitlabSourceRepo.repo.id.encodeURLPath()

        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val response: HttpResponse = client.post("${settings.baseUrl}/api/v4/projects/${sourceProjectId}/merge_requests") {
            body = mapOf(
                    "source_branch" to localBranch,
                    "target_branch" to gitlabTargetRepo,
                    "target_project_id" to gitlabTargetRepo.repo.id,
                    "title" to title,
                    "description" to description
            )
        }
        if (response.status != HttpStatusCode.OK) {
            log.warn("Failed creating MergeRequest") // TODO improve visibility to user
        }
        val content = response.readText()
        val mergeRequest: GitLabMergeRequest = json.decodeFromString(GitLabMergeRequest.serializer(), content)
        return GitLabMergeRequestApiWrapper(
                searchHost = repo.searchHostName,
                codeHost = repo.codeHostName,
                mergeRequest = mergeRequest,
                cloneable = GitLabGitCloneableRepoWrapper(source = gitlabSourceRepo, target = gitlabTargetRepo),
                raw = content
        )
    }

    fun addDefaultReviewers(settings: GitLabSettings, pullRequest: GitLabMergeRequestListItemWrapper): GitLabMergeRequestListItemWrapper? {
        // TODO("not implemented")
        return null
    }
}
