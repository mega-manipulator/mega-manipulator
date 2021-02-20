package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.vcs.bitbucketserver.BitbucketPrReceiver
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import com.github.jensim.megamanipulatior.settings.CodeHostSettings
import com.github.jensim.megamanipulatior.settings.GitHubSettings
import com.github.jensim.megamanipulatior.settings.GitLabSettings
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator

object PrRouter {

    private fun resolve(codeHost: String): CodeHostSettings? = SettingsFileOperator.readSettings()
        ?.codeHostSettings?.firstOrNull { it.settings.sourceGraphName == codeHost }?.settings

    fun getDefaultBranch(repo: SearchResult): String = when (val settings = resolve(repo.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.getDefaultBranch(settings, repo)
        is GitLabSettings -> TODO("Not implemented!")
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun getDefaultReviewers(repo: SearchResult): List<String> = when (val settings = resolve(repo.codeHostName)) {
        is BitBucketSettings -> BitbucketPrReceiver.getDefaultReviewers(settings, repo)
        is GitLabSettings -> TODO("Not implemented!")
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun getPr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.codeHost)) {
        is BitBucketSettings -> BitbucketPrReceiver.getPr(settings, pullRequest)
        is GitLabSettings -> TODO("Not implemented!")
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun createPr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.codeHost)) {
        is BitBucketSettings -> BitbucketPrReceiver.createPr(settings, pullRequest)
        is GitLabSettings -> TODO("Not implemented!")
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun updatePr(pullRequest: PullRequest): PullRequest? = when (val settings = resolve(pullRequest.codeHost)) {
        is BitBucketSettings -> BitbucketPrReceiver.updatePr(settings, pullRequest)
        is GitLabSettings -> TODO("Not implemented!")
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }

    fun getAllPrs(): List<PullRequest> = SettingsFileOperator.readSettings()?.codeHostSettings.orEmpty().flatMap {
        when (val settings = it.settings) {
            is BitBucketSettings -> BitbucketPrReceiver.getAllPrs(settings)
            is GitLabSettings -> TODO("Not implemented!")
            is GitHubSettings -> TODO("Not implemented!")
        }
    }

    fun closePr(pullRequest: PullRequest) = when (val settings = resolve(pullRequest.codeHost)) {
        is BitBucketSettings -> BitbucketPrReceiver.closePr(settings, pullRequest)
        is GitLabSettings -> TODO("Not implemented!")
        is GitHubSettings -> TODO("Not implemented!")
        null -> TODO("Not configured code host")
    }
}
