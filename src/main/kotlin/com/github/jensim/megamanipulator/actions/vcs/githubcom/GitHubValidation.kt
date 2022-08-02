package com.github.jensim.megamanipulator.actions.vcs.githubcom

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

object GitHubValidation {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun validateAccess(baseUrl: String, client: HttpClient): String = try {
        val response: HttpResponse = client.get("$baseUrl/repos/mega-manipulator/mega-manipulator")
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
}
