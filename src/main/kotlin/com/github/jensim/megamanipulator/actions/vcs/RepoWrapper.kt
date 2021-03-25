package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitBucketRepo

sealed class ForkRepoWrapper {
    abstract fun getSearchHost(): String
    abstract fun getCodeHost(): String
    abstract fun getProject(): String
    abstract fun getRepo(): String
    fun asDisplayString() = "${getSearchHost()}/${getCodeHost()}/${getProject()}/${getRepo()}"
}

data class BitBucketForkRepo(
        private val searchHost: String,
        private val codeHost: String,
        val repo: BitBucketRepo,
) : ForkRepoWrapper() {
    override fun getSearchHost(): String = searchHost
    override fun getCodeHost(): String = codeHost
    override fun getProject(): String = repo.project!!.key
    override fun getRepo(): String = repo.slug
}
