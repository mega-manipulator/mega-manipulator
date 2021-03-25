package com.github.jensim.megamanipulator.actions.vcs

interface PullRequest {
    fun codeHostName(): String
    fun searchHostName(): String
    fun project(): String
    fun repo(): String
    fun title(): String
    fun body(): String
    fun fromBranch(): String
    fun toBranch(): String
    fun alterCopy(
            codeHostName: String? = null,
            searchHostName: String? = null,
            title: String? = null,
            body: String? = null,
    ): PullRequest

    fun isFork(): Boolean
}
