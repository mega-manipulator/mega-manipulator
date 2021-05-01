package com.github.jensim.megamanipulator.actions.search.hound

import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.search.hound.HoundTypes.HoundRepo
import com.github.jensim.megamanipulator.actions.search.hound.HoundTypes.HoundSearchResults
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.SearchHostSettings.HoundSettings
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HoundClient(
    private val httpClientProvider: HttpClientProvider,
    private val json: Json,
) {

    suspend fun search(searchHostName: String, settings: HoundSettings, search: String): Set<SearchResult> {
        val client = httpClientProvider.getClient(searchHostName, settings)
        val repos = client.get<Map<String, HoundRepo>>("${settings.baseUrl}/api/v1/repos").filterValues { it.vcs == "git" }
        val rawResp: HttpResponse = client.get("${settings.baseUrl}/api/v1/search") {
            parameter("q", search)
            parameter("stats", "nope") // fosho or nope (true or false)
            parameter("repos", "*")
            parameter("rng", ":1000")
            parameter("files", "")
            parameter("i", "fosho")
        }
        val body = rawResp.readText()
        val searchResp: HoundSearchResults = json.decodeFromString(body)
        return searchResp.Results.keys.mapNotNull { repos[it]?.url?.gitUrlToResult(searchHostName) }.toSet()
    }

    suspend fun validate(searchHostName: String, settings: HoundSettings): String = try {
        val client = httpClientProvider.getClient(searchHostName, settings)
        val response = client.get<HttpResponse>("${settings.baseUrl}/api/v1/repos")
        "${response.status.value}:${response.status.description}"
    } catch (e: Exception) {
        e.printStackTrace()
        "Client error"
    }

    private fun String.gitUrlToResult(searchHost: String): SearchResult? = try {
        val center: String = if ((startsWith("http") || startsWith("ssh://")) && endsWith(".git")) { // https://github.com/hound-search/hound.git
            substringAfter("://").dropLast(4)
        } else if (startsWith("git@") && endsWith(".git")) { // git@github.com:hound-search/hound.git
            drop(4).dropLast(4)
        } else {
            ""
        }
        val parts: List<String> = if (center.count { it == '/' } == 2) {
            center.split('/')
        } else if (center.count { it == '/' } == 1 && (1..2).contains(center.count { it == ':' })) {
            val first = center.substringBefore('/')
            listOf(first.substringBeforeLast(':'), first.substringAfterLast(':'), center.substringAfter('/'))
        } else {
            emptyList()
        }
        if (parts.size == 3) {
            SearchResult(searchHostName = searchHost, codeHostName = parts[0], project = parts[1], repo = parts[2])
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
