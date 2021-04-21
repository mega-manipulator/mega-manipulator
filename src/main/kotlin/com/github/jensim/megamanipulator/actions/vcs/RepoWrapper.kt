package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketRepo
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComRepo
import kotlinx.serialization.Serializable

@Serializable
sealed class RepoWrapper {
    abstract fun getSearchHost(): String
    abstract fun getCodeHost(): String
    abstract fun getProject(): String
    abstract fun getRepo(): String
    abstract fun getCloneUrl(): String?
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
    override fun getCloneUrl(): String? = repo.links?.clone?.first { it.name == "ssh" }?.href
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
    override fun getCloneUrl(): String = repo.ssh_url
    override fun getDefaultBranch(): String = repo.default_branch
}
