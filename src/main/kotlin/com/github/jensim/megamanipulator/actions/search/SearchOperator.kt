package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.search.github.GitHubSearchClient
import com.github.jensim.megamanipulator.actions.search.hound.HoundClient
import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchClient
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.searchhost.GithubSearchSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.HoundSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class SearchOperator @NonInjectable constructor(
    project: Project,
    settingsFileOperator: SettingsFileOperator?,
    sourcegraphSearchClient: SourcegraphSearchClient?,
    houndClient: HoundClient?,
    gitHubSearchClient: GitHubSearchClient?,
) {
    constructor(project: Project) : this(
        project = project,
        settingsFileOperator = null,
        sourcegraphSearchClient = null,
        houndClient = null,
        gitHubSearchClient = null,
    )

    private val settingsFileOperator: SettingsFileOperator by lazyService(project, settingsFileOperator)
    private val sourcegraphSearchClient: SourcegraphSearchClient by lazyService(project, sourcegraphSearchClient)
    private val houndClient: HoundClient by lazyService(project, houndClient)
    private val gitHubSearchClient: GitHubSearchClient by lazyService(project, gitHubSearchClient)

    suspend fun search(searchHostName: String, search: String): Set<SearchResult> {
        val settings: SearchHostSettings = settingsFileOperator.readSettings()?.searchHostSettings?.get(searchHostName)?.value()
            ?: throw NullPointerException("No settings for search host named $searchHostName")
        return when (settings) {
            is SourceGraphSettings -> sourcegraphSearchClient.search(searchHostName, settings, search)
            is HoundSettings -> houndClient.search(searchHostName, settings, search)
            is GithubSearchSettings -> gitHubSearchClient.search(searchHostName, settings, search)
        }
    }

    suspend fun validateTokens(): Map<String, Deferred<String>> =
        settingsFileOperator.readSettings()?.searchHostSettings.orEmpty().map { (name, settingsGroup) ->
            name to GlobalScope.async {
                when (settingsGroup.value()) {
                    is SourceGraphSettings -> sourcegraphSearchClient.validateToken(name, settingsGroup.sourceGraph!!)
                    is HoundSettings -> houndClient.validate(name, settingsGroup.hound!!)
                    is GithubSearchSettings -> gitHubSearchClient.validateAccess(name, settingsGroup.gitHub!!)
                }
            }
        }.toMap()
}
