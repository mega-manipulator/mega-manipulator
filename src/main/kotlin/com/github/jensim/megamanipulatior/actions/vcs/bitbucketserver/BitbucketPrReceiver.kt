package com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver

import com.github.jensim.megamanipulatior.actions.git.GitOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.BranchRef
import com.github.jensim.megamanipulatior.actions.vcs.PrReceiver
import com.github.jensim.megamanipulatior.actions.vcs.PullRequest
import com.github.jensim.megamanipulatior.actions.vcs.Reviewer
import com.github.jensim.megamanipulatior.http.HttpClientProvider
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import java.io.File
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout

object BitbucketPrReceiver : PrReceiver<BitBucketSettings> {

    override fun getDefaultBranch(settings: BitBucketSettings, repo: SearchResult): String {
        val client: HttpClient = HttpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
        TODO("Not yet implemented")
    }

    override fun getDefaultReviewers(settings: BitBucketSettings, repo: SearchResult): List<String> {
        val branch = GitOperator.getBranch(repo) ?: return emptyList()
        return runBlocking {
            val client: HttpClient = HttpClientProvider.getClient(repo.searchHostName, repo.codeHostName, settings)
            val defaultBranch = getDefaultBranch(settings, repo)
            if (defaultBranch == branch) {
                emptyList()
            } else {
                val bbRepo: BitBucketRepo = getRepo(client, settings, repo)
                val reviewers: List<BitBucketParticipant> = client.get("${settings.baseUrl}/rest/default-reviewers/1.0/projects/${bbRepo.project}/repos/${bbRepo.slug}/reviewers?sourceRepoId=${bbRepo.id}&targetRepoId=${bbRepo.id}&sourceRefId${branch}&targetRefId${defaultBranch}")
                reviewers.map { it.user.name }
            }
        }
    }

    private suspend fun getRepo(client: HttpClient, settings: BitBucketSettings, repo: SearchResult): BitBucketRepo {
        return client.get("${settings.baseUrl}/rest/api/1.0/projects/${repo.project}/repos/${repo.repo}")
    }

    override fun getPr(searchHostName: String, codeHostName: String, settings: BitBucketSettings, repo: SearchResult): PullRequest? = TODO("Not yet implemented")
    override fun getPr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun createPr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")
    override fun updatePr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")

    override fun getAllPrs(searchHostName: String, codeHostName: String, settings: BitBucketSettings): List<PullRequest> {
        val client: HttpClient = HttpClientProvider.getClient(searchHostName, codeHostName, settings)
        val collector = ArrayList<PullRequest>()
        var start = 0L
        while (true) {
            val response: BitBucketDashboardPullRequestResponse = runBlocking {
                withTimeout(Duration.ofMinutes(2)) {
                    client.get("${settings.baseUrl}/rest/api/1.0/dashboard/pull-requests?state=OPEN&role=AUTHOR&start=$start&limit=100")
                }
            }
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

    override fun closePr(settings: BitBucketSettings, pullRequest: PullRequest): PullRequest? = TODO("Not yet implemented")

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
