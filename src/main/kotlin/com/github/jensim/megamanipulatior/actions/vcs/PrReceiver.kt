package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import com.github.jensim.megamanipulatior.settings.CodeHostSettings
import io.ktor.client.HttpClient

interface PrReceiver<T : CodeHostSettings> {

    suspend fun getDefaultBranch(client: HttpClient, settings: T, repo: SearchResult): String
    fun getDefaultReviewers(settings: T, pullRequest: PullRequest): List<String>
    fun getPr(settings: T, pullRequest: PullRequest): PullRequest?
    fun createPr(title: String, description: String, settings: T, repo: SearchResult): PullRequest?
    fun updatePr(settings: T, pullRequest: PullRequest): PullRequest?
    fun getAllPrs(searchHostName: String, codeHostName: String, settings: T): List<PullRequest>
    fun closePr(settings: T, pullRequest: PullRequest): PullRequest?
    fun getPr(searchHostName: String, codeHostName: String, settings: BitBucketSettings, repo: SearchResult): PullRequest?
}
