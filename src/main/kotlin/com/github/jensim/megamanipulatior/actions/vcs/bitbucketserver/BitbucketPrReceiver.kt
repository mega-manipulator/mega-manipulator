package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.BitBucketPullRequestWrapper
import com.github.jensim.megamanipulatior.actions.vcs.PullRequest
import com.github.jensim.megamanipulatior.http.HttpClientProvider
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import com.intellij.notification.NotificationType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put

object BitbucketPrReceiver {

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): List<BitBucketUser> {
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${pullRequest.project()}/repos/${pullRequest.repo()}/reviewers?sourceRepoId=${pullRequest.bitbucketPR.fromRef.repository.id}&targetRepoId=${pullRequest.bitbucketPR.toRef.repository.id}&sourceRefId=${pullRequest.bitbucketPR.fromRef.id}&targetRefId=${pullRequest.bitbucketPR.toRef.id}")
    }

    suspend fun addDefaultReviewers(settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequest {
        val client: HttpClient = HttpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        val defaultReviewers = getDefaultReviewers(client, settings, pullRequest).map { BitBucketParticipant(BitBucketUser(it.name)) }
        val all = (pullRequest.bitbucketPR.reviewers + defaultReviewers).distinct()
        val bbPR2 = pullRequest.copy(bitbucketPR = pullRequest.bitbucketPR.copy(reviewers = all))
        return updatePr(client, settings, bbPR2)
    }

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, repo: SearchResult, fromBranchRef: String, toBranchRef: String): List<BitBucketUser> {
        val bitBucketRepo: BitBucketRepo = getRepo(client, settings, repo)
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${repo.project}/repos/${repo.repo}/reviewers?sourceRepoId=${bitBucketRepo.id}&targetRepoId=${bitBucketRepo.id}&sourceRefId=${fromBranchRef}&targetRefId=${toBranchRef}")
    }

    private suspend fun getRepo(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): BitBucketRepo {
        return client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}")
    }

    suspend fun createPr(title: String, description: String, settings: BitBucketSettings, repo: SearchResult): PullRequest {
        val client: HttpClient = HttpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)

        val defaultBranch = client.get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch").id
        val localBranch: String = LocalRepoOperator.getBranch(repo)!!
        val reviewers = getDefaultReviewers(client, settings, repo, localBranch, defaultBranch)
            .map { BitBucketParticipant(BitBucketUser(name = it.name)) }
        val pr: BitBucketPullRequest = client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/pull-requests") {
            body = BitBucketPullRequest(
                title = title,
                description = description,
                fromRef = BitBucketBranchRef(
                    id = localBranch,
                    repository = BitBucketRepo(
                        slug = repo.repo,
                        project = BitBucketProject(key = repo.project),
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

        return BitBucketPullRequestWrapper(searchHost = repo.searchHostName, codeHost = repo.codeHostName, pr)
    }

    suspend fun updatePr(settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequest {
        val client: HttpClient = HttpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        return updatePr(client, settings, pullRequest)
    }

    private suspend fun updatePr(client: HttpClient, settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequest {
        val pr: BitBucketPullRequest = client.put("${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.repo()}/pull-requests/${pullRequest.bitbucketPR.id}") {
            body = pullRequest.bitbucketPR.copy(
                author = null,
            )
        }
        return BitBucketPullRequestWrapper(
            searchHost = pullRequest.searchHost,
            codeHost = pullRequest.codeHost,
            bitbucketPR = pr
        )
    }

    suspend fun getAllPrs(searchHostName: String, codeHostName: String, settings: BitBucketSettings): List<PullRequest> {
        val client: HttpClient = HttpClientProvider.getClient(searchHostName, codeHostName, settings)
        val collector = ArrayList<PullRequest>()
        var start = 0L
        while (true) {
            val response: BitBucketDashboardPullRequestResponse = client
                .get("${settings.baseUrl}/rest/api/1.0/dashboard/pull-requests?state=OPEN&role=AUTHOR&start=$start&limit=100")
            collector.addAll(
                response.values.map {
                    BitBucketPullRequestWrapper(searchHostName, codeHostName, it)
                }
            )
            if (response.isLastPage) {
                break
            } else {
                start += response.size
            }
        }
        return collector
    }

    suspend fun closePr(settings: BitBucketSettings, pullRequest: BitBucketPullRequestWrapper): PullRequest {
        val client = HttpClientProvider.getClient(pullRequest.searchHostName(), pullRequest.codeHostName(), settings)
        try {
            val urlString = "${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project()}/repos/${pullRequest.repo()}/pull-requests/${pullRequest.bitbucketPR.id}/decline?version=${pullRequest.bitbucketPR.version}"
            client.post<Unit>(urlString) {
                body = emptyMap<String, String>()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            NotificationsOperator.show(
                title = "Failed declining PR",
                body = "${e.message}",
                type = NotificationType.ERROR
            )
        }
        return pullRequest
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
