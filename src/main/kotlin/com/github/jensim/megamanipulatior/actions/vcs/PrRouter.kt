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

    suspend fun addDefaultReviewers(pullRequest: PullRequest): PullRequest = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.addDefaultReviewers(settings, pullRequest)
        else -> TODO("Not implemented!")
    }

    suspend fun createPr(title: String, description: String, repo: SearchResult): PullRequest? = when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.createPr(title, description, settings, repo)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    suspend fun updatePr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.updatePr(settings, pullRequest)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String): List<PullRequest>? {
        val settings = SettingsFileOperator.readSettings()
        return SettingsFileOperator.readSettings()?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.settings?.let {
            when (it) {
                is BitBucketSettings -> BitbucketPrReceiver.getAllPrs(searchHost, codeHost, it)
                is GitHubSettings -> TODO("Not implemented!")
            }
        }
    }

    suspend fun closePr(pullRequest: PullRequest) = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.closePr(settings, pullRequest)
        is GitHubSettings -> Unit//TODO("Not implemented!")
        null -> Unit//TODO("Not configured code host")
    }
}
