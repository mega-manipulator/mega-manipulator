package com.github.jensim.megamanipulator.actions.search.hound

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jensim.megamanipulator.actions.search.SearchResult
import com.github.jensim.megamanipulator.actions.search.hound.HoundTypes.HoundRepo
import com.github.jensim.megamanipulator.actions.search.hound.HoundTypes.HoundSearchResults
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.http.unwrap
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder.objectMapper
import com.github.jensim.megamanipulator.settings.types.searchhost.HoundSettings
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readText
import org.slf4j.LoggerFactory

class HoundClient @NonInjectable constructor(
    project: Project,
    httpClientProvider: HttpClientProvider?,
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    constructor(project: Project) : this(project, null)

    private val httpClientProvider: HttpClientProvider by lazyService(project, httpClientProvider)

    suspend fun search(searchHostName: String, settings: HoundSettings, search: String): Set<SearchResult> {
        val client = httpClientProvider.getClient(searchHostName, settings)
        val response: HttpResponse = client.get("${settings.baseUrl}/api/v1/repos")
        val repos = response.unwrap<Map<String, HoundRepo>>().filterValues { it.vcs == "git" }
        val rawResp: HttpResponse = client.get("${settings.baseUrl}/api/v1/search") {
            parameter("q", search)
            parameter("stats", "nope") // fosho or nope (true or false)
            parameter("repos", "*")
            parameter("rng", ":1000")
            parameter("files", "")
            parameter("i", "fosho")
            header("Accept", "application/json")
        }
        val body: String = rawResp.readText()
        val searchResp: HoundSearchResults = objectMapper.readValue(body)
        return searchResp.Results.keys.mapNotNull { repos[it]?.url?.gitUrlToResult(searchHostName) }.toSet()
    }

    suspend fun validate(searchHostName: String, settings: HoundSettings): String = try {
        val client = httpClientProvider.getClient(searchHostName, settings)
        val response: HttpResponse = client.get("${settings.baseUrl}/api/v1/repos") {
            header("Accept", "application/json")
        }
        "${response.status.value}:${response.status.description}"
    } catch (e: Exception) {
        val msg = "Client error: ${e.message}"
        logger.error(msg, e)
        msg
    }

    private fun String.gitUrlToResult(searchHost: String): SearchResult? = try {
        val center: String =
            if ((startsWith("http") || startsWith("ssh://")) && endsWith(".git")) { // https://github.com/hound-search/hound.git
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
        val msg = "Failed converting gitUrls to results"
        logger.error(msg, e)
        null
    }
}
