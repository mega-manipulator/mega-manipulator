package com.github.jensim.megamanipulatior.actions.search.sourcegraph

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.actions.search.SearchTypes
import com.github.jensim.megamanipulatior.http.HttpClientProvider
import com.github.jensim.megamanipulatior.settings.AuthMethod
import com.github.jensim.megamanipulatior.settings.HttpsOverride
import com.github.jensim.megamanipulatior.settings.PasswordsOperator
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.settings.SourceGraphSettings
import com.intellij.notification.NotificationType
import com.jetbrains.rd.util.printlnError
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import java.time.Duration
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.withTimeout

object SourcegraphSearchOperator {

    fun search(searchHostName: String, settings: SourceGraphSettings, search: String): Set<SearchResult> {
        try {
            val baseUrl = settings.baseUrl
            /*
            TODO
             * Paginate on result
             */
            val httpsOverride: HttpsOverride? = SettingsFileOperator.readSettings()?.resolveHttpsOverride(searchHostName)
            val password = when (settings.authMethod) {
                AuthMethod.TOKEN -> PasswordsOperator.getOrAskForPassword("token", settings.baseUrl)
                AuthMethod.USERNAME_PASSWORD -> PasswordsOperator.getOrAskForPassword(settings.username!!, settings.baseUrl)
            }
            val client: HttpClient = HttpClientProvider.getClient(httpsOverride, settings.authMethod, settings.username, password)
            val response: SearchTypes.GraphQLResponse = runBlocking {
                withTimeout(Duration.ofMinutes(2)) {
                    client.post("$baseUrl/.api/graphql?Search=") {
                        body = SearchTypes.GraphQlRequest(SearchTypes.SearchVaraibles(search))
                    }
                }
            }
            response.errors?.let {
                printlnError("ERROR $it")
            }
            return response.data?.search?.results?.results.orEmpty().mapNotNull {
                internalNameToSearchResult(searchHostName, it.repository?.name)
            }.toSet()
        } catch (e: Exception) {
            NotificationsOperator.show(
                title = "Search failed",
                body = e.message ?: "Exception running search",
                type = NotificationType.ERROR,
            )
            e.printStackTrace()
            return emptySet()
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
}
