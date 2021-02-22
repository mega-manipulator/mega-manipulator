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

    fun getDefaultBranch(repo: SearchResult): String = when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.getDefaultBranch(settings, repo)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun getDefaultReviewers(repo: SearchResult): List<String> = when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.getDefaultReviewers(settings, repo)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun getPr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.getPr(settings, pullRequest)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun createPr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.createPr(settings, pullRequest)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun updatePr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.updatePr(settings, pullRequest)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun getAllPrs(): List<PullRequest> = SettingsFileOperator.readSettings()?.searchHostSettings?.values
        ?.flatMap { it.codeHostSettings.values.map { it.settings } }.orEmpty().flatMap {
            when (it) {
                is BitBucketSettings -> BitbucketPrReceiver.getAllPrs(it)
                is GitHubSettings -> TODO("Not implemented!")
            }
        }

    fun closePr(pullRequest: PullRequest) = when (val settings = resolve(pullRequest.searchHostName, pullRequest.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.closePr(settings, pullRequest)
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }
}
