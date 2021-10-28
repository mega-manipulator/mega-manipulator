package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GitLabApiRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.GitLabAssignedMergeRequestListItemWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabMergeRequestApiWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabMergeRequestListItemWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabMergeRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoGraphQlWrapping
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PrActionStatus
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAssignedPullRequests
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAuthoredPullRequests
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAuthoredPullRequests.Result
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetCurrentUser
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetForkRepos
import com.github.jensim.megamanipulator.graphql.generated.gitlab.SingleRepoQuery
import com.github.jensim.megamanipulator.graphql.generated.gitlab.enums.MergeRequestState
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitLabSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import com.intellij.util.containers.isNullOrEmpty
import com.intellij.util.io.encodeUrlQueryParameter
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@Suppress("debtUnusedPrivateMember", "TooManyFunctions", "LoopWithTooManyJumpStatements", "UnusedPrivateMember")
class GitLabClient @NonInjectable constructor(
    project: Project,
    httpClientProvider: HttpClientProvider?,
    localRepoOperator: LocalRepoOperator?,
) {

    constructor(project: Project) : this(
        project = project,
        httpClientProvider = null,
        localRepoOperator = null
    )

    private val httpClientProvider: HttpClientProvider by lazyService(project, httpClientProvider)
    private val localRepoOperator: LocalRepoOperator by lazyService(project, localRepoOperator)

    private val graphQLClientKotlinxSerializer = GraphQLClientKotlinxSerializer()
    private val json: Json = SerializationHolder.readableJson
    private val log = LoggerFactory.getLogger(this.javaClass)

    // https://gitlab.com/-/graphql-explorer
    // https://docs.gitlab.com/ee/api/graphql/reference/index.html#repository

    private fun getClient(searchHost: String, codeHost: String, settings: GitLabSettings) = GraphQLKtorClient(
        url = URL("${settings.baseUrl}/api/graphql"),
        httpClient = httpClientProvider.getClient(searchHost, codeHost, settings),
        serializer = graphQLClientKotlinxSerializer,
    )

    private suspend fun getRepo(
        pullRequest: GitLabMergeRequestWrapper,
        projectId: Long,
        client: HttpClient,
        settings: GitLabSettings
    ): GitLabRepoWrapping {
        // https://docs.gitlab.com/ee/api/projects.html#get-single-project
        // GET /api/v4/projects/:id
        val response = client.get<HttpResponse>("${settings.baseUrl}/api/v4/projects/$projectId") {
            accept(ContentType.Application.Json)
        }
        if (response.status.value >= 300) {
            throw RuntimeException("Failed getting repo ${response.readText()}")
        } else {
            val content = response.readText()
            val project = json.decodeFromString(GitLabProject.serializer(), content)
            return GitLabApiRepoWrapping(
                searchHost = pullRequest.searchHostName(),
                codeHost = pullRequest.codeHostName(),
                gitLabProject = project
            )
        }
    }

    suspend fun getRepo(searchResult: SearchResult, settings: GitLabSettings): GitLabRepoWrapping {
        val client = getClient(searchResult.searchHostName, searchResult.codeHostName, settings)
        val variables = SingleRepoQuery.Variables("${searchResult.project}/${searchResult.repo}")
        val repo: GraphQLClientResponse<SingleRepoQuery.Result> = client.execute(SingleRepoQuery(variables))
        repo.errors?.let {
            log.warn("Errors returned from gitlab query: {}", it)
        }
        return GitLabRepoGraphQlWrapping(
            searchHost = searchResult.searchHostName,
            codeHost = searchResult.codeHostName,
            gitLabProject = repo.data?.project!!
        )
    }

    suspend fun commentPR(comment: String, pullRequest: GitLabMergeRequestWrapper, settings: GitLabSettings) {
        // https://docs.gitlab.com/ee/api/merge_requests.html#comments-on-merge-requests
        // https://docs.gitlab.com/ee/api/notes.html#create-new-issue-note
        // POST /api/v4/projects/:id/merge_requests/:merge_request_iid/notes
        val client: HttpClient = httpClientProvider.getClient(
            searchHostName = pullRequest.searchHostName(),
            codeHostName = pullRequest.codeHostName(),
            settings = settings
        )
        val iid = pullRequest.mergeRequestIid
        val response =
            client.post<HttpResponse>("${settings.baseUrl}/api/v4/projects/${pullRequest.targetProjectId}/merge_requests/$iid/notes") {
                contentType(ContentType.Application.Json)
                body = mapOf("body" to comment)
            }
        if (response.status.value >= 300) {
            log.warn("Failed creatibg comment '${response.readText()}'")
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

    suspend fun deletePrivateRepo(fork: GitLabRepoWrapping, settings: GitLabSettings): PrActionStatus {
        // https://docs.gitlab.com/ee/api/projects.html#delete-project
        // DELETE /api/v4/projects/:id
        if (fork.getProject() != settings.username) {
            return PrActionStatus(
                success = false,
                msg = "Stopped deletion attempt for repo that was not a private fork"
            )
        }
        val client: HttpClient = httpClientProvider.getClient(fork.getSearchHost(), fork.getCodeHost(), settings)
        val projectId = fork.projectId
        val response: HttpResponse = client.delete("${settings.baseUrl}/api/v4/projects/$projectId")
        return if (response.status.value >= 300) {
            val msg =
                "Failed deleting Gitlab repo ${fork.fullPath} http status code ${response.status} and message '${response.readText()}'"
            log.warn(msg)
            PrActionStatus(success = false, msg = msg)
        } else {
            log.info("Deleted private repo ${fork.asPathString()}")
            PrActionStatus(true)
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(
        searchHost: String,
        codeHost: String,
        settings: GitLabSettings
    ): List<GitLabRepoWrapping> {
        log.warn("Feature not yet supported. Get Private fork repos without prs")
        val client: GraphQLKtorClient = getClient(searchHost, codeHost, settings)
        val forksAccumulator: MutableList<GitLabRepoWrapping> = ArrayList()
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
                            val repo = getRepo(
                                SearchResult(
                                    project = repoParts[0],
                                    repo = repoParts[1],
                                    codeHostName = codeHost,
                                    searchHostName = searchHost
                                ),
                                settings
                            )
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

    suspend fun closePr(
        dropFork: Boolean,
        dropBranch: Boolean,
        settings: GitLabSettings,
        pullRequest: GitLabMergeRequestWrapper
    ): PrActionStatus {
        // https://docs.gitlab.com/ee/api/merge_requests.html#delete-a-merge-request
        // DELETE /api/v4/projects/:id/merge_requests/:merge_request_iid
        val client: HttpClient = httpClientProvider.getClient(
            searchHostName = pullRequest.searchHostName(),
            codeHostName = pullRequest.codeHostName(),
            settings = settings
        )
        val response =
            client.delete<HttpResponse>("${settings.baseUrl}/api/v4/projects/${pullRequest.targetProjectId}/merge_requests/${pullRequest.mergeRequestIid}")

        return if (response.status.value >= 300) {
            val msg = "Failed deleting merge request '${response.readText()}'"
            log.warn(msg)
            PrActionStatus(success = false, msg = msg)
        } else {
            log.info("Closed MergeRequest for ${pullRequest.asPathString()} with title '${pullRequest.title()}'")
            val sourceProjectId: Long? = pullRequest.sourceProjectId
            if (dropFork && pullRequest.isFork() && sourceProjectId != null && (sourceProjectId != pullRequest.targetProjectId)) {
                // TODO - verify that there are no other active PRs
                val repo = getRepo(pullRequest, sourceProjectId, client, settings)
                deletePrivateRepo(repo, settings)
            } else if (dropBranch && !pullRequest.isFork() && sourceProjectId != null) {
                // https://docs.gitlab.com/ee/api/branches.html#delete-repository-branch
                // DELETE /api/v4/projects/:id/repository/branches/:branch
                val sourceBranch = pullRequest.fromBranch()
                val branchResponse: HttpResponse =
                    client.delete("${settings.baseUrl}/api/v4/projects/$sourceProjectId/repository/branches/${sourceBranch.encodeUrlQueryParameter()}")
                if (branchResponse.status.value >= 300) {
                    val msg =
                        "Failed deleting branch '$sourceBranch' for '${pullRequest.asPathString()}' due to ${branchResponse.readText()}"
                    log.warn(msg)
                    PrActionStatus(success = false, msg = msg)
                } else {
                    PrActionStatus(true)
                }
            } else {
                PrActionStatus(true)
            }
        }
    }

    suspend fun getAllPrs(
        searchHost: String,
        codeHost: String,
        settings: GitLabSettings,
        limit: Int,
        role: String,
        state: String,
    ): List<GitLabMergeRequestWrapper> = when (role) {
        "assignee" -> getAllReviewPrs(searchHost = searchHost, codeHost = codeHost, settings = settings, state = state, limit = limit)
        "author" -> getAllAuthorPrs(searchHost = searchHost, codeHost = codeHost, settings = settings, state = state, limit = limit)
        else -> throw IllegalArgumentException("Role '$role' is not recognized")
    }

    private suspend fun getAllReviewPrs(
        searchHost: String,
        codeHost: String,
        settings: GitLabSettings,
        state: String,
        limit: Int,
    ): List<GitLabAssignedMergeRequestListItemWrapper> {
        val client: GraphQLKtorClient = getClient(searchHost, codeHost, settings)
        val accumulator: MutableList<GitLabAssignedMergeRequestListItemWrapper> = ArrayList()
        var lastCursor: String? = null
        while (accumulator.size < limit) {
            val vars = GetAssignedPullRequests.Variables(cursor = lastCursor, state = MergeRequestState.valueOf(state))
            val resp: GraphQLClientResponse<GetAssignedPullRequests.Result> =
                client.execute(GetAssignedPullRequests(vars))
            when {
                !resp.errors.isNullOrEmpty() -> {
                    log.warn("Error received from gitlab {}", resp.errors)
                    break
                }
                !resp.data?.currentUser?.assignedMergeRequests?.nodes.isNullOrEmpty() ->
                    resp.data?.currentUser?.assignedMergeRequests?.nodes?.mapNotNull {
                        it?.let {
                            val raw = json.encodeToString(it)
                            accumulator.add(
                                GitLabAssignedMergeRequestListItemWrapper(
                                    searchHost = searchHost,
                                    codeHost = codeHost,
                                    mergeRequest = it,
                                    raw = raw
                                )
                            )
                        }
                    }
                else -> {
                    log.warn("This must have been a ")
                    break
                }
            }
            if (resp.data?.currentUser?.assignedMergeRequests?.pageInfo?.hasNextPage == true &&
                resp.data?.currentUser?.assignedMergeRequests?.pageInfo?.endCursor != null
            ) {
                lastCursor = resp.data?.currentUser?.assignedMergeRequests?.pageInfo?.endCursor
            } else {
                break
            }
        }
        return accumulator
    }

    private suspend fun getAllAuthorPrs(
        searchHost: String,
        codeHost: String,
        settings: GitLabSettings,
        state: String,
        limit: Int,
    ): List<GitLabMergeRequestListItemWrapper> {
        val client: GraphQLKtorClient = getClient(searchHost, codeHost, settings)
        val accumulator: MutableList<GitLabMergeRequestListItemWrapper> = ArrayList()
        var lastCursor: String? = null
        while (accumulator.size < limit) {

            val vars = GetAuthoredPullRequests.Variables(cursor = lastCursor, state = MergeRequestState.valueOf(state))
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
                            accumulator.add(
                                GitLabMergeRequestListItemWrapper(
                                    searchHost = searchHost,
                                    codeHost = codeHost,
                                    mergeRequest = it,
                                    raw = raw
                                )
                            )
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

    suspend fun updatePr(
        newTitle: String,
        newDescription: String,
        settings: GitLabSettings,
        pullRequest: GitLabMergeRequestWrapper
    ): PrActionStatus {
        // https://docs.gitlab.com/ee/api/merge_requests.html#update-mr
        // PUT /api/v4/projects/:id/merge_requests/:merge_request_iid
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val projectId = pullRequest.targetProjectId
        val mergeRequestIid = pullRequest.mergeRequestIid
        val response: HttpResponse =
            client.put("${settings.baseUrl}/api/v4/projects/$projectId/merge_requests/$mergeRequestIid") {
                contentType(ContentType.Application.Json)
                body = mapOf("description" to newDescription, "title" to newTitle)
            }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed updating PR due to: '${response.readText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    @Suppress("ReturnCount")
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
        val response = client.post<HttpResponse>("${settings.baseUrl}/api/v4/projects/${groupRepo.projectId}/fork") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = mapOf<String, String>()
        }
        if (response.status.value >= 300) {
            val status = HttpStatusCode.fromValue(response.status.value)
            throw RuntimeException("Failed forking repo ${repo.asPathString()} due to httpStatus:$status and message: '${response.readText()}'")
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

    suspend fun createPr(
        title: String,
        description: String,
        settings: GitLabSettings,
        repo: SearchResult
    ): GitLabMergeRequestWrapper {
        // https://docs.gitlab.com/ee/api/merge_requests.html#create-mr
        // POST /api/v4/projects/:id/merge_requests
        val gitlabTargetRepo: GitLabRepoWrapping = getRepo(repo, settings)
        val localBranch: String = localRepoOperator.getBranch(repo)!!
        val fork: Pair<String, String>? = localRepoOperator.getForkProject(repo)
        val fromProject = fork?.first ?: repo.project
        val fromRepo = fork?.second ?: repo.repo
        val gitlabSourceRepo = getRepo(SearchResult(fromProject, fromRepo, repo.codeHostName, repo.searchHostName), settings)
        val sourceProjectId = gitlabSourceRepo.projectId

        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val urlString = "${settings.baseUrl}/api/v4/projects/$sourceProjectId/merge_requests"
        val targetProjectIdNumeric: Long = gitlabTargetRepo.projectId
        val body = GitLabMergeRequestRequest(
            source_branch = localBranch,
            target_branch = gitlabTargetRepo.getDefaultBranch()!!,
            target_project_id = targetProjectIdNumeric,
            title = title,
            description = description
        )
        val response: HttpResponse = client.post(urlString) {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            this.body = body
        }
        val content = response.readText()
        if (response.status.value >= 300) {
            log.warn("Failed creating MergeRequest $content") // TODO improve visibility to user
        }
        val mergeRequest: GitLabMergeRequest = json.decodeFromString(GitLabMergeRequest.serializer(), content)
        return GitLabMergeRequestApiWrapper(
            searchHost = repo.searchHostName,
            codeHost = repo.codeHostName,
            mergeRequest = mergeRequest,
            cloneable = GitLabGitCloneableRepoWrapper(source = gitlabSourceRepo, target = gitlabTargetRepo),
            raw = content
        )
    }

    @Suppress("FunctionOnlyReturningConstant")
    fun addDefaultReviewers(settings: GitLabSettings, pullRequest: GitLabMergeRequestListItemWrapper): PrActionStatus {
        return PrActionStatus(false, msg = "Not implemented, may never be..")
    }

    suspend fun approvePr(pullRequest: GitLabMergeRequestWrapper, settings: GitLabSettings): PrActionStatus {
        return setPrOpinion(pullRequest, settings, "approve")
    }

    suspend fun disapprovePr(pullRequest: GitLabMergeRequestWrapper, settings: GitLabSettings): PrActionStatus {
        return setPrOpinion(pullRequest, settings, "unapprove")
    }

    private suspend fun setPrOpinion(pullRequest: GitLabMergeRequestWrapper, settings: GitLabSettings, endpoint: String): PrActionStatus {
        // https://docs.gitlab.com/ee/api/merge_request_approvals.html#unapprove-merge-request
        // POST /api/v4/projects/:id/merge_requests/:merge_request_iid/unapprove
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val response: HttpResponse = client.post("${settings.baseUrl}/api/v4/projects/${pullRequest.targetProjectId}/merge_requests/${pullRequest.mergeRequestIid}/$endpoint") {
            accept(ContentType.Application.Json)
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed $endpoint PR due to: '${response.readText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    suspend fun merge(pullRequest: GitLabMergeRequestWrapper, settings: GitLabSettings): PrActionStatus {
        // https://docs.gitlab.com/ee/api/merge_requests.html#accept-mr
        // PUT /projects/:id/merge_requests/:merge_request_iid/merge
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val response: HttpResponse = client.put("${settings.baseUrl}/api/v4/projects/${pullRequest.targetProjectId}/merge_requests/${pullRequest.mergeRequestIid}/merge") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed disapproving PR due to: '${response.readText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }
}
