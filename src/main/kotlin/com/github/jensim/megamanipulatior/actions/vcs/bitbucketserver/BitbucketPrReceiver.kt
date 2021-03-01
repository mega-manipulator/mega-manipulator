package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.BranchRef
import com.github.jensim.megamanipulatior.actions.vcs.PrReceiver
import com.github.jensim.megamanipulatior.actions.vcs.PullRequest
import com.github.jensim.megamanipulatior.actions.vcs.Reviewer
import com.github.jensim.megamanipulatior.http.HttpClientProvider
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import com.intellij.notification.NotificationType
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import java.io.File

object BitbucketPrReceiver : PrReceiver<BitBucketSettings> {

    override suspend fun getDefaultBranch(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): String {
        return client.get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch")
            .displayId
    }

    override suspend fun getDefaultReviewers(settings: BitBucketSettings, pullRequest: PullRequest): List<String> {
        val client: HttpClient = HttpClientProvider.getClient(pullRequest.searchHostName, pullRequest.codeHostName, settings)
        return client.get<List<BitBucketUser>>("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${pullRequest.project}/repos/${pullRequest.repo}/reviewers?sourceRepoId=${pullRequest.repoId}&targetRepoId=${pullRequest.repoId}&sourceRefId=${pullRequest.branchFrom.branchName}&targetRefId=${pullRequest.branchTo.branchName}")
            .map { it.name }
    }

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, repo: SearchResult, fromBranchRef: String, toBranchRef: String): List<String> {
        val bitBucketRepo = getRepo(client, settings, repo)
        return client.get<List<BitBucketUser>>("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${repo.project}/repos/${repo.repo}/reviewers?sourceRepoId=${bitBucketRepo.id}&targetRepoId=${bitBucketRepo.id}&sourceRefId=${fromBranchRef}&targetRefId=${toBranchRef}")
            .map { it.name }
    }

    private suspend fun getRepo(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): BitBucketRepo {
        return client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}")
    }

    override suspend fun getPr(searchHostName: String, codeHostName: String, settings: BitBucketSettings, repo: SearchResult): PullRequest? = TODO("Not yet implemented")
    override suspend fun getPr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")

    override suspend fun createPr(title: String, description: String, settings: BitBucketSettings, repo: SearchResult): PullRequest? {
        val client: HttpClient = HttpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)

        val defaultBranch = client
            .get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch")
            .id
        val localBranch: String = LocalRepoOperator.getBranch(repo)!!
        val reviewers = getDefaultReviewers(client, settings, repo, localBranch, defaultBranch)
            .map { BitBucketPrReviewer(BitBucketPrUser(name = it)) }
        val pr: BitBucketDashboardPullRequest = client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/pull-requests") {
            body = BitBucketPullRequestRequest(
                title = "TODO",
                description = "TODO",
                fromRef = BitBucketPrBranchRef(
                    id = localBranch,
                    repository = BitBucketPrRepo(
                        slug = repo.repo,
                        project = BitBucketProject(key = repo.project)
                    )
                ),
                toRef = BitBucketPrBranchRef(
                    id = defaultBranch,
                    repository = BitBucketPrRepo(
                        slug = repo.repo,
                        project = BitBucketProject(key = repo.project)
                    )
                ),
                reviewers = reviewers,
            )
        }

        return PullRequest(
            id = "${pr.id}",
            version = "${pr.version}",
            codeHostName = repo.codeHostName,
            searchHostName = repo.searchHostName,
            project = repo.project,
            repo = repo.repo,
            repoId = "${pr.toRef.repository.id}",
            branchFrom = BranchRef(
                branchName = pr.fromRef.id,
                repo = pr.fromRef.repository.slug,
                project = pr.fromRef.repository.project.key,
            ),
            branchTo = BranchRef(
                branchName = pr.toRef.id,
                repo = pr.toRef.repository.slug,
                project = pr.toRef.repository.project.key,
            ),
            title = pr.title,
            body = pr.description,
            reviewers = pr.reviewers.map {
                Reviewer(
                    name = it.user.name,
                    displayName = it.user.displayName
                )
            },
        )
    }

    override suspend fun updatePr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")

    override suspend fun getAllPrs(searchHostName: String, codeHostName: String, settings: BitBucketSettings): List<PullRequest> {
        val client: HttpClient = HttpClientProvider.getClient(searchHostName, codeHostName, settings)
        val collector = ArrayList<PullRequest>()
        var start = 0L
        while (true) {
            val response: BitBucketDashboardPullRequestResponse = client
                .get("${settings.baseUrl}/rest/api/1.0/dashboard/pull-requests?state=OPEN&role=AUTHOR&start=$start&limit=100")
            collector.addAll(
                response.values.map {
                    PullRequest(
                        id = "${it.id}",
                        searchHostName = searchHostName,
                        codeHostName = codeHostName,
                        title = it.title,
                        version = "${it.version}",
                        body = it.description,
                        branchFrom = BranchRef(
                            branchName = it.fromRef.id,
                            repo = it.fromRef.repository.slug,
                            project = it.fromRef.repository.project.key,
                        ),
                        branchTo = BranchRef(
                            branchName = it.toRef.id,
                            repo = it.toRef.repository.slug,
                            project = it.toRef.repository.project.key,
                        ),
                        project = it.toRef.repository.project.key,
                        repo = it.toRef.repository.slug,
                        repoId = it.toRef.repository.id.toString(),
                        reviewers = it.reviewers.map {
                            Reviewer(
                                name = it.user.name,
                                displayName = it.user.displayName
                            )
                        }
                    )
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

    private fun getAllPrsForOneRepo(settings: BitBucketSettings, repo: File): List<PullRequest> {
        TODO("not implemented")
    }

    override suspend fun closePr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest {
        val client = HttpClientProvider.getClient(pullRequest.searchHostName, pullRequest.codeHostName, settings)
        try {
            val urlString = "${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project}/repos/${pullRequest.repo}/pull-requests/${pullRequest.id}/decline?version=${pullRequest.version}"
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
