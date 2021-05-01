package com.github.jensim.megamanipulator.actions.search.hound

import kotlinx.serialization.Serializable

@SuppressWarnings("ConstructorParameterNaming")
object HoundTypes {

    @Serializable
    data class HoundRepo(
        // http://localhost:6080/api/v1/repos
        val url: String, // ": "https://github.com/etsy/hound.git",
        val vcs: String, // ": "git",
    )

    @Serializable
    data class HoundSearchResults(
        val Results: Map<String, HoundSearchResult>,
    )

    @Serializable
    data class HoundSearchResult(
        val FilesWithMatch: Long,
    )
}
