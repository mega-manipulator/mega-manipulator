package com.github.jensim.megamanipulatior.actions.search

import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.jetbrains.rd.util.printlnError
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.headers
import io.ktor.client.request.post
import java.security.cert.X509Certificate
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.ssl.SSLContextBuilder

object SearchOperator {

    private class TrustAnythingStrategy : TrustStrategy {
        override fun isTrusted(p0: Array<out X509Certificate>?, p1: String?): Boolean = true
    }

    private val client: HttpClient
        get() = HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer()

            }
            engine {
                customizeClient {
                    setSSLContext(
                        SSLContextBuilder
                            .create()
                            .loadTrustMaterial(TrustAnythingStrategy())
                            .build()
                    )
                    setSSLHostnameVerifier(NoopHostnameVerifier())
                }
            }
        }

    suspend fun search(search: String): Set<SearchResult> {
        try {
            val token: String = System.getenv("SRC_ACCESS_TOKEN")
            val baseUrl = SettingsFileOperator.readSettingsOrNull()?.sourceGraphSettings?.baseUrl!!

            val response: SearchTypes.GraphQLResponse = client.post("$baseUrl/.api/graphql?Search=") {
                body = SearchTypes.GraphQlRequest(SearchTypes.SearchVaraibles(search))
                headers {
                    append("Authorization", "token $token")
                    append("Content-Type", "application/json")
                    append("Accept", "application/json")
                }

            }
            response.errors?.let {
                printlnError("ERROR $it")
            }
            return response.data?.search?.results?.results.orEmpty().mapNotNull {
                internalNameToSearchResult(it.repository?.name)
            }.toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptySet()
        }
    }

    private fun internalNameToSearchResult(name: String?): SearchResult? {
        val parts = name?.split('/')
        return if (parts?.size != 3) {
            null
        } else {
            SearchResult(codeHostName = parts[0], project = parts[1], repo = parts[2])
        }
    }
}