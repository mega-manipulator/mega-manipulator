package com.github.jensim.megamanipulator.actions.vcs.githubcom

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.regex.Pattern
import kotlin.math.max

object GitHubValidation {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val lastRequestTime = AtomicLong(0)

    suspend fun validateAccess(baseUrl: String, client: HttpClient): String? = try {
        val response: HttpResponse = rateLimitRetry(false) { client.get("$baseUrl/repos/mega-manipulator/mega-manipulator") }
        val scopeString = response.headers["X-OAuth-Scopes"]
        val scopes = scopeString.orEmpty().split(Pattern.compile(","))
            .map { it.trim() }
        val expected = listOf("repo", "delete_repo")
        val missing = expected - scopes
        val missingText = if (missing.isNotEmpty()) {
            "<br>\nmissing scopes: $missing"
        } else {
            ""
        }
        val rateLimit = response.headers["X-RateLimit-Limit"].orEmpty()
        val rateLimitText: String = if (rateLimit == "") {
            "<br>\nNo rate limit header in response, bad token?"
        } else if ((rateLimit.toIntOrNull() ?: 0) < 100) {
            "\nRateLimit is low, token is probably not setup correctly"
        } else {
            ""
        }
        val validation = "$missingText$rateLimitText"
        if (validation.isEmpty()) {
            null
        } else {
            "${response.status.value}:${response.status.description}$validation"
        }
    } catch (e: Exception) {
        logger.error("Failed request", e)
        "Client error: ${e.message}"
    }

    suspend fun rateLimitRetry(
        /**
         * https://docs.github.com/en/rest/guides/best-practices-for-integrators#dealing-with-rate-limits
         * True for POST, PATCH, PUT, or DELETE
         */
        preFlightSleep: Boolean,
        request: suspend () -> HttpResponse,
    ): HttpResponse {
        var attempt = 1
        while (true) {
            val now = System.currentTimeMillis()
            val last = lastRequestTime.getAndSet(now) + if (preFlightSleep) 1_000 else 0
            val sleep = last - now
            if (sleep > 0) {
                logger.info("Sleeping for $sleep ms, as we've been rate limited")
                delay(sleep)
                logger.info("Sleep over, back to work")
            }

            val response = request()
            response.getRateLimitReset()?.let {
                lastRequestTime.set(it)
            }
            if (response.status.isSuccess()) {
                return response
            }
            if (attempt >= 3) {
                logger.warn("Failed 3 times, returning last response ${response.request.method.value}:${response.request.url}")
                return response
            } else if (delayIfRateLimit(response)) {
                attempt++
            } else {
                return response
            }
        }
    }

    /**
     * Returns true if and when it's okay to keep on going
     */
    suspend fun delayIfRateLimit(
        response: HttpResponse,
        maxSleep: Long = 70_000L,
        preSleepAction: (sleep: Long) -> Unit = {}
    ): Boolean {
        if (response.status.isSuccess()) {
            return false
        } else if (response.status.value == 403) {
            response.getRateLimitReset()?.let { sleep ->
                if (sleep in -1..maxSleep) {
                    preSleepAction(sleep)
                    delay(max(1L, sleep))
                    return true
                }
            }
            if (response.headers["Retry-After"] != null) {
                val retryAfter = response.headers["Retry-After"]
                retryAfter?.toIntOrNull()?.let { seconds ->
                    val millis = seconds * 1010L
                    if (millis in (1..maxSleep)) {
                        preSleepAction(millis)
                        delay(millis)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun HttpResponse.getRateLimitReset(): Long? {
        if (status.value == 403) {
            if (headers["X-RateLimit-Remaining"] == "0") {
                val reset = headers["X-RateLimit-Reset"]?.toIntOrNull() ?: return null
                val sleep = System.currentTimeMillis() - reset
                if (sleep > 0) {
                    return sleep
                }
            }
        }
        return null
    }
}
