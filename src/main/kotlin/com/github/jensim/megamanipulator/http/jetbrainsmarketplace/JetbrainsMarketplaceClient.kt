package com.github.jensim.megamanipulator.http.jetbrainsmarketplace

import com.github.jensim.megamanipulator.http.unwrap
import com.github.jensim.megamanipulator.settings.SerializationHolder.confCompact
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

class JetbrainsMarketplaceClient {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val versionsUrl = "https://plugins.jetbrains.com/api/plugins/16396/updates?channel=&size=1"

    fun getLatestVersion(): String? = try {
        val client = HttpClient(Apache) {
            install(ContentNegotiation) {
                jackson {
                    confCompact()
                }
            }
        }
        runBlocking {
            val response = client.get(versionsUrl)
            val versions: Array<PluginVersionResponse> = response.unwrap()
            versions.firstOrNull()?.version
        }
    } catch (e: Exception) {
        logger.error("Failed fetching the latest version from JetBrains marketplace", e)
        null
    }
}
