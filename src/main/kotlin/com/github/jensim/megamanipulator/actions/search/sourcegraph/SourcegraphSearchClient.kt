package com.github.jensim.megamanipulator.actions.search.sourcegraph

import com.expediagroup.graphql.client.jackson.GraphQLClientJacksonSerializer
import com.expediagroup.graphql.client.ktor.GraphQLKtorClient
import com.expediagroup.graphql.client.types.GraphQLClientResponse
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.graphql.generated.sourcegraph.Search
import com.github.jensim.megamanipulator.graphql.generated.sourcegraph.Search.Result
import com.github.jensim.megamanipulator.graphql.generated.sourcegraph.search.CommitSearchResult
import com.github.jensim.megamanipulator.graphql.generated.sourcegraph.search.FileMatch
import com.github.jensim.megamanipulator.graphql.generated.sourcegraph.search.Repository2
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.types.searchhost.SourceGraphSettings
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import com.jetbrains.rd.util.printlnError
import org.slf4j.LoggerFactory
import java.net.URL

class SourcegraphSearchClient @NonInjectable constructor(
    project: Project,
    httpClientProvider: HttpClientProvider?,
    notificationsOperator: NotificationsOperator?,
) {

    constructor(project: Project) : this(
        project = project,
        httpClientProvider = null,
        notificationsOperator = null
    )

    private val httpClientProvider: HttpClientProvider by lazyService(project, httpClientProvider)
    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)

    private val graphQLClientJacksonSerializer = GraphQLClientJacksonSerializer()
    private val log = LoggerFactory.getLogger(this.javaClass)

    suspend fun search(searchHostName: String, settings: SourceGraphSettings, search: String): Set<SearchResult> {
        try {
            val raw = rawSearch(searchHostName, settings, search)
            if (!raw.errors.isNullOrEmpty()) {
                throw RuntimeException("${raw.errors}")
            }
            raw.data?.search?.results?.alert?.let {
                printlnError("ALERT $it")
                notificationsOperator.show(
                    title = "Alert from SourceGraph search result",
                    body = "More info in application logs",
                    type = WARNING
                )
            }
            return raw.data?.search?.results?.results.orEmpty().mapNotNull {
                val repoName = when (it) {
                    is CommitSearchResult -> it.commit.repository.name
                    is FileMatch -> it.repository.name
                    is Repository2 -> it.name
                    else -> null
                }
                internalNameToSearchResult(searchHostName, repoName)
            }.toSet()
        } catch (e: Exception) {
            log.error("Exception caught running sourcegraph search", e)
            notificationsOperator.show(
                title = "Search failed (More data in app logs)",
                body = e.message ?: "Exception running search",
                type = ERROR,
            )
            return emptySet()
        }
    }

    private suspend fun rawSearch(
        searchHostName: String,
        settings: SourceGraphSettings,
        searchString: String
    ): GraphQLClientResponse<Search.Result> {
        val client = getClient(searchHostName, settings)
        val searchVars = Search.Variables(searchString)
        val search = Search(searchVars)
        return client.execute(search)
    }

    private fun getClient(searchHost: String, settings: SourceGraphSettings) = GraphQLKtorClient(
        url = URL("${settings.baseUrl}/.api/graphql"),
        httpClient = httpClientProvider.getClient(searchHost, settings),
        serializer = graphQLClientJacksonSerializer,
    )

    private fun internalNameToSearchResult(searchHostName: String, name: String?): SearchResult? {
        val parts = name?.split('/')
        return if (parts?.size != 3) {
            null
        } else {
            SearchResult(searchHostName = searchHostName, codeHostName = parts[0], project = parts[1], repo = parts[2])
        }
    }

    suspend fun validateToken(searchHostName: String, settings: SourceGraphSettings): String = try {
        val response: GraphQLClientResponse<Result> = rawSearch(searchHostName, settings, "count:1 timeout:10s f")
        when {
            !response.errors.isNullOrEmpty() -> {
                log.warn("Errors from SourceGraph GraphQL response {}", response.errors)
                "ERRORS"
            }
            response.data?.search?.results?.results?.size == 1 -> "OK"
            else -> "NO SEARCH RESULT"
        }
    } catch (e: Exception) {
        log.error("Failed request", e)
        "Client error"
    }
}
