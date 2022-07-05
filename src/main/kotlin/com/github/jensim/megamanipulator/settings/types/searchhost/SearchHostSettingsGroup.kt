package com.github.jensim.megamanipulator.settings.types.searchhost

data class SearchHostSettingsGroup(
    val sourceGraph: SourceGraphSettings? = null,
    val hound: HoundSettings? = null,
) {
    init {
        val defined = listOfNotNull(sourceGraph, hound)
        if (defined.size != 1) {
            throw IllegalArgumentException("Expected 1 type of search host settings defined. But ${defined.size} was provided ${defined.map { it.javaClass.simpleName }}")
        }
    }

    fun value(): SearchHostSettings = (sourceGraph ?: hound)!!
}
