package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketRepo
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComRepo
import com.github.jensim.megamanipulator.graphql.generated.gitlab.singlerepoquery.Project
import com.github.jensim.megamanipulator.settings.types.CloneType
import com.github.jensim.megamanipulator.settings.types.CloneType.HTTPS
import com.github.jensim.megamanipulator.settings.types.CloneType.SSH
import kotlinx.serialization.Serializable

@Serializable
sealed class RepoWrapper {
    abstract fun getSearchHost(): String
    abstract fun getCodeHost(): String
    abstract fun getProject(): String
    abstract fun getRepo(): String
    abstract fun getCloneUrl(cloneType: CloneType): String?
    abstract fun getDefaultBranch(): String?
    fun asPathString() = "${getSearchHost()}/${getCodeHost()}/${getProject()}/${getRepo()}"
}

@Serializable
data class BitBucketRepoWrapping(
    private val searchHost: String,
    private val codeHost: String,
    val repo: BitBucketRepo,
    private val defaultBranch: String?,
) : RepoWrapper() {
    override fun getSearchHost(): String = searchHost
    override fun getCodeHost(): String = codeHost
    override fun getProject(): String = repo.project!!.key
    override fun getRepo(): String = repo.slug
    override fun getCloneUrl(cloneType: CloneType): String? = when (cloneType) {
        SSH -> repo.links?.clone?.first { it.name == "ssh" }?.href
        HTTPS -> repo.links?.clone?.first { it.name == "http" }?.href
    }
    override fun getDefaultBranch(): String? = defaultBranch
}

@Serializable
data class GithubComRepoWrapping(
    private val searchHost: String,
    private val codeHost: String,
    val repo: GithubComRepo,
) : RepoWrapper() {

    override fun getSearchHost(): String = searchHost
    override fun getCodeHost(): String = codeHost
    override fun getProject(): String = repo.owner.login
    override fun getRepo(): String = repo.name
    override fun getCloneUrl(cloneType: CloneType): String = when (cloneType) {
        SSH -> repo.ssh_url
        HTTPS -> repo.clone_url
    }

    override fun getDefaultBranch(): String = repo.default_branch
}

data class GitLabRepoWrapping(
    private val searchHost: String,
    private val codeHost: String,
    val repo: Project
) : RepoWrapper() {

    override fun getSearchHost(): String = searchHost
    override fun getCodeHost(): String = codeHost
    override fun getProject(): String = repo.namespace?.fullName!!
    override fun getRepo(): String = repo.path
    override fun getCloneUrl(cloneType: CloneType): String? = when (cloneType) {
        SSH -> repo.sshUrlToRepo
        HTTPS -> repo.httpUrlToRepo
    }
    override fun getDefaultBranch(): String? = repo.repository?.rootRef
}
