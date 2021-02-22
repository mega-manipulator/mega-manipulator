package com.github.jensim.megamanipulatior.http

import com.github.jensim.megamanipulatior.settings.HttpsOverride
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import java.security.cert.X509Certificate
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.ssl.SSLContextBuilder

object HttpClientProvider {

    private class TrustAnythingStrategy : TrustStrategy {
        override fun isTrusted(p0: Array<out X509Certificate>?, p1: String?): Boolean = true
    }

    private val client: HttpClient by lazy {
        HttpClient(Apache)
    }

    private val trustSelfSignedClient: HttpClient by lazy {
        HttpClient(Apache) {
            install(JsonFeature) {
                serializer = JacksonSerializer()
            }
            engine {
                customizeClient {
                    setSSLContext(
                        SSLContextBuilder
                            .create()
                            .loadTrustMaterial(TrustSelfSignedStrategy())
                            .build()
                    )
                    setSSLHostnameVerifier(NoopHostnameVerifier())
                }
            }
        }
    }

    private val trustAnyClient: HttpClient by lazy {
        HttpClient(Apache) {
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
    }

    fun getClient(httpsOverride: HttpsOverride?): HttpClient = when (httpsOverride) {
        HttpsOverride.ALLOW_SELF_SIGNED_CERT -> trustSelfSignedClient
        HttpsOverride.ALLOW_ANYTHING -> trustAnyClient
        null -> client
    }
}
