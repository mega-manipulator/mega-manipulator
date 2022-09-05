package com.github.jensim.megamanipulator.actions.vcs.githubcom

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jensim.megamanipulator.actions.git.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.GithubComPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.GithubComRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PrActionStatus
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GitHubValidation.rateLimitRetry
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.http.unwrap
import com.github.jensim.megamanipulator.project.CoroutinesHolder
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder.objectMapper
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.codehost.GitHubSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

@SuppressWarnings("TooManyFunctions", "ReturnCount")
class GithubComClient @NonInjectable constructor(
    project: Project,
    httpClientProvider: HttpClientProvider?,
    localRepoOperator: LocalRepoOperator?,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    constructor(project: Project) : this(project, null, null)

    private val httpClientProvider: HttpClientProvider by lazyService(project, httpClientProvider)
    private val localRepoOperator: LocalRepoOperator by lazyService(project, localRepoOperator)
    private val coroutineCntx: CoroutineContext = Dispatchers.IO + SupervisorJob()

    fun addDefaultReviewers(
        settings: GitHubSettings,
        pullRequest: GithubComPullRequestWrapper
    ): PrActionStatus {
        return PrActionStatus(
            success = false,
            msg = "Might never implement  ¯\\_(ツ)_/¯ ${settings.username}@${pullRequest.searchHost}/${pullRequest.codeHost}"
        )
    }

    suspend fun createPr(
        title: String,
        description: String,
        settings: GitHubSettings,
        repo: SearchResult
    ): GithubComPullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val fork: Pair<String, String>? = localRepoOperator.getForkProject(repo)
        val localBranch: String = localRepoOperator.getBranch(repo)!!
        val headProject = fork?.first?.let { "$it:" } ?: ""
        val repoUrlString = "${settings.baseUrl}/repos/${repo.project}/${repo.repo}"
        val ghRepo: GithubComRepo = client.get(repoUrlString).unwrap()
        val body = GithubPullRequestRequest(
            title = title,
            body = description,
            head = "$headProject$localBranch",
            base = ghRepo.default_branch,
            draft = false,
            maintainer_can_modify = true,
        )
        val prPostUrlString = "${settings.baseUrl}/repos/${repo.project}/${repo.repo}/pulls"
        val response = rateLimitRetry(true) {
            client.post(prPostUrlString) {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(body)
            }
        }
        val prResponse = response.bodyAsText()
        if (!response.status.isSuccess()) {
            val msg = "Failed creating PR with request! $prResponse from request $prPostUrlString  $body"
            logger.error(msg)
            throw RuntimeException(msg)
        }
        val pr: GithubComPullRequest = objectMapper.readValue(prResponse)
        return GithubComPullRequestWrapper(
            searchHost = repo.searchHostName,
            codeHost = repo.codeHostName,
            pullRequest = pr,
            raw = prResponse
        )
    }

    suspend fun createFork(settings: GitHubSettings, repo: SearchResult): String {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        // According to github docs, the fork process can take up to 5 minutes
        // https://docs.github.com/en/rest/reference/repos#create-a-fork
        val ghrepo: GithubComRepo? = try {
            val tmpRepo: GithubComRepo = rateLimitRetry(false) {
                client.get("${settings.baseUrl}/repos/${settings.username}/${repo.repo}")
            }.unwrap()
            if (tmpRepo.fork && tmpRepo.parent?.owner?.login == repo.project) tmpRepo else null
        } catch (e: Exception) {
            null
        }
        // Recover or create fork
        return if (ghrepo == null) {
            // TODO fix fork repo name
            val previewRepo: GithubComRepo = rateLimitRetry(true) {
                client.post("${settings.baseUrl}/repos/${repo.project}/${repo.repo}/forks") {
                    setBody(emptyMap<String, Any>())
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
            }.unwrap()
            when (settings.cloneType) {
                CloneType.SSH -> previewRepo.ssh_url
                CloneType.HTTPS -> previewRepo.clone_url
            }
        } else {
            when (settings.cloneType) {
                CloneType.SSH -> ghrepo.ssh_url
                CloneType.HTTPS -> ghrepo.clone_url
            }
        }
    }

    suspend fun updatePr(
        newTitle: String,
        newDescription: String,
        settings: GitHubSettings,
        pullRequest: GithubComPullRequestWrapper
    ): PrActionStatus {
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
        val response: HttpResponse = rateLimitRetry(true) {
            client.patch(pullRequest.pullRequest.url) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("title" to newTitle, "body" to newDescription))
            }
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed updating PR due to: '${response.bodyAsText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    suspend fun getAllPrs(
        searchHost: String,
        codeHost: String,
        settings: GitHubSettings,
        limit: Int,
        state: String?,
        role: String?,
        project: String?,
        repo: String?,
    ): List<GithubComPullRequestWrapper> {
        // https://docs.github.com/en/search-github/searching-on-github/searching-issues-and-pull-requests
        val role: String = role?.let { "+$role%3A${settings.username}" } ?: ""
        val state: String = state?.let { "+state%3A$state" } ?: ""

        val repoOwner: String = if (project != null && repo != null) {
            "+repo%3A$project/$repo"
        } else if (project == null && repo == null) {
            ""
        } else if (project != null) {
            "+user%3A$project"
        } else {
            throw IllegalArgumentException("Project cannot be undefined if repo is defined")
        }

        val client: HttpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
        val seq: Flow<GithubComIssue> = flow {
            var page = 1
            var found = 0L
            val perPage = min(100, limit)
            while (true) {
                val result: GithubComSearchResult<GithubComIssue> = rateLimitRetry(false) {
                    client.get("${settings.baseUrl}/search/issues?per_page=$perPage&page=${page++}&q=type%3Apr${state}$role$repoOwner")
                }.unwrap()
                result.items.forEach { emit(it) }
                if (result.items.isEmpty()) break
                found += result.total_count
                if (found > limit) break
            }
        }

        return seq.toList()
            .mapNotNull { it.pull_request?.url }
            .chunked(settings.httpConcurrency ?: 1)
            .flatMap { chunk ->
                val futures = chunk.map {
                    CoroutinesHolder.scope.async {
                        val response: HttpResponse = client.get(it)
                        val prString = response.bodyAsText()
                        val pr: GithubComPullRequest = objectMapper.readValue(prString)
                        GithubComPullRequestWrapper(
                            searchHost = searchHost,
                            codeHost = codeHost,
                            pullRequest = pr,
                            raw = prString,
                        )
                    }
                }
                futures.awaitAll()
            }
    }

    suspend fun closePr(
        dropFork: Boolean,
        dropBranch: Boolean,
        settings: GitHubSettings,
        pullRequest: GithubComPullRequestWrapper
    ): PrActionStatus {
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
        val response: HttpResponse = client.patch(pullRequest.pullRequest.url) {
            contentType(ContentType.Application.Json)
            setBody(mapOf<String, Any>("state" to "closed"))
        }
        if (response.status.value >= 300) {
            return PrActionStatus(false, response.bodyAsText())
        } else {
            if (pullRequest.pullRequest.head?.repo != null) {
                if (dropFork && pullRequest.pullRequest.head.repo.fork && pullRequest.pullRequest.head.repo.id != pullRequest.pullRequest.base?.repo?.id) {
                    if (pullRequest.pullRequest.head.repo.open_issues_count == 0L && pullRequest.pullRequest.head.repo.owner.login == settings.username) {
                        client.delete("${settings.baseUrl}/repos/${settings.username}/${pullRequest.pullRequest.head.repo.name}").let {
                            if (it.status.value >= 300) {
                                return PrActionStatus(false, "Failed dropFork due to ${it.bodyAsText()}")
                            }
                        }
                    }
                } else if (dropBranch && pullRequest.pullRequest.head.repo.id == pullRequest.pullRequest.base?.repo?.id) {
                    // https://docs.github.com/en/rest/reference/git#delete-a-reference
                    client.delete("${settings.baseUrl}/repos/${pullRequest.pullRequest.head.repo.owner.login}/${pullRequest.pullRequest.head.repo.name}/git/refs/heads/${pullRequest.pullRequest.head.ref}").let {
                        if (it.status.value >= 300) {
                            return PrActionStatus(false, "Failed dropBranch due to ${it.bodyAsText()}")
                        }
                    }
                }
            }
            return PrActionStatus(true)
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(
        searchHost: String,
        codeHost: String,
        settings: GitHubSettings
    ): List<GithubComRepoWrapping> = withContext(context = coroutineCntx) {
        val client: HttpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
        val repoFlow: Flow<GithubComRepo> = flow {

            val pageCount = AtomicInteger(1)
            while (true) {
                val response: HttpResponse = rateLimitRetry(false) {
                    client.get("${settings.baseUrl}/users/${settings.username}/repos?page=${pageCount.getAndIncrement()}")
                }
                val page: List<GithubComRepo> = response.unwrap()
                val asyncList: Sequence<List<Deferred<GithubComRepo>>> = page.asSequence()
                    .filter { it.fork }
                    .chunked(settings.httpConcurrency ?: 1)
                    .map { chunk: List<GithubComRepo> ->
                        chunk.map {
                            CoroutinesHolder.scope.async(start = LAZY) {
                                rateLimitRetry(false) {
                                    client.get("${settings.baseUrl}/repos/${settings.username}/${it.name}")
                                }.unwrap()
                            }
                        }
                    }
                asyncList.forEach { chunk ->
                    chunk.awaitAll().forEach { repo ->
                        emit(repo)
                    }
                }
                if (page.isEmpty()) break
            }
        }
        repoFlow.map { GithubComRepoWrapping(searchHost, codeHost, it) }
            .filter { it.repo.fork && it.repo.open_issues_count == 0L }
            .toList()
    }

    suspend fun deletePrivateRepo(fork: GithubComRepoWrapping, settings: GitHubSettings) {
        val client: HttpClient = httpClientProvider.getClient(fork.getSearchHost(), fork.getCodeHost(), settings)
        client.delete("${settings.baseUrl}/repos/${settings.username}/${fork.repo.name}")
    }

    suspend fun getRepo(searchResult: SearchResult, settings: GitHubSettings): GithubComRepoWrapping {
        val client: HttpClient =
            httpClientProvider.getClient(searchResult.searchHostName, searchResult.codeHostName, settings)
        val response: HttpResponse = rateLimitRetry(false) {
            client.get("${settings.baseUrl}/repos/${searchResult.project}/${searchResult.repo}")
        }
        val repo: GithubComRepo = response.unwrap()
        return GithubComRepoWrapping(searchResult.searchHostName, searchResult.codeHostName, repo)
    }

    suspend fun commentPR(comment: String, pullRequest: GithubComPullRequestWrapper, settings: GitHubSettings) {
        // https://docs.github.com/en/rest/reference/pulls#comments
        // https://docs.github.com/en/rest/reference/issues#create-an-issue-comment
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHost, pullRequest.codeHost, settings)
        rateLimitRetry(true) {
            client.post(pullRequest.pullRequest.comments_url) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("body" to comment))
            }
        }
    }

    suspend fun validateAccess(searchHost: String, codeHost: String, settings: GitHubSettings): String? = try {
        val client: HttpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
        GitHubValidation.validateAccess(settings.baseUrl, client)
    } catch (e: Exception) {
        val msg = "Failed setting up client: ${e.message}"
        logger.error(msg, e)
        msg
    }

    suspend fun approvePr(pullRequest: GithubComPullRequestWrapper, settings: GitHubSettings): PrActionStatus {
        return createReview(GithubReviewEvent.APPROVE, pullRequest, settings)
    }

    suspend fun disapprovePr(pullRequest: GithubComPullRequestWrapper, settings: GitHubSettings): PrActionStatus {
        return createReview(GithubReviewEvent.REQUEST_CHANGES, pullRequest, settings)
    }

    private suspend fun createReview(event: GithubReviewEvent, pullRequest: GithubComPullRequestWrapper, settings: GitHubSettings): PrActionStatus {
        // https://docs.github.com/en/rest/reference/pulls#create-a-review-for-a-pull-request
        // POST /repos/{owner}/{repo}/pulls/{pull_number}/reviews

        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val response: HttpResponse = rateLimitRetry(true) {
            client.post("${settings.baseUrl}/repos/${pullRequest.pullRequest.base?.repo?.full_name}/pulls/${pullRequest.pullRequest.id}/reviews") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "event" to event.name,
                        "body" to "Bulk ${event.name.lowercase()} using <a href=\"https://mega-manipulator.github.io/\">mega-manipulator</a>"
                    )
                )
            }
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed ${event.name.lowercase()} PR due to: '${response.bodyAsText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    private enum class GithubReviewEvent {
        APPROVE, REQUEST_CHANGES // , COMMENT, PENDING
    }

    suspend fun merge(pullRequest: GithubComPullRequestWrapper, settings: GitHubSettings): PrActionStatus {
        // https://docs.github.com/en/rest/reference/pulls#merge-a-pull-request
        // PUT /repos/{owner}/{repo}/pulls/{pull_number}/merge
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val response: HttpResponse = rateLimitRetry(true) {
            client.put("${settings.baseUrl}/repos/${pullRequest.pullRequest.base?.repo?.full_name}/pulls/${pullRequest.pullRequest.id}/merge") {
                accept(ContentType.Application.Json)
            }
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed merge PR due to: '${response.bodyAsText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }
}
