package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketRepo
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComRepo
import com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabProject
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

sealed class GitLabRepoWrapping : RepoWrapper() {
    abstract val fullPath:String
    abstract val projectId:Long
}

data class GitLabRepoGraphQlWrapping(
    private val searchHost: String,
    private val codeHost: String,
    val gitLabProject: Project
) : GitLabRepoWrapping() {
    override val fullPath: String = gitLabProject.fullPath
    override val projectId: Long = gitLabProject.id.removePrefix("gid://gitlab/Project/").toLong()
    override fun getSearchHost(): String = searchHost
    override fun getCodeHost(): String = codeHost
    override fun getProject(): String = gitLabProject.namespace?.path!!
    override fun getRepo(): String = gitLabProject.path
    override fun getCloneUrl(cloneType: CloneType): String? = when (cloneType) {
        SSH -> gitLabProject.sshUrlToRepo
        HTTPS -> gitLabProject.httpUrlToRepo
    }
    override fun getDefaultBranch(): String? = gitLabProject.repository?.rootRef
}

data class GitLabApiRepoWrapping(
        private val searchHost: String,
        private val codeHost: String,
        private val gitLabProject: GitLabProject
) : GitLabRepoWrapping(){
    override val fullPath: String = "${gitLabProject.path}/${gitLabProject.namespace.path}"
    override val projectId: Long = gitLabProject.id

    override fun getSearchHost(): String = searchHost
    override fun getCodeHost(): String = codeHost
    override fun getProject(): String = gitLabProject.namespace.path
    override fun getRepo(): String = gitLabProject.path
    override fun getCloneUrl(cloneType: CloneType): String = when (cloneType) {
        SSH -> gitLabProject.ssh_url_to_repo
        HTTPS -> gitLabProject.http_url_to_repo
    }
    override fun getDefaultBranch(): String = gitLabProject.default_branch
}
