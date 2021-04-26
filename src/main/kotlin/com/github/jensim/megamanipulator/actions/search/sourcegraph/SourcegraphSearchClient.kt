package com.github.jensim.megamanipulator.actions.search.sourcegraph

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.search.SearchTypes
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.SearchHostSettings.SourceGraphSettings
import com.intellij.notification.NotificationType
import com.jetbrains.rd.util.printlnError
import io.ktor.client.features.timeout
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SourcegraphSearchClient(
    private val httpClientProvider: HttpClientProvider,
    private val notificationsOperator: NotificationsOperator,
    private val json: Json,
) {

    suspend fun search(searchHostName: String, settings: SourceGraphSettings, search: String): Set<SearchResult> {
        try {
            /* TODO
             * Paginate on result
             */
            val raw = rawSearch(searchHostName, settings, search)
            val response: SearchTypes.GraphQLResponse = json.decodeFromString(raw.readText())
            response.errors?.let {
                printlnError("ERROR $it")
            }
            return response.data?.search?.results?.results.orEmpty().mapNotNull {
                internalNameToSearchResult(searchHostName, it.repository?.name)
            }.toSet()
        } catch (e: Exception) {
            notificationsOperator.show(
                title = "Search failed",
                body = e.message ?: "Exception running search",
                type = NotificationType.ERROR,
            )
            e.printStackTrace()
            return emptySet()
        }
    }

    private suspend fun rawSearch(searchHostName: String, settings: SourceGraphSettings, search: String): HttpResponse {
        val client = httpClientProvider.getClient(searchHostName, settings)
        return client.post("${settings.baseUrl}/.api/graphql?Search=") {
            body = SearchTypes.GraphQlRequest(SearchTypes.SearchVaraibles(search))
            timeout {
                socketTimeoutMillis = 5 * 60 * 1000
                requestTimeoutMillis = 5 * 60 * 1000
            }
        }
    }

    private fun internalNameToSearchResult(searchHostName: String, name: String?): SearchResult? {
        val parts = name?.split('/')
        return if (parts?.size != 3) {
            null
        } else {
            SearchResult(searchHostName = searchHostName, codeHostName = parts[0], project = parts[1], repo = parts[2])
        }
    }

    suspend fun validateToken(searchHostName: String, settings: SourceGraphSettings): String = try {
        val response: HttpResponse = rawSearch(searchHostName, settings, "count:1 timeout:10 f")
        "${response.status.value}:${response.status.description}"
    } catch (e: Exception) {
        e.printStackTrace()
        "Client error"
    }
}
