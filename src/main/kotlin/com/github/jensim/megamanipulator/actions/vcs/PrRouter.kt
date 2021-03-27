package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClient
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComClient
import com.github.jensim.megamanipulator.settings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.GitHubSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator

object PrRouter {

    private fun resolve(searchHost: String, codeHost: String): CodeHostSettings? = SettingsFileOperator.readSettings()
        ?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.settings

    suspend fun addDefaultReviewers(pullRequest: PullRequestWrapper): PullRequestWrapper {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> BitbucketServerClient.addDefaultReviewers(settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> GithubComClient.addDefaultReviewers(settings, pullRequest)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun createPr(title: String, description: String, repo: SearchResult): PullRequestWrapper {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> BitbucketServerClient.createPr(title, description, settings, repo)
            is GitHubSettings -> GithubComClient.createPr(title, description, settings, repo)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun createFork(repo: SearchResult): String? {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> BitbucketServerClient.createFork(settings, repo)
            is GitHubSettings -> GithubComClient.createFork(settings, repo)
            null -> null
        }
    }

    suspend fun updatePr(newTitle: String, newDescription: String, pullRequest: PullRequestWrapper): PullRequestWrapper {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> BitbucketServerClient.updatePr(newTitle, newDescription, settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> GithubComClient.updatePr(newTitle, newDescription, settings, pullRequest)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String): List<PullRequestWrapper>? {
        return SettingsFileOperator.readSettings()?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.settings?.let {
            when (it) {
                is BitBucketSettings -> BitbucketServerClient.getAllPrs(searchHost, codeHost, it)
                is GitHubSettings -> GithubComClient.getAllPrs(searchHost, codeHost, it)
            }
        }
    }

    suspend fun closePr(dropForkOrBranch: Boolean, pullRequest: PullRequestWrapper) {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> BitbucketServerClient.closePr(dropForkOrBranch, settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> GithubComClient.closePr(dropForkOrBranch, settings, pullRequest)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String): List<RepoWrapper>? {
        return SettingsFileOperator.readSettings()?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.settings?.let {
            when (it) {
                is BitBucketSettings -> BitbucketServerClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
                is GitHubSettings -> GithubComClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
            }
        }
    }

    suspend fun deletePrivateRepo(fork: RepoWrapper) {
        SettingsFileOperator.readSettings()?.searchHostSettings?.get(fork.getSearchHost())?.codeHostSettings?.get(fork.getCodeHost())?.settings?.let { it ->
            when {
                it is BitBucketSettings && fork is BitBucketRepoWrapping -> BitbucketServerClient.deletePrivateRepo(fork, it)
                it is GitHubSettings && fork is GithubComRepoWrapping -> GithubComClient.deletePrivateRepo(fork, it)
                else -> throw IllegalArgumentException("Unable to match config correctly")
            }
        }
    }

    suspend fun getRepo(searchResult: SearchResult): RepoWrapper? {
        return SettingsFileOperator.readSettings()?.searchHostSettings?.get(searchResult.searchHostName)?.codeHostSettings?.get(searchResult.codeHostName)?.settings?.let {
            when (it) {
                is BitBucketSettings -> BitbucketServerClient.getRepo(searchResult, it)
                is GitHubSettings -> GithubComClient.getRepo(searchResult, it)
            }
        }
    }
}
