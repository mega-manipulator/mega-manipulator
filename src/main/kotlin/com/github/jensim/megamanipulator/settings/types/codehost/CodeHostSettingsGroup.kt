package com.github.jensim.megamanipulator.settings.types.codehost

data class CodeHostSettingsGroup(
    val gitLab: GitLabSettings? = null,
    val gitHub: GitHubSettings? = null,
    val bitBucket: BitBucketSettings? = null,
) {
    init {
        val defined = listOfNotNull(gitHub, gitLab, bitBucket)
        if (defined.size != 1) {
            throw IllegalArgumentException("Expected 1 type of code host settings defined. But ${defined.size} was provided ${defined.map { it.javaClass.simpleName }}")
        }
    }

    fun value(): CodeHostSettings = (gitHub ?: gitLab ?: bitBucket)!!
}
