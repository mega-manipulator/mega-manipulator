package com.github.jensim.megamanipulator.actions.vcs.githubcom

import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GithubComPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GithubComRepoWrapping
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.SerializationHolder
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import java.util.concurrent.atomic.AtomicInteger

class GithubComClient(
    private val httpClientProvider: HttpClientProvider,
    private val localRepoOperator: LocalRepoOperator,
    private val json: Json,
) {

    companion object {
        val instance by lazy {
            GithubComClient(
                httpClientProvider = HttpClientProvider.instance,
                localRepoOperator = LocalRepoOperator.instance,
                json = SerializationHolder.instance.readableJson
            )
        }
    }

    fun addDefaultReviewers(settings: GitHubSettings, pullRequest: GithubComPullRequestWrapper): GithubComPullRequestWrapper {
        throw UnsupportedOperationException("Might never implement  ¯\\_(ツ)_/¯ ${settings.username}@${pullRequest.searchHost}/${pullRequest.codeHost}")
    }

    suspend fun createPr(title: String, description: String, settings: GitHubSettings, repo: SearchResult): GithubComPullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val fork: Pair<String, String>? = localRepoOperator.getForkProject(repo)
        val localBranch: String = localRepoOperator.getBranch(repo)!!
        val headProject = fork?.first ?: repo.project
        val ghRepo: GithubComRepo = client.get("${settings.baseUrl}/repos/${repo.project}/${repo.repo}")
        val prRaw: JsonElement = client.post("${settings.baseUrl}/repos/${repo.project}/${repo.repo}/pulls") {
            body = GithubPullRequestRequest(
                title = title,
                body = description,
                head = "$headProject:$localBranch",
                base = ghRepo.default_branch,
            )
        }
        val pr: GithubComPullRequest = json.decodeFromJsonElement(prRaw)
        val prString = json.encodeToString(prRaw)
        return GithubComPullRequestWrapper(
            searchHost = repo.searchHostName,
            codeHost = repo.codeHostName,
            pullRequest = pr,
            raw = prString
        )
    }

    suspend fun createFork(settings: GitHubSettings, repo: SearchResult): String {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        // According to github docs, the fork process can take up to 5 minutes
        // https://docs.github.com/en/rest/reference/repos#create-a-fork
        val ghrepo: GithubComRepo? = try {
            val tmpRepo: GithubComRepo = client.get("${settings.baseUrl}/repos/${settings.username}/${repo.repo}")
            if (tmpRepo.fork && tmpRepo.parent?.owner?.login == repo.project) tmpRepo else null
        } catch (e: Exception) {
            null
        }
        // Recover or create fork
        return if (ghrepo == null) {
            // TODO fix fork repo name
            val previewRepo: GithubComRepo = client.post("${settings.baseUrl}/repos/${repo.project}/${repo.repo}/forks") { emptyMap<String, Any>() }
            previewRepo.ssh_url
        } else {
            ghrepo.ssh_url
        }
    }

    suspend fun updatePr(newTitle: String, newDescription: String, settings: GitHubSettings, pullRequest: GithubComPullRequestWrapper): GithubComPullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
        val prRaw: JsonElement = client.patch(pullRequest.pullRequest.url) {
            body = mapOf("title" to newTitle, "body" to newDescription)
        }
        val pr: GithubComPullRequest = json.decodeFromJsonElement(prRaw)
        val prString = json.encodeToString(prRaw)
        return GithubComPullRequestWrapper(
            searchHost = pullRequest.searchHost,
            codeHost = pullRequest.codeHost,
            pullRequest = pr,
            raw = prString,
        )
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String, settings: GitHubSettings): List<GithubComPullRequestWrapper> {
        val client: HttpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
        val seq: Flow<GithubComIssue> = flow {
            val page = AtomicInteger(0)
            while (true) {
                val result: GithubComSearchResult<GithubComIssue> = client.get("${settings.baseUrl}/search/issues?page=${page.getAndIncrement()}&q=state%3Aopen+author%3A${settings.username}+type%3Apr")
                result.items.forEach { emit(it) }
                if (result.items.isEmpty()) break
            }
        }
        return seq.toList().toList()
            .mapNotNull { it.pull_request?.url }
            .map {
                val prRaw: JsonElement = client.get(it)
                val pr: GithubComPullRequest = json.decodeFromJsonElement(prRaw)
                val prString = json.encodeToString(prRaw)
                GithubComPullRequestWrapper(
                    searchHost = searchHost,
                    codeHost = codeHost,
                    pullRequest = pr,
                    raw = prString,
                )
            }
    }

    suspend fun closePr(dropForkOrBranch: Boolean, settings: GitHubSettings, pullRequest: GithubComPullRequestWrapper) {
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
        val updatedPr: GithubComPullRequest = client.patch(pullRequest.pullRequest.url) {
            body = mapOf<String, Any>("state" to "closed")
        }
        if (dropForkOrBranch) {
            if (updatedPr.head?.repo != null) {
                if (updatedPr.head.repo.fork && updatedPr.head.repo.id != updatedPr.base?.repo?.id) {
                    if (updatedPr.head.repo.open_issues_count == 0L && updatedPr.head.repo.owner.login == settings.username) {
                        client.delete<String?>("${settings.baseUrl}/repos/${settings.username}/${updatedPr.head.repo.name}")
                    }
                } else {
                    client.delete<String?>("${settings.baseUrl}/repos/${updatedPr.head.repo.owner.login}/${updatedPr.head.repo.name}/git/refs/${updatedPr.head.ref}")
                }
            }
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String, settings: GitHubSettings): List<GithubComRepoWrapping> {
        val client: HttpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
        val repoFlow: Flow<GithubComRepo> = flow {
            val pageCount = AtomicInteger(0)
            while (true) {
                val page: List<GithubComRepo> = client.get("${settings.baseUrl}/users/${settings.username}/repos?page=${pageCount.getAndIncrement()}")
                page.forEach { emit(it) }
                if (page.isEmpty()) break
            }
        }
        return repoFlow.map { GithubComRepoWrapping(searchHost, codeHost, it) }
            .filter { it.repo.fork && it.repo.open_issues_count == 0L }
            .toList()
    }

    suspend fun deletePrivateRepo(fork: GithubComRepoWrapping, settings: GitHubSettings) {
        val client: HttpClient = httpClientProvider.getClient(fork.getSearchHost(), fork.getCodeHost(), settings)
        client.delete<String?>("${settings.baseUrl}/repos/${settings.username}/${fork.repo.name}")
    }

    suspend fun getRepo(searchResult: SearchResult, settings: GitHubSettings): GithubComRepoWrapping {
        val client: HttpClient = httpClientProvider.getClient(searchResult.searchHostName, searchResult.codeHostName, settings)
        val repo: GithubComRepo = client.get("${settings.baseUrl}/repos/${searchResult.project}/${searchResult.repo}")
        return GithubComRepoWrapping(searchResult.searchHostName, searchResult.codeHostName, repo)
    }
}
