package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.search.hound.HoundClient
import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchClient
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.HoundSettings
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings.SourceGraphSettings
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class SearchOperator(
    private val settingsFileOperator: SettingsFileOperator,
    private val sourcegraphSearchClient: SourcegraphSearchClient,
    private val houndClient: HoundClient,
) {

    suspend fun search(searchHostName: String, search: String): Set<SearchResult> {
        val settings: SearchHostSettings = settingsFileOperator.readSettings()?.searchHostSettings?.get(searchHostName)
            ?: throw NullPointerException("No settings for search host named $searchHostName")
        return when (settings) {
            is SourceGraphSettings -> sourcegraphSearchClient.search(searchHostName, settings, search)
            is HoundSettings -> houndClient.search(searchHostName, settings, search)
        }
    }

    suspend fun validateTokens(): Map<String, Deferred<String>> = settingsFileOperator.readSettings()?.searchHostSettings.orEmpty().map { (name, settings) ->
        name to GlobalScope.async {
            when (settings) {
                is SourceGraphSettings -> sourcegraphSearchClient.validateToken(name, settings)
                is HoundSettings -> houndClient.validate(name, settings)
            }
        }
    }.toMap()
}
