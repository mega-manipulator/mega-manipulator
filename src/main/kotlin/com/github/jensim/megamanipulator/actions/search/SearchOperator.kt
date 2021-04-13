package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator

class SearchOperator(
    private val settingsFileOperator: SettingsFileOperator,
    private val sourcegraphSearchOperator: SourcegraphSearchOperator,
) {

    companion object {
        val instance by lazy {
            SearchOperator(
                settingsFileOperator = SettingsFileOperator.instance,
                sourcegraphSearchOperator = SourcegraphSearchOperator.instance,
            )
        }
    }

    suspend fun search(searchHostName: String, search: String): Set<SearchResult> {
        val settings: SearchHostSettings = settingsFileOperator.readSettings()?.searchHostSettings?.get(searchHostName)
            ?: throw NullPointerException("No settings for search host named $searchHostName")
        return when (settings) {
            is SearchHostSettings.SourceGraphSettings -> sourcegraphSearchOperator.search(searchHostName, settings, search)
        }
    }
}
