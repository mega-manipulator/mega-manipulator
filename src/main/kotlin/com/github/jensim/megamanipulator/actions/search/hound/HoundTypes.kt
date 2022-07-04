package com.github.jensim.megamanipulator.actions.search.hound

@SuppressWarnings("ConstructorParameterNaming")
object HoundTypes {

    data class HoundRepo(
        // http://localhost:6080/api/v1/repos
        val url: String, // ": "https://github.com/etsy/hound.git",
        val vcs: String, // ": "git",
    )

    data class HoundSearchResults(
        val Results: Map<String, HoundSearchResult>,
    )

    data class HoundSearchResult(
        val FilesWithMatch: Long,
    )
}
