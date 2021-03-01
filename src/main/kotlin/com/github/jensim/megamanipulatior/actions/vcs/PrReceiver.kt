package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.settings.BitBucketSettings
import com.github.jensim.megamanipulatior.settings.CodeHostSettings
import io.ktor.client.HttpClient

interface PrReceiver<T : CodeHostSettings> {

    suspend fun getDefaultBranch(client: HttpClient, settings: T, repo: SearchResult): String
    suspend fun getDefaultReviewers(settings: T, pullRequest: PullRequest): List<String>
    suspend fun getPr(settings: T, pullRequest: PullRequest): PullRequest?
    suspend fun createPr(title: String, description: String, settings: T, repo: SearchResult): PullRequest?
    suspend fun updatePr(settings: T, pullRequest: PullRequest): PullRequest?
    suspend fun getAllPrs(searchHostName: String, codeHostName: String, settings: T): List<PullRequest>
    suspend fun closePr(settings: T, pullRequest: PullRequest): PullRequest?
    suspend fun getPr(searchHostName: String, codeHostName: String, settings: BitBucketSettings, repo: SearchResult): PullRequest?
}
