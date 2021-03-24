package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.SourceGraphSettings

object SearchOperator {

    suspend fun search(searchHostName: String, search: String): Set<SearchResult> {
        val settings: SearchHostSettings = SettingsFileOperator.readSettings()?.searchHostSettings?.get(searchHostName)?.settings
            ?: throw NullPointerException("No settings for search host named $searchHostName")
        return when (settings) {
            is SourceGraphSettings -> SourcegraphSearchOperator.search(searchHostName, settings, search)
        }
    }
}
