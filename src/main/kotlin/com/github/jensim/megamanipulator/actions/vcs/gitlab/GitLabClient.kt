package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GitLabPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.graphql.generated.gitlab.CloseMergeRequest
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAuthoredPullRequests
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetAuthoredPullRequests.Result
import com.github.jensim.megamanipulator.graphql.generated.gitlab.GetCurrentUser
import com.github.jensim.megamanipulator.graphql.generated.gitlab.SingleRepoQuery
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitLabSettings
import com.intellij.util.containers.isNullOrEmpty
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.encodeURLPath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.net.URL

@Suppress("debtUnusedPrivateMember", "TooManyFunctions", "LoopWithTooManyJumpStatements", "UnusedPrivateMember")
class GitLabClient(
    private val httpClientProvider: HttpClientProvider,
    private val json: Json,
    private val graphQLClientKotlinxSerializer: GraphQLClientKotlinxSerializer,
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    // https://gitlab.com/-/graphql-explorer
    // https://docs.gitlab.com/ee/api/graphql/reference/index.html#repository

    private fun getClient(searchHost: String, codeHost: String, settings: GitLabSettings) = GraphQLKtorClient(
        url = URL("${settings.baseUrl}/api/graphql"),
        httpClient = httpClientProvider.getClient(searchHost, codeHost, settings),
        serializer = graphQLClientKotlinxSerializer,
    )

    suspend fun getRepo(searchResult: SearchResult, settings: GitLabSettings): RepoWrapper {
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

    suspend fun commentPR(comment: String, pullRequest: GitLabPullRequestWrapper, settings: GitLabSettings) {
        // https://docs.gitlab.com/ee/api/notes.html#create-new-issue-note
        // POST /projects/:id/merge_requests/:merge_request_iid/notes
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

    fun deletePrivateRepo(fork: GitLabRepoWrapping, settings: GitLabSettings) {
        // https://docs.gitlab.com/ee/api/projects.html#delete-project
        // DELETE /projects/:id
        TODO("not implemented")
    }

    fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String, settings: GitLabSettings): List<RepoWrapper> {
        TODO("not implemented")
    }

    suspend fun closePr(dropFork: Boolean, dropBranch: Boolean, settings: GitLabSettings, pullRequest: GitLabPullRequestWrapper) {
        val client = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
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
                TODO()
            } else if (dropBranch && !pullRequest.isFork()) {
                TODO()
            }
        }
        TODO("not implemented")
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String, settings: GitLabSettings): List<PullRequestWrapper> {
        val client = getClient(searchHost, codeHost, settings)
        val accumulator: MutableList<PullRequestWrapper> = ArrayList()
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
                            accumulator.add(GitLabPullRequestWrapper(searchHost, codeHost, it, raw))
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

    fun updatePr(newTitle: String, newDescription: String, settings: GitLabSettings, pullRequest: GitLabPullRequestWrapper): PullRequestWrapper? {
        TODO("not implemented")
    }

    fun createFork(settings: GitLabSettings, repo: SearchResult): String? {
        TODO("not implemented")
    }

    fun createPr(title: String, description: String, settings: GitLabSettings, repo: SearchResult): PullRequestWrapper? {
        TODO("not implemented")
    }

    fun addDefaultReviewers(settings: GitLabSettings, pullRequest: GitLabPullRequestWrapper): PullRequestWrapper? {
        TODO("not implemented")
    }
}
