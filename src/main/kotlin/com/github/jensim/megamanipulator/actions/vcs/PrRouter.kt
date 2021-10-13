package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClient
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComClient
import com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabClient
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.BitBucketSettings
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitHubSettings
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings.GitLabSettings
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import java.util.concurrent.atomic.AtomicLong

@SuppressWarnings("TooManyFunctions")
class PrRouter @NonInjectable constructor(
    project: Project,
    settingsFileOperator: SettingsFileOperator?,
    bitbucketServerClient: BitbucketServerClient?,
    githubComClient: GithubComClient?,
    gitLabClient: GitLabClient?,
    notificationsOperator: NotificationsOperator?,
) {

    constructor(project: Project) : this(
        project = project,
        settingsFileOperator = null,
        bitbucketServerClient = null,
        githubComClient = null,
        gitLabClient = null,
        notificationsOperator = null
    )

    private val settingsFileOperator: SettingsFileOperator by lazyService(project, settingsFileOperator)
    private val bitbucketServerClient: BitbucketServerClient by lazyService(project, bitbucketServerClient)
    private val githubComClient: GithubComClient by lazyService(project, githubComClient)
    private val gitLabClient: GitLabClient by lazyService(project, gitLabClient)
    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)

    private val lastSettingsWarning = AtomicLong()

    private fun resolve(searchHost: String, codeHost: String): CodeHostSettings? {
        val resolved = settingsFileOperator.readSettings()
            ?.searchHostSettings?.get(searchHost)?.codeHostSettings?.get(codeHost)
        if (resolved == null) {
            val last = lastSettingsWarning.get()
            val current = System.currentTimeMillis()
            if (last < (current - 100)) {
                lastSettingsWarning.set(current)
                notificationsOperator.show(
                    title = "Missing config",
                    body = "Failed finding config for '$searchHost'/'$codeHost'",
                    type = WARNING
                )
            }
        }
        return resolved
    }

    suspend fun addDefaultReviewers(pullRequest: PullRequestWrapper): PrActionStatus {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.addDefaultReviewers(settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.addDefaultReviewers(settings, pullRequest)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestListItemWrapper -> gitLabClient.addDefaultReviewers(settings, pullRequest)
            else -> PrActionStatus(success = false, msg = "Unable to match config correctly")
        }
    }

    suspend fun createPr(title: String, description: String, repo: SearchResult): PullRequestWrapper? {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> bitbucketServerClient.createPr(title, description, settings, repo)
            is GitHubSettings -> githubComClient.createPr(title, description, settings, repo)
            is GitLabSettings -> gitLabClient.createPr(title, description, settings, repo)
            null -> null
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun createFork(repo: SearchResult): String? {
        return when (val settings = resolve(repo.searchHostName, repo.codeHostName)) {
            is BitBucketSettings -> bitbucketServerClient.createFork(settings, repo)
            is GitHubSettings -> githubComClient.createFork(settings, repo)
            is GitLabSettings -> gitLabClient.createFork(settings, repo)
            null -> null
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun updatePr(newTitle: String, newDescription: String, pullRequest: PullRequestWrapper): PrActionStatus {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.updatePr(newTitle, newDescription, settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.updatePr(newTitle, newDescription, settings, pullRequest)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestWrapper -> gitLabClient.updatePr(newTitle, newDescription, settings, pullRequest)
            else -> PrActionStatus(false, "Unable to match config correctly")
        }
    }

    suspend fun getAllReviewPrs(searchHost: String, codeHost: String): List<PullRequestWrapper>? {
        return resolve(searchHost, codeHost)?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getAllReviewerPrs(searchHost, codeHost, it)
                is GitHubSettings -> githubComClient.getAllReviewerPrs(searchHost, codeHost, it)
                is GitLabSettings -> gitLabClient.getAllReviewPrs(searchHost, codeHost, it)
            }
        }
    }
    suspend fun getAllAuthorPrs(searchHost: String, codeHost: String): List<PullRequestWrapper>? {
        return resolve(searchHost, codeHost)?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getAllAuthorPrs(searchHost, codeHost, it)
                is GitHubSettings -> githubComClient.getAllAuthorPrs(searchHost, codeHost, it)
                is GitLabSettings -> gitLabClient.getAllAuthorPrs(searchHost, codeHost, it)
            }
        }
    }

    suspend fun closePr(dropFork: Boolean, dropBranch: Boolean, pullRequest: PullRequestWrapper): PrActionStatus {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.closePr(dropFork, dropBranch, settings, pullRequest)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.closePr(dropFork, dropBranch, settings, pullRequest)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestWrapper -> gitLabClient.closePr(dropFork, dropBranch, settings, pullRequest)
            else -> PrActionStatus(success = false, msg = "Unable to match config correctly")
        }
    }

    suspend fun getPrivateForkReposWithoutPRs(searchHost: String, codeHost: String): List<RepoWrapper>? {
        return resolve(searchHost, codeHost)?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
                is GitHubSettings -> githubComClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
                is GitLabSettings -> gitLabClient.getPrivateForkReposWithoutPRs(searchHost, codeHost, it)
            }
        }
    }

    suspend fun deletePrivateRepo(fork: RepoWrapper) {
        resolve(fork.getSearchHost(), fork.getCodeHost())?.let { settings ->
            when {
                settings is BitBucketSettings && fork is BitBucketRepoWrapping -> bitbucketServerClient.deletePrivateRepo(fork, settings)
                settings is GitHubSettings && fork is GithubComRepoWrapping -> githubComClient.deletePrivateRepo(fork, settings)
                settings is GitLabSettings && fork is GitLabRepoWrapping -> gitLabClient.deletePrivateRepo(fork, settings)
                else -> throw IllegalArgumentException("Unable to match config correctly")
            }
        }
    }

    suspend fun getRepo(searchResult: SearchResult): RepoWrapper? {
        val settings = resolve(searchResult.searchHostName, searchResult.codeHostName)
        return settings?.let {
            when (it) {
                is BitBucketSettings -> bitbucketServerClient.getRepo(searchResult, it)
                is GitHubSettings -> githubComClient.getRepo(searchResult, it)
                is GitLabSettings -> gitLabClient.getRepo(searchResult, it)
            }
        }
    }

    suspend fun commentPR(comment: String, pullRequest: PullRequestWrapper) {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.commentPR(comment, pullRequest, settings)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.commentPR(comment, pullRequest, settings)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestWrapper -> gitLabClient.commentPR(comment, pullRequest, settings)
            settings == null -> Unit
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun validateAccess(): Map<String, Deferred<String>> {
        return settingsFileOperator.readSettings()?.searchHostSettings.orEmpty().flatMap { search ->
            search.value.codeHostSettings.map { code ->
                "${search.key}/${code.key}" to GlobalScope.async {
                    when (val settings = code.value) {
                        is BitBucketSettings -> bitbucketServerClient.validateAccess(search.key, code.key, settings)
                        is GitHubSettings -> githubComClient.validateAccess(search.key, code.key, settings)
                        is GitLabSettings -> gitLabClient.validateAccess(search.key, code.key, settings)
                    }
                }
            }
        }.toMap()
    }

    suspend fun approvePr(pullRequest: PullRequestWrapper): PrActionStatus {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.approvePr(pullRequest, settings)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.approvePr(pullRequest, settings)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestWrapper -> gitLabClient.approvePr(pullRequest, settings)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun disapprovePr(pullRequest: PullRequestWrapper): PrActionStatus {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.disapprovePr(pullRequest, settings)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.disapprovePr(pullRequest, settings)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestWrapper -> gitLabClient.disapprovePr(pullRequest, settings)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }

    suspend fun mergePr(pullRequest: PullRequestWrapper): PrActionStatus {
        val settings = resolve(pullRequest.searchHostName(), pullRequest.codeHostName())
        return when {
            settings is BitBucketSettings && pullRequest is BitBucketPullRequestWrapper -> bitbucketServerClient.merge(pullRequest, settings)
            settings is GitHubSettings && pullRequest is GithubComPullRequestWrapper -> githubComClient.merge(pullRequest, settings)
            settings is GitLabSettings && pullRequest is GitLabMergeRequestWrapper -> gitLabClient.merge(pullRequest, settings)
            else -> throw IllegalArgumentException("Unable to match config correctly")
        }
    }
}
