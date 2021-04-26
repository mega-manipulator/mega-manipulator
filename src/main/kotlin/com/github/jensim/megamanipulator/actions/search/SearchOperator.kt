package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchClient
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class SearchOperator(
    private val settingsFileOperator: SettingsFileOperator,
    private val sourcegraphSearchClient: SourcegraphSearchClient,
) {

    suspend fun search(searchHostName: String, search: String): Set<SearchResult> {
        val settings: SearchHostSettings = settingsFileOperator.readSettings()?.searchHostSettings?.get(searchHostName)
            ?: throw NullPointerException("No settings for search host named $searchHostName")
        return when (settings) {
            is SourceGraphSettings -> sourcegraphSearchClient.search(searchHostName, settings, search)
        }
    }

    suspend fun validateTokens(): Map<String, Deferred<String>> = settingsFileOperator.readSettings()?.searchHostSettings.orEmpty().map { (name, settings) ->
        name to GlobalScope.async {
            when (settings) {
                is SourceGraphSettings -> sourcegraphSearchClient.validateToken(name, settings)
            }
        }
    }.toMap()
}
