package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClient
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComClient
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.CodeHostSettings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator

class PrRouter(
    private val settingsFileOperator: SettingsFileOperator,
    private val bitbucketServerClient: BitbucketServerClient,
    private val githubComClient: GithubComClient,
) {

    companion object {

        val instance by lazy {
            PrRouter(
                settingsFileOperator = SettingsFileOperator.instance,
                bitbucketServerClient = BitbucketServerClient.instance,
                githubComClient = GithubComClient.instance,
            )
        }
    }

    private fun resolve(searchHost: String, codeHost: String): CodeHostSettings? = settingsFileOperator.readSettings()
        ?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)

    suspend fun addDefaultReviewers(pullRequest: PullRequestWrapper): PullRequestWrapper {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.addDefaultReviewers(settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.addDefaultReviewers(settings, pullRequest)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun createPr(title: String, description: String, repo: SearchResult): PullRequestWrapper {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> bitbucketServerClient.createPr(title, description, settings, repo)
            is GitHubSettings -> githubComClient.createPr(title, description, settings, repo)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun createFork(repo: SearchResult): String? {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> bitbucketServerClient.createFork(settings, repo)
            is GitHubSettings -> githubComClient.createFork(settings, repo)
            null -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun updatePr(newTitle: String, newDescription: String, pullRequest: PullRequestWrapper): PullRequestWrapper {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.updatePr(newTitle, newDescription, settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.updatePr(newTitle, newDescription, settings, pullRequest)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun getAllPrs(searchHost: String, codeHost: String): List<PullRequestWrapper> {
        return settingsFileOperator.readSettings()?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getAllPrs(searchHost, codeHost, it)
                is GitHubSettings -> githubComClient.getAllPrs(searchHost, codeHost, it)
            }
        } ?: throw IllegalArgumentException("No config!")
    }

    suspend fun closePr(dropForkOrBranch: Boolean, pullRequest: PullRequestWrapper) {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.closePr(dropForkOrBranch, settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.closePr(dropForkOrBranch, settings, pullRequest)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String): List<RepoWrapper> {
        return settingsFileOperator.readSettings()?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
                is GitHubSettings -> githubComClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
            }
        } ?: throw IllegalArgumentException("No config!")
    }

    suspend fun deletePrivateRepo(fork: RepoWrapper) {
        settingsFileOperator.readSettings()?.searchHostSettings?.get(fork.getSearchHost())?.codeHostSettings?.get(fork.getCodeHost())?.let { it ->
            when {
                it is BitBucketSettings && fork is BitBucketRepoWrapping -> bitbucketServerClient.deletePrivateRepo(fork, it)
                it is GitHubSettings && fork is GithubComRepoWrapping -> githubComClient.deletePrivateRepo(fork, it)
                else -> throw IllegalArgumentException("Unable to match config correctly")
            }
        } ?: throw IllegalArgumentException("No config!")
    }

    suspend fun getRepo(searchResult: SearchResult): RepoWrapper {
        return settingsFileOperator.readSettings()?.searchHostSettings?.get(searchResult.searchHostName)?.codeHostSettings?.get(searchResult.codeHostName)?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getRepo(searchResult, it)
                is GitHubSettings -> githubComClient.getRepo(searchResult, it)
            }
        } ?: throw IllegalArgumentException("No config!")
    }
}
