package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver.BitbucketPrReceiver
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import com.github.jensim.megamanipulatior.settings.CodeHostSettings
import com.github.jensim.megamanipulatior.settings.GitHubSettings
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator

object PrRouter {

    private fun resolve(searchHost: String, codeHost: String): CodeHostSettings? = SettingsFileOperator.readSettings()
        ?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.settings

    suspend fun addDefaultReviewers(pullRequest: PullRequest): PullRequest {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> BitbucketPrReceiver.addDefaultReviewers(settings, pullRequest)
            else -> throw IllegalArgumentException("Provided types does not match expectations")
        }
    }

    suspend fun createPr(title: String, description: String, repo: SearchResult): PullRequest {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> BitbucketPrReceiver.createPr(title, description, settings, repo)
            else -> throw IllegalArgumentException("Provided types does not match expectations")
        }
    }

    suspend fun createFork(repo: SearchResult): String? {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> BitbucketPrReceiver.createFork(settings, repo)
            else -> throw IllegalArgumentException("Provided types does not match expectations")
        }
    }

    suspend fun updatePr(pullRequest: PullRequest): PullRequest {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> BitbucketPrReceiver.updatePr(settings, pullRequest)
            else -> throw IllegalArgumentException("Provided types does not match expectations")
        }
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String): List<PullRequest>? {
        return SettingsFileOperator.readSettings()?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.settings?.let {
            when (it) {
                is BitBucketSettings -> BitbucketPrReceiver.getAllPrs(searchHost, codeHost, it)
                is GitHubSettings -> TODO("Not implemented!")
            }
        }
    }

    suspend fun closePr(pullRequest: PullRequest) {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> BitbucketPrReceiver.closePr(settings, pullRequest)
            else -> throw IllegalArgumentException("Provided types does not match expectations")
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String): List<Nothing> = TODO()
}
