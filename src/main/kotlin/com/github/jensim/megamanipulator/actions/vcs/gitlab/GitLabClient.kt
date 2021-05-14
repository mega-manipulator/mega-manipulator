package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GitLabPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.graphql.generated.gitlab.SingleRepoQuery
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitLabSettings
import org.slf4j.LoggerFactory
import java.net.URL

@SuppressWarnings(value = ["UnusedPrivateMember", "TooManyFunctions"])
class GitLabClient(
    private val httpClientProvider: HttpClientProvider,
) {

    private val log = LoggerFactory.getLogger(this.javaClass)

    // https://gitlab.com/-/graphql-explorer
    // https://docs.gitlab.com/ee/api/graphql/reference/index.html#repository

    private fun getClient(searchHost: String, codeHost: String, settings: GitLabSettings) = GraphQLKtorClient(
        url = URL(settings.baseUrl),
        httpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
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

    fun commentPR(comment: String, pullRequest: GitLabPullRequestWrapper, settings: GitLabSettings) {
        TODO("not implemented")
    }

    fun validateAccess(searchHost: String, codeHost: String, settings: GitLabSettings): String {
        TODO("not implemented")
    }

    fun deletePrivateRepo(fork: GitLabRepoWrapping, settings: GitLabSettings) {
        TODO("not implemented")
    }

    fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String, settings: GitLabSettings): List<RepoWrapper> {
        TODO("not implemented")
    }

    fun closePr(dropForkOrBranch: Boolean, settings: GitLabSettings, pullRequest: GitLabPullRequestWrapper) {
        TODO("not implemented")
    }

    fun getAllPrs(searchHost: String, codeHost: String, settings: GitLabSettings): List<PullRequestWrapper> {
        TODO("not implemented")
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
