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
import io.ktor.client.request.put
import java.io.File

object BitbucketPrReceiver : PrReceiver<BitBucketSettings> {

    override suspend fun getDefaultBranch(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): String {
        return client.get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch")
            .displayId
    }

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, pullRequest: PullRequest): List<BitBucketUser> {
        return client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${pullRequest.project}/repos/${pullRequest.repo}/reviewers?sourceRepoId=${pullRequest.repoId}&targetRepoId=${pullRequest.repoId}&sourceRefId=${pullRequest.branchFrom.branchName}&targetRefId=${pullRequest.branchTo.branchName}")
    }

    override suspend fun addDefaultReviewers(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest {
        val client: HttpClient = HttpClientProvider.getClient(pullRequest.searchHostName, pullRequest.codeHostName, settings)
        val defaultReviewers = getDefaultReviewers(client, settings, pullRequest).map { BitBucketPrReviewer(BitBucketPrUser(it.name)) }
        val bbPR = pullRequest.toBitbucket()
        val bbPR2 = bbPR.copy(reviewers = (bbPR.reviewers + defaultReviewers).distinct())
        return updatePr(client, settings, pullRequest, bbPR2)
    }

    private suspend fun getDefaultReviewers(client: HttpClient, settings: BitBucketSettings, repo: SearchResult, fromBranchRef: String, toBranchRef: String): List<BitBucketUser> {
        val bitBucketRepo: BitBucketRepo = getRepo(client, settings, repo)
        return client.get<List<BitBucketUser>>("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${repo.project}/repos/${repo.repo}/reviewers?sourceRepoId=${bitBucketRepo.id}&targetRepoId=${bitBucketRepo.id}&sourceRefId=${fromBranchRef}&targetRefId=${toBranchRef}")
    }

    private suspend fun getRepo(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): BitBucketRepo {
        return client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}")
    }

    override suspend fun getPr(searchHostName: String, codeHostName: String, settings: BitBucketSettings, repo: SearchResult): PullRequest? = TODO("Not yet implemented")
    override suspend fun getPr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")

    override suspend fun createPr(title: String, description: String, settings: BitBucketSettings, repo: SearchResult): PullRequest {
        val client: HttpClient = HttpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)

        val defaultBranch = client
            .get<BitBucketDefaultBranch>("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/default-branch")
            .id
        val localBranch: String = LocalRepoOperator.getBranch(repo)!!
        val reviewers = getDefaultReviewers(client, settings, repo, localBranch, defaultBranch)
            .map { BitBucketPrReviewer(BitBucketPrUser(name = it.name)) }
        val pr: BitBucketDashboardPullRequest = client.post("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}/pull-requests") {
            body = BitBucketPullRequestRequest(
                id = null,
                title = title,
                description = description,
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

        return pr.toDomain(repo)
    }

    private fun BitBucketDashboardPullRequest.toDomain(repo: SearchResult) = PullRequest(
        id = "${this.id}",
        version = "${this.version}",
        codeHostName = repo.codeHostName,
        searchHostName = repo.searchHostName,
        project = repo.project,
        repo = repo.repo,
        repoId = "${this.toRef.repository.id}",
        branchFrom = BranchRef(
            branchName = this.fromRef.id,
            repo = this.fromRef.repository.slug,
            project = this.fromRef.repository.project.key,
        ),
        branchTo = BranchRef(
            branchName = this.toRef.id,
            repo = this.toRef.repository.slug,
            project = this.toRef.repository.project.key,
        ),
        title = this.title,
        body = this.description,
        reviewers = this.reviewers.map {
            Reviewer(
                name = it.user.name,
                displayName = it.user.displayName
            )
        },
    )


    private fun PullRequest.toBitbucket() = BitBucketPullRequestRequest(
        id = id.toLong(),
        title = title,
        description = body,
        fromRef = BitBucketPrBranchRef(
            id = branchFrom.branchName,
            repository = BitBucketPrRepo(
                slug = repo,
                project = BitBucketProject(key = project)
            )
        ),
        toRef = BitBucketPrBranchRef(
            id = branchTo.branchName,
            repository = BitBucketPrRepo(
                slug = repo,
                project = BitBucketProject(key = project)
            )
        ),
        reviewers = reviewers.toBitBucket(),
    )

    private fun List<Reviewer>.toBitBucket(): List<BitBucketPrReviewer> = map {
        BitBucketPrReviewer(BitBucketPrUser(it.name))
    }

    override suspend fun updatePr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest {
        val client: HttpClient = HttpClientProvider.getClient(pullRequest.searchHostName, pullRequest.codeHostName, settings)
        val bbPR: BitBucketPullRequestRequest = pullRequest.toBitbucket()
        return updatePr(client, settings, pullRequest, bbPR)
    }

    private suspend fun updatePr(client: HttpClient, settings: BitBucketSettings, pullRequest: PullRequest, bbPullRequest: BitBucketPullRequestRequest): PullRequest {
        val pr: BitBucketDashboardPullRequest = client.put("${settings.baseUrl}/rest/api/1.0/projects/${pullRequest.project}/repos/${pullRequest.repo}/pull-requests/${pullRequest.id}") {
            body = bbPullRequest
        }
        return pr.toDomain(
            SearchResult(
                project = pullRequest.project,
                repo = pullRequest.repo,
                codeHostName = pullRequest.codeHostName,
                searchHostName = pullRequest.searchHostName,
            )
        )
    }

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
