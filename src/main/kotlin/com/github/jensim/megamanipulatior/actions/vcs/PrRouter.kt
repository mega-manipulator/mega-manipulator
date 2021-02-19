package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.settings.CodeHostType
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator

object PrRouter : PrReceiver {

    private fun resolve(codeHost: String): CodeHostType? = SettingsFileOperator.readSettings()
        ?.codeHostSettings?.firstOrNull { it.settings.sourceGraphName == codeHost }?.type

    private fun resolveRoute(codeHost: String): PrReceiver = when (resolve(codeHost)) {
        CodeHostType.BITBUCKET_SERVER -> BitbucketPrReceiver
        CodeHostType.GIT_LAB -> TODO()
        CodeHostType.GITHUB -> TODO()
        else -> TODO()
    }

    override fun getPr(pullRequest: PullRequest): PullRequest? = resolveRoute(pullRequest.codeHost).getPr(pullRequest)
    override fun createPr(pullRequest: PullRequest): PullRequest? = resolveRoute(pullRequest.codeHost).getPr(pullRequest)
    override fun updatePr(pullRequest: PullRequest): PullRequest? = resolveRoute(pullRequest.codeHost).getPr(pullRequest)
    override fun getAllPrs(): List<PullRequest> = TODO()


}