package com.github.jensim.megamanipulator.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.BitBucketPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.BitBucketRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PrActionStatus
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.BitBucketSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import org.slf4j.LoggerFactory

@SuppressWarnings("TooManyFunctions")
class BitbucketServerClient @NonInjectable constructor(
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

    private val json: Json = SerializationHolder.readableJson
    private val log = LoggerFactory.getLogger(javaClass)

    private suspend fun getDefaultReviewers(
        client: HttpClient,
        settings: BitBucketSettings,
        pullRequest: BitBucketPullRequestWrapper
    ): List<BitBucketUser> {
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/reviewers?sourceRepoId=${pullRequest.bitbucketPR.fromRef.repository.id}&targetRepoId=${pullRequest.bitbucketPR.toRef.repository.id}&sourceRefId=${pullRequest.bitbucketPR.fromRef.id}&targetRefId=${pullRequest.bitbucketPR.toRef.id}")
    }

    suspend fun addDefaultReviewers(
        settings: BitBucketSettings,
        pullRequest: BitBucketPullRequestWrapper
    ): PrActionStatus {
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val defaultReviewers =
            getDefaultReviewers(client, settings, pullRequest).map { BitBucketParticipant(BitBucketUser(it.name)) }
        val all = (pullRequest.bitbucketPR.reviewers + defaultReviewers).distinct()
        val bbPR2 = pullRequest.copy(bitbucketPR = pullRequest.bitbucketPR.copy(reviewers = all))
        return updatePr(client, settings, bbPR2)
    }

    private suspend fun getDefaultReviewers(
        client: HttpClient,
        settings: BitBucketSettings,
        repo: SearchResult,
        sourceRepo: SearchResult = repo,
        fromBranchRef: String,
        toBranchRef: String
    ): List<BitBucketUser> {
        val bitBucketRepo: BitBucketRepo = getRepo(client, settings, repo)
        val bitBucketSourceRepo = if (repo == sourceRepo) bitBucketRepo else {
            getRepo(client, settings, sourceRepo)
        }
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${repo.project}/repos/${repo.repo}/reviewers?sourceRepoId=${bitBucketSourceRepo.id}&targetRepoId=${bitBucketRepo.id}&sourceRefId=$fromBranchRef&targetRefId=$toBranchRef") {
            accept(ContentType.Application.Json)
        }
    }

    suspend fun getRepo(searchResult: SearchResult, settings: BitBucketSettings): BitBucketRepoWrapping {
        val client: HttpClient =
            httpClientProvider.getClient(searchResult.searchHostName, searchResult.codeHostName, settings)
        val repo = getRepo(client, settings, searchResult)
        val defaultBranch =
            client.get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project?.key!!}/repos/${repo.slug}/default-branch") {
                accept(ContentType.Application.Json)
            }.displayId
        return BitBucketRepoWrapping(
            searchHost = searchResult.searchHostName,
            codeHost = searchResult.codeHostName,
            repo = repo,
            defaultBranch = defaultBranch
        )
    }

    private suspend fun getRepo(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): BitBucketRepo {
        return client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}") {
            accept(ContentType.Application.Json)
        }
    }

    suspend fun createPr(
        title: String,
        description: String,
        settings: BitBucketSettings,
        repo: SearchResult
    ): PullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val defaultBranch =
            client.get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch").id
        val localBranch: String = localRepoOperator.getBranch(repo)!!
        val fork: Pair<String, String>? = localRepoOperator.getForkProject(repo)
        val fromProject = fork?.first ?: repo.project
        val fromRepo = fork?.second ?: repo.repo
        val sourceRepo = repo.copy(project = fromProject, repo = fromRepo)
        val reviewers = getDefaultReviewers(
            client = client,
            settings = settings,
            repo = repo,
            sourceRepo = sourceRepo,
            fromBranchRef = localBranch,
            toBranchRef = defaultBranch
        )
            .map { BitBucketParticipant(BitBucketUser(name = it.name)) }
        val request = BitBucketPullRequestRequest(
            title = title,
            description = description,
            fromRef = BitBucketBranchRef(
                id = localBranch,
                repository = BitBucketRepo(
                    slug = fromRepo,
                    project = BitBucketProject(key = fromProject),
                )
            ),
            toRef = BitBucketBranchRef(
                id = defaultBranch,
                repository = BitBucketRepo(
                    slug = repo.repo,
                    project = BitBucketProject(key = repo.project),
                )
            ),
            reviewers = reviewers,
        )
        val prResponse: HttpResponse =
            client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/pull-requests") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = request
            }
        if (prResponse.status.value >= 300) {
            log.error("Not OK response creating Pull Request")
        }
        val prRaw = prResponse.readText()
        val pr: BitBucketPullRequest = json.decodeFromString(BitBucketPullRequest.serializer(), prRaw)

        return BitBucketPullRequestWrapper(
            searchHost = repo.searchHostName,
            codeHost = repo.codeHostName,
            bitbucketPR = pr,
            raw = prRaw,
        )
    }

    suspend fun updatePr(
        newTitle: String,
        newDescription: String,
        settings: BitBucketSettings,
        pullRequest: BitBucketPullRequestWrapper
    ): PrActionStatus {
        val moddedPullRequest = pullRequest.alterCopy(title = newTitle, body = newDescription)
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        return updatePr(client, settings, moddedPullRequest)
    }

    private suspend fun updatePr(
        client: HttpClient,
        settings: BitBucketSettings,
        pullRequest: BitBucketPullRequestWrapper
    ): PrActionStatus {
        val response: HttpResponse = client.put("${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/pull-requests/${pullRequest.bitbucketPR.id}") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = pullRequest.bitbucketPR
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed updating PR due to: '${response.readText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    suspend fun getAllAuthorPrs(
        searchHostName: String,
        codeHostName: String,
        settings: BitBucketSettings
    ): List<PullRequestWrapper> = getAllPrs(
        searchHostName = searchHostName,
        codeHostName = codeHostName,
        settings = settings,
        type = "AUTHOR"
    )

    suspend fun getAllReviewerPrs(
        searchHostName: String,
        codeHostName: String,
        settings: BitBucketSettings
    ): List<PullRequestWrapper> = getAllPrs(
        searchHostName = searchHostName,
        codeHostName = codeHostName,
        settings = settings,
        type = "REVIEWER"
    )

    @Suppress("style.LoopWithTooManyJumpStatements")
    private suspend fun getAllPrs(
        searchHostName: String,
        codeHostName: String,
        settings: BitBucketSettings,
        type: String
    ): List<PullRequestWrapper> {
        val client: HttpClient = httpClientProvider.getClient(searchHostName, codeHostName, settings)
        val collector = ArrayList<PullRequestWrapper>()
        var start = 0L
        while (true) {
            val response: BitBucketPage = try {
                client.get("${settings.baseUrl}/rest/api/1.0/dashboard/pull-requests?state=OPEN&role=$type&start=$start&limit=100") {
                    accept(ContentType.Application.Json)
                }
            } catch (e: Exception) {
                log.error("Failed fetching pull requests from bitbucket", e)
                break
            }
            response.message?.let {
                println("Message received $it")
            }
            collector.addAll(
                response.values.orEmpty().map { raw ->
                    val pr = json.decodeFromJsonElement<BitBucketPullRequest>(raw)
                    val prString = json.encodeToString(raw)
                    BitBucketPullRequestWrapper(
                        searchHost = searchHostName,
                        codeHost = codeHostName,
                        bitbucketPR = pr,
                        raw = prString,
                    )
                }
            )
            if (response.isLastPage != false) {
                break
            } else {
                start += response.size ?: 0
            }
        }
        return collector
    }

    @SuppressWarnings("ReturnCount")
    suspend fun closePr(
        dropFork: Boolean,
        dropBranch: Boolean,
        settings: BitBucketSettings,
        pullRequest: BitBucketPullRequestWrapper
    ): PrActionStatus {
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        try {
            val urlString =
                "${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/pull-requests/${pullRequest.bitbucketPR.id}/decline?version=${pullRequest.bitbucketPR.version}"
            val response = client.post<HttpResponse>(urlString) {
                contentType(ContentType.Application.Json)
                body = emptyMap<String, String>()
            }
            if (response.status.value >= 300) {
                log.error("Failed declining PullRequest ${response.readText()}")
                return PrActionStatus(false)
            }
            if (dropFork && pullRequest.isFork()) {
                val repository = pullRequest.bitbucketPR.fromRef.repository
                // Get open PRs
                val page: BitBucketPage =
                    client.get("${settings.baseUrl}/rest/api/1.0/projects/${repository.project?.key!!}/repos/${repository.slug}/pull-requests?direction=OUTGOING&state=OPEN") {
                        accept(ContentType.Application.Json)
                    }
                if ((page.size ?: 0) == 0) {
                    return deletePrivateRepo(
                        BitBucketRepoWrapping(
                            searchHost = pullRequest.searchHost,
                            codeHost = pullRequest.codeHost,
                            repo = repository,
                            defaultBranch = null,
                        ),
                        settings
                    )
                }
                return PrActionStatus(true)
            } else if (dropBranch && !pullRequest.isFork() && pullRequest.bitbucketPR.fromRef.repository == pullRequest.bitbucketPR.toRef.repository) {
                return removeRemoteBranch(settings, pullRequest, client)
            } else {
                return PrActionStatus(true)
            }
        } catch (e: Exception) {
            log.error("Failed declining PR", e)
            return PrActionStatus(false, e.message)
        }
    }

    private suspend fun removeRemoteBranch(
        settings: BitBucketSettings,
        pullRequest: BitBucketPullRequestWrapper,
        client: HttpClient
    ): PrActionStatus {
        // https://docs.atlassian.com/bitbucket-server/rest/5.8.0/bitbucket-branch-rest.html#idm45555984542992
        val response: HttpResponse = client.delete("${settings.baseUrl}/rest/branch-utils/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/branches") {
            contentType(ContentType.Application.Json)
            body = BitBucketRemoveBranchRequest(name = pullRequest.bitbucketPR.fromRef.id, dryRun = false)
        }
        return if (response.status.value >= 300) {
            PrActionStatus(false, response.readText())
        } else {
            PrActionStatus(true)
        }
    }

    suspend fun createFork(settings: BitBucketSettings, repo: SearchResult): String? {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val bbRepo: BitBucketRepo = try {
            if (repo.project.toLowerCase() == "~${settings.username.toLowerCase()}") {
                // is private repo
                client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}") {
                    accept(ContentType.Application.Json)
                }
            } else {
                // If repo with prefix already exists..
                client.get("${settings.baseUrl}/rest/api/1.0/users/${settings.username}/repos/${repo.repo}") {
                    accept(ContentType.Application.Json)
                }
            }
        } catch (e: Exception) {
            log.warn("Failed finding fork", e)
            // Repo does not exist - lets fork
            null
        } ?: try {
            client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = mapOf<String, String>()
//                body = BitBucketForkRequest(
//                        slug = repo.repo,
//                        project = BitBucketProjectRequest(key = "~${settings.username}")
//                )
            }
        } catch (e: Throwable) {
            log.error("Failed forking repo", e)
            throw e
        }

        return when (settings.cloneType) {
            CloneType.SSH -> bbRepo.links?.clone?.firstOrNull { it.name == "ssh" }?.href
            CloneType.HTTPS -> bbRepo.links?.clone?.firstOrNull { it.name == "http" }?.href
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(
        searchHostName: String,
        codeHostName: String,
        settings: BitBucketSettings
    ): List<BitBucketRepoWrapping> {
        val client: HttpClient = httpClientProvider.getClient(searchHostName, codeHostName, settings)
        var start = 0
        val collector = HashSet<BitBucketRepo>()
        while (true) {
            val urlString = "${settings.baseUrl}/rest/api/1.0/users/${settings.username}/repos?start=$start"
            val page: BitBucketPage = client.get(urlString) {
                accept(ContentType.Application.Json)
            }
            page.values.orEmpty()
                .map { json.decodeFromJsonElement<BitBucketRepo>(it) }
                .filter { it.origin != null }
                .forEach { collector.add(it) }
            if (page.isLastPage != false) break
            start += page.size ?: 0
        }
        return collector.filter {
            // TODO FILTER if repo head or branch commits is ahead of origin
            val page: BitBucketPage =
                client.get("${settings.baseUrl}/rest/api/1.0/projects/${it.project?.key}/repos/${it.slug}/pull-requests?direction=OUTGOING&state=OPEN") {
                    accept(ContentType.Application.Json)
                }
            page.size == 0
        }.map {
            BitBucketRepoWrapping(
                searchHost = searchHostName,
                codeHost = codeHostName,
                repo = it,
                defaultBranch = null,
            )
        }
    }

    /**
     * Schedule a repo for deletion, input should be a repo from getPrivateForkReposWithoutPRs
     * @see getPrivateForkReposWithoutPRs
     */
    suspend fun deletePrivateRepo(repo: BitBucketRepoWrapping, settings: BitBucketSettings): PrActionStatus {
        val client: HttpClient = httpClientProvider.getClient(repo.getSearchHost(), repo.getCodeHost(), settings)
        val response =
            client.delete<HttpResponse>("${settings.baseUrl}/rest/api/1.0/users/${settings.username}/repos/${repo.getRepo()}") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = emptyMap<String, String>()
            }
        return if (response.status.value >= 300) {
            val msg = "Failed deleting private repo ${repo.asPathString()} due to: '${response.readText()}'"
            log.error(msg)
            PrActionStatus(success = false, msg = msg)
        } else {
            PrActionStatus(true)
        }
    }

    suspend fun commentPR(comment: String, pullRequest: BitBucketPullRequestWrapper, settings: BitBucketSettings) {
        // https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp323
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val response =
            client.post<HttpResponse>("${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/pull-requests/${pullRequest.bitbucketPR.id}/comments") {
                contentType(ContentType.Application.Json)
                accept(ContentType.Application.Json)
                body = BitBucketComment(text = comment)
            }
        if (response.status.value >= 300) {
            log.error("Failed commenting due to: '${response.readText()}'")
        }
    }

    suspend fun validateAccess(searchHost: String, codeHost: String, settings: BitBucketSettings): String = try {
        val client: HttpClient = httpClientProvider.getClient(searchHost, codeHost, settings)
        val response = client.get<HttpResponse>("${settings.baseUrl}/rest/api/1.0/inbox/pull-requests/count") {
            accept(ContentType.Application.Json)
        }
        val desc = HttpStatusCode.fromValue(response.status.value).description
        "${response.status.value}:$desc"
    } catch (e: Exception) {
        e.printStackTrace()
        "Client error"
    }

    suspend fun approvePr(pullRequest: BitBucketPullRequestWrapper, settings: BitBucketSettings): PrActionStatus {
        return setPrStatus(pullRequest, settings, BitBucketPullRequestStatus.APPROVED)
    }

    suspend fun disapprovePr(pullRequest: BitBucketPullRequestWrapper, settings: BitBucketSettings): PrActionStatus {
        return setPrStatus(pullRequest, settings, BitBucketPullRequestStatus.NEEDS_WORK)
    }

    private suspend fun setPrStatus(
        pullRequest: BitBucketPullRequestWrapper,
        settings: BitBucketSettings,
        status: BitBucketPullRequestStatus
    ): PrActionStatus {
        // https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp344
        // /rest/api/1.0/projects/{projectKey}/repos/{repositorySlug}/pull-requests/{pullRequestId}/participants/{userSlug}
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val projectKey = pullRequest.bitbucketPR.toRef.repository.project?.key
        val repositorySlug = pullRequest.bitbucketPR.toRef.repository.slug
        val pullRequestId = pullRequest.bitbucketPR.id
        val userSlug = settings.username
        val response: HttpResponse = client.put("${settings.baseUrl}/rest/api/1.0/projects/$projectKey/repos/$repositorySlug/pull-requests/$pullRequestId/participants/$userSlug") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Application.Json)
            body = BitBucketParticipantStatusRequest(
                user = BitBucketUser(name = settings.username),
                approved = status == BitBucketPullRequestStatus.APPROVED,
                status = status
            )
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed setting pr status due to: '${response.readText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    suspend fun merge(pullRequest: BitBucketPullRequestWrapper, settings: BitBucketSettings): PrActionStatus {
        // https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp308
        // /rest/api/1.0/projects/{projectKey}/repos/{repositorySlug}/pull-requests/{pullRequestId}/merge?version
        val client: HttpClient =
            httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val projectKey = pullRequest.bitbucketPR.toRef.repository.project?.key
        val repositorySlug = pullRequest.bitbucketPR.toRef.repository.slug
        val pullRequestId = pullRequest.bitbucketPR.id
        val prVersion = pullRequest.bitbucketPR.version
        val response: HttpResponse = client.post("${settings.baseUrl}/rest/api/1.0/projects/$projectKey/repos/$repositorySlug/pull-requests/$pullRequestId/merge?version=$prVersion") {
            accept(ContentType.Application.Json)
            header("X-Atlassian-Token", "no-check")
        }
        return if (response.status.value >= 300) {
            PrActionStatus(success = false, msg = "Failed merging due to: '${response.readText()}'")
        } else {
            PrActionStatus(success = true)
        }
    }

    /*
    pull requests

        dashboard:
        https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp97

        per repo
        https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp292
    */

    /*
    default reviewers
    https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-default-reviewers-rest.html
     */
}
