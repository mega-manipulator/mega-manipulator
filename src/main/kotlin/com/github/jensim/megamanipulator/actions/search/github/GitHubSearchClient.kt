package com.github.jensim.megamanipulator.actions.search.github

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComRepo
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
import java.util.regex.Pattern

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
                "code" -> search(params["q"]!!, searchHostName, settings, client, "code") { it: GitHubCode -> it.repository }
                "commits" -> search(params["q"]!!, searchHostName, settings, client, "commits") { it: GitHubCommit -> it.repository }
                null, "",
                "repositories" -> search(params["q"]!!, searchHostName, settings, client, "repositories") { it: GithubComRepo -> it }
                else -> {
                    notificationsOperator.show("Failed GitHub search", "Unknown search type", WARNING)
                    emptySet()
                }
            }
        } catch (e: Exception) {
            val message = "Failed searching Github for '$search'"
            logger.error(message, e)
            notificationsOperator.show("Failed GitHub search", message, WARNING)
            return emptySet()
        }
    }

    private suspend fun <T> search(
        q: String,
        searchHostName: String,
        settings: GithubSearchSettings,
        client: HttpClient,
        type: String,
        repoExtract: (T) -> GithubComRepo
    ): Set<SearchResult> {
        val accumulator = HashSet<SearchResult>()
        var page = 1
        while (true) {
            val response = client.get<HttpResponse>("${settings.baseUrl}/search/$type?q=$q&page=$page") {
                accept(ContentType.Application.Json)
            }
            val searchResponse = response.unwrap<GithubSearchResponse<T>>()
            val items = searchResponse.items.orEmpty().map {
                val repo = repoExtract(it)
                SearchResult(repo.owner.login, repo.name, "github.com", searchHostName)
            }.toSet()
            accumulator.addAll(items)
            if (!searchResponse.incomplete_results) break
            page += 1
        }
        return accumulator
    }

    suspend fun validateAccess(searchHost: String, settings: GithubSearchSettings): String = try {
        val client: HttpClient = httpClientProvider.getClient(searchHost, settings)
        val response: HttpResponse = client.get("${settings.baseUrl}/repos/mega-manipulator/mega-manipulator")
        val scopeString = response.headers["X-OAuth-Scopes"]
        val scopes = scopeString.orEmpty().split(Pattern.compile(",")).map { it.trim() }
        println(scopes)
        val expected = listOf("repo", "delete_repo")
        val missing = expected - scopes
        val missingText = if (missing.isNotEmpty()) "<br>\nmissing scopes: $missing" else ""
        val rateLimit = response.headers["X-RateLimit-Limit"].orEmpty()
        val rateLimitText: String = if (rateLimit == "") "<br>\nNo rate limit header in response, bad token?" else if ((rateLimit.toIntOrNull() ?: 0) < 100) "\nRateLimit is low, token is probably not setup correctly" else ""
        "${response.status.value}:${response.status.description}$missingText$rateLimitText"
    } catch (e: Exception) {
        logger.error("Failed request", e)
        "Client error: ${e.message}"
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
