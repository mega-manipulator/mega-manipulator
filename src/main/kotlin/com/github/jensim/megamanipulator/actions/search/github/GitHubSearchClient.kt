package com.github.jensim.megamanipulator.actions.search.github

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GitHubValidation
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GitHubValidation.delayIfRateLimit
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GitHubValidation.rateLimitRetry
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.http.unwrap
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.types.searchhost.GithubSearchSettings
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import com.intellij.util.queryParameters
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import org.slf4j.LoggerFactory
import java.net.URI
import kotlin.math.min

class GitHubSearchClient @NonInjectable constructor(
    project: Project,
    httpClientProvider: HttpClientProvider?,
    notificationsOperator: NotificationsOperator?,
) {

    constructor(project: Project) : this(
        project = project,
        httpClientProvider = null,
        notificationsOperator = null,
    )

    private val logger = LoggerFactory.getLogger(javaClass)
    private val prefix = "https://github.com/search?"

    private val httpClientProvider: HttpClientProvider by lazyService(project, httpClientProvider)
    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)

    suspend fun search(searchHostName: String, settings: GithubSearchSettings, search: String): Set<SearchResult> {
        try {
            val params = validateSearchString(search) ?: return emptySet()
            val client = httpClientProvider.getClient(searchHostName, settings)
            return when (params["type"]) {
                "code" -> search(params["q"]!!, settings, client, "code") { it: GitHubCode -> SearchResult(it.repository.owner.login, it.repository.name, "github.com", searchHostName) }
                "commits" -> search(params["q"]!!, settings, client, "commits") { it: GitHubCommit -> SearchResult(it.repository.owner.login, it.repository.name, "github.com", searchHostName) }
                null, "",
                "repositories" -> search(params["q"]!!, settings, client, "repositories") { it: GithubRepo -> SearchResult(it.owner.login, it.name, "github.com", searchHostName) }
                else -> {
                    notificationsOperator.show("Failed GitHub search", "Unknown search type", WARNING)
                    emptySet()
                }
            }
        } catch (e: Exception) {
            val message = "Failed searching Github for '$search', ${e.message}"
            logger.error(message, e)
            notificationsOperator.show("Failed GitHub search", message, WARNING)
            return emptySet()
        }
    }

    private suspend inline fun <reified T> search(
        q: String,
        settings: GithubSearchSettings,
        client: HttpClient,
        type: String,
        transformResult: (T) -> SearchResult
    ): Set<SearchResult> {
        val accumulator = HashSet<SearchResult>()
        var page = 1
        while (true) {
            val limit: Int = min(100, 1000 - accumulator.size)
            if (limit < 1) {
                val msg = "Paginated search result accumulated to ${accumulator.size}, there is a cap at 1000 to protect you & your IDE from overflows. Try to limit your search a bit more."
                logger.warn(msg)
                notificationsOperator.show(
                    title = "Search result capped",
                    body = msg,
                    type = WARNING,
                )
                break
            }
            val perPage = "per_page=$limit"
            val urlString = "${settings.baseUrl}/search/$type?$perPage&q=$q&page=$page"
            val response: HttpResponse = rateLimitRetry(false) {
                client.get(urlString) {
                    accept(ContentType.Application.Json)
                }
            }
            if (gottaRetry(response)) continue
            val searchResponse = try {
                response.unwrap<GithubSearchResponse<T>>()
            } catch (e: Exception) {
                logger.error("Failed searching for $urlString", e)
                break
            }

            val items = searchResponse.items.orEmpty().map { transformResult(it) }.toSet()
            if (items.isEmpty()) {
                logger.debug("No more items found")
                break
            }
            accumulator.addAll(items)
            page += 1
        }
        return accumulator
    }

    private suspend fun gottaRetry(response: HttpResponse): Boolean {
        return delayIfRateLimit(response) { sleep ->
            notificationsOperator.show("Search Rate limit hit", "Will reset in ${sleep / 1000} seconds")
        }
    }

    suspend fun validateAccess(searchHost: String, settings: GithubSearchSettings): String? = try {
        val client: HttpClient = httpClientProvider.getClient(searchHost, settings)
        GitHubValidation.validateAccess(settings.baseUrl, client)
    } catch (e: Exception) {
        val msg = "Failed setting up client: ${e.message}"
        logger.error(msg, e)
        msg
    }

    private fun validateSearchString(search: String): Map<String, String>? {
        if (!search.startsWith(prefix)) {
            notificationsOperator.show("Bad search string", "Search string must start with $prefix", WARNING)
            return null
        }
        val params = URI(search).queryParameters
        if (!params.containsKey("q")) {
            notificationsOperator.show("Bad search string", "Search string must query param 'q'", WARNING)
            return null
        }
        if (!params.containsKey("type")) {
            notificationsOperator.show("Bad search string", "Search string must query param 'type'", WARNING)
            return mapOf("q" to params["q"]!!)
        }
        return params
    }
}
