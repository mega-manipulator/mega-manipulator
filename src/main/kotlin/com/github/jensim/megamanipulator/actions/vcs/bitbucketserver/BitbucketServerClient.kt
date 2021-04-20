package com.github.jensim.megamanipulator.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.BitBucketPullRequestWrapper
import com.github.jensim.megamanipulator.actions.vcs.BitBucketRepoWrapping
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.CodeHostSettings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.intellij.notification.NotificationType
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

@SuppressWarnings("TooManyFunctions")
class BitbucketServerClient(
    private val httpClientProvider: HttpClientProvider,
    private val localRepoOperator: LocalRepoOperator,
    private val notificationsOperator: NotificationsOperator,
    private val json: Json,
) {
    companion object {
        val instance by lazy {
            BitbucketServerClient(
                httpClientProvider = HttpClientProvider.instance,
                localRepoOperator = LocalRepoOperator.instance,
                notificationsOperator = NotificationsOperator.instance,
                json = SerializationHolder.instance.readableJson,
            )
        }
    }

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): List<BitBucketUser> {
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/reviewers?sourceRepoId=${pullRequest.bitbucketPR.fromRef.repository.id}&targetRepoId=${pullRequest.bitbucketPR.toRef.repository.id}&sourceRefId=${pullRequest.bitbucketPR.fromRef.id}&targetRefId=${pullRequest.bitbucketPR.toRef.id}")
    }

    suspend fun addDefaultReviewers(settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val defaultReviewers = getDefaultReviewers(client, settings, pullRequest).map { BitBucketParticipant(BitBucketUser(it.name)) }
        val all = (pullRequest.bitbucketPR.reviewers + defaultReviewers).distinct()
        val bbPR2 = pullRequest.copy(bitbucketPR = pullRequest.bitbucketPR.copy(reviewers = all))
        return updatePr(client, settings, bbPR2)
    }

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, repo: SearchResult, fromBranchRef: String, toBranchRef: String): List<BitBucketUser> {
        val bitBucketRepo: BitBucketRepo = getRepo(client, settings, repo)
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${repo.project}/repos/${repo.repo}/reviewers?sourceRepoId=${bitBucketRepo.id}&targetRepoId=${bitBucketRepo.id}&sourceRefId=$fromBranchRef&targetRefId=$toBranchRef")
    }

    suspend fun getRepo(searchResult: SearchResult, settings: BitBucketSettings): BitBucketRepoWrapping {
        val client: HttpClient = httpClientProvider.getClient(searchResult.searchHostName, searchResult.codeHostName, settings)
        val repo = getRepo(client, settings, searchResult)
        return BitBucketRepoWrapping(searchResult.searchHostName, searchResult.codeHostName, repo)
    }

    private suspend fun getRepo(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): BitBucketRepo {
        return client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}")
    }

    suspend fun createPr(title: String, description: String, settings: BitBucketSettings, repo: SearchResult): PullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val defaultBranch = client.get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch").id
        val localBranch: String = localRepoOperator.getBranch(repo)!!
        val fork: Pair<String, String>? = localRepoOperator.getForkProject(repo)
        val fromProject = fork?.first ?: repo.project
        val fromRepo = fork?.second ?: repo.repo
        val reviewers = getDefaultReviewers(client, settings, repo, localBranch, defaultBranch)
            .map { BitBucketParticipant(BitBucketUser(name = it.name)) }
        val prRaw: JsonElement = client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/pull-requests") {
            body = BitBucketPullRequestRequest(
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
        }
        val pr: BitBucketPullRequest = json.decodeFromJsonElement(prRaw)
        val prString = json.encodeToString(prRaw)

        return BitBucketPullRequestWrapper(
            searchHost = repo.searchHostName,
            codeHost = repo.codeHostName,
            bitbucketPR = pr,
            raw = prString,
        )
    }

    suspend fun updatePr(newTitle: String, newDescription: String, settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequestWrapper {
        val moddedPullRequest = pullRequest.alterCopy(title = newTitle, body = newDescription)
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        return updatePr(client, settings, moddedPullRequest)
    }

    private suspend fun updatePr(client: HttpClient, settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequestWrapper {
        val prRaw: JsonElement = client.put("${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/pull-requests/${pullRequest.bitbucketPR.id}") {
            body = pullRequest.bitbucketPR
        }
        val pr: BitBucketPullRequest = json.decodeFromJsonElement(prRaw)
        val prString = json.encodeToString(prRaw)

        return BitBucketPullRequestWrapper(
            searchHost = pullRequest.searchHost,
            codeHost = pullRequest.codeHost,
            bitbucketPR = pr,
            raw = prString,
        )
    }

    @Suppress("style.LoopWithTooManyJumpStatements")
    suspend fun getAllPrs(searchHostName: String, codeHostName: String, settings: BitBucketSettings): List<PullRequestWrapper> {
        val client: HttpClient = httpClientProvider.getClient(searchHostName, codeHostName, settings)
        val collector = ArrayList<PullRequestWrapper>()
        var start = 0L
        while (true) {
            val response: BitBucketPage = try {
                client.get("${settings.baseUrl}/rest/api/1.0/dashboard/pull-requests?state=OPEN&role=AUTHOR&start=$start&limit=100")
            } catch (e: Exception) {
                e.printStackTrace()
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

    suspend fun closePr(dropForkOrBranch: Boolean, settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequestWrapper {
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        try {
            val urlString = "${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/pull-requests/${pullRequest.bitbucketPR.id}/decline?version=${pullRequest.bitbucketPR.version}"
            client.post<Unit>(urlString) {
                body = emptyMap<String, String>()
            }
            if (dropForkOrBranch) {
                if (pullRequest.isFork()) {
                    val repository = pullRequest.bitbucketPR.fromRef.repository
                    // Get open PRs
                    val page: BitBucketPage = client.get("${settings.baseUrl}/rest/api/1.0/projects/${repository.project?.key!!}/repos/${repository.slug}/pull-requests?direction=OUTGOING&state=OPEN")
                    if ((page.size ?: 0) == 0) {
                        deletePrivateRepo(BitBucketRepoWrapping(pullRequest.searchHost, pullRequest.codeHost, repository), settings)
                    }
                } else {
                    removeRemoteBranch(settings, pullRequest, client)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            notificationsOperator.show(
                title = "Failed declining PR",
                body = "${e.message}",
                type = NotificationType.ERROR
            )
        }
        return pullRequest
    }

    private suspend fun removeRemoteBranch(settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper, client: HttpClient) {
        // https://docs.atlassian.com/bitbucket-server/rest/5.8.0/bitbucket-branch-rest.html#idm45555984542992
        client.delete<String?>("${settings.baseUrl}/rest/branch-utils/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/branches") {
            body = mapOf<String, Any>("name" to pullRequest.bitbucketPR.fromRef.id, "dryRun" to false)
        }
    }

    suspend fun createFork(settings: BitBucketSettings, repo: SearchResult): String? {
        val client: HttpClient = httpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        val bbRepo: BitBucketRepo = try {
            if (repo.project.toLowerCase() == "~${settings.username.toLowerCase()}") {
                // is private repo
                client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}")
            } else {
                // If repo with prefix already exists..
                client.get("${settings.baseUrl}/rest/api/1.0/users/~${settings.username}/repos/${settings.forkRepoPrefix}${repo.repo}")
            }
        } catch (e: Exception) {
            // Repo does not exist - lets fork
            null
        } ?: client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}") {
            body = BitBucketForkRequest(
                slug = "${settings.forkRepoPrefix}${repo.repo}",
                project = BitBucketProjectRequest(key = "~${settings.username}")
            )
        }
        return bbRepo.links?.clone?.firstOrNull { it.name == "ssh" }?.href
    }

    /**
     * Get all the repos prefixed with the fork-repo-prefix, that do not have open outgoing PRs connected to them
     */
    suspend fun getPrivateForkReposWithoutPRs(searchHostName: String, codeHostName: String, settings: BitBucketSettings): List<BitBucketRepoWrapping> {
        val client: HttpClient = httpClientProvider.getClient(searchHostName, codeHostName, settings)
        var start = 0
        val collector = HashSet<BitBucketRepo>()
        while (true) {
            val page: BitBucketPage = client.get("${settings.baseUrl}/rest/api/1.0/users/~${settings.username}/repos?start=$start")
            page.values.orEmpty()
                .map { json.decodeFromJsonElement<BitBucketRepo>(it) }
                .filter { it.slug.startsWith(settings.forkRepoPrefix) }
                .forEach { collector.add(it) }
            if (page.isLastPage != false) break
            start += page.size ?: 0
        }
        return collector.filter {
            val page: BitBucketPage = client.get("${settings.baseUrl}/rest/api/1.0/projects/${it.project?.key}/repos/${it.slug}/pull-requests?direction=OUTGOING&state=OPEN")
            page.size == 0
        }.map { BitBucketRepoWrapping(searchHostName, codeHostName, it) }
    }

    /**
     * Schedule a repo for deletion, input should be a repo from getPrivateForkReposWithoutPRs
     * @see getPrivateForkReposWithoutPRs
     */
    suspend fun deletePrivateRepo(repo: BitBucketRepoWrapping, settings: BitBucketSettings) {
        val client: HttpClient = httpClientProvider.getClient(repo.getSearchHost(), repo.getCodeHost(), settings)
        client.delete<BitBucketMessage>("${settings.baseUrl}/rest/api/1.0/users/~${settings.username}/repos/${settings.forkRepoPrefix}${repo.getRepo()}") {
            body = emptyMap<String, String>()
        }
    }

    suspend fun commentPR(comment: String, pullRequest: BitBucketPullRequestWrapper, settings: BitBucketSettings) {
        // https://docs.atlassian.com/bitbucket-server/rest/7.10.0/bitbucket-rest.html#idp323
        val client: HttpClient = httpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        client.post<JsonElement>("${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.baseRepo()}/pull-requests/${pullRequest.bitbucketPR.id}/comments") {
            body = BitBucketComment(text = comment)
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
