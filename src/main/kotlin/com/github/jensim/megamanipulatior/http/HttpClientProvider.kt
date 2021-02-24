package com.github.jensim.megamanipulatior.http

import com.github.jensim.megamanipulatior.settings.AuthMethod
import com.github.jensim.megamanipulatior.settings.HttpsOverride
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.auth.Auth
import io.ktor.client.features.auth.providers.basic
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.headers
import java.security.cert.X509Certificate
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.ssl.SSLContextBuilder

object HttpClientProvider {

    private class TrustAnythingStrategy : TrustStrategy {
        override fun isTrusted(p0: Array<out X509Certificate>?, p1: String?): Boolean = true
    }

    private fun bakeClient(installs: HttpClientConfig<ApacheEngineConfig>.() -> Unit): HttpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer()
        }
        installs()
    }

    private fun HttpClientConfig<ApacheEngineConfig>.trustSelfSignedClient() {
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

    private fun HttpClientConfig<ApacheEngineConfig>.trustAnyClient() {
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

    fun getClient(httpsOverride: HttpsOverride?, authMethod: AuthMethod, username: String? = null, password: String): HttpClient {
        return bakeClient {
            when (httpsOverride) {
                HttpsOverride.ALLOW_SELF_SIGNED_CERT -> trustSelfSignedClient()
                HttpsOverride.ALLOW_ANYTHING -> trustAnyClient()
            }
            when (authMethod) {
                AuthMethod.TOKEN -> installTokenAuth(password)
                AuthMethod.USERNAME_PASSWORD -> installBasicAuth(username!!, password)
            }
        }
    }

    private fun HttpClientConfig<ApacheEngineConfig>.installBasicAuth(username: String, password: String) {
        install(Auth) {
            basic {
                this.username = username
                this.password = password
            }
            defaultRequest {
                headers {
                    append("Content-Type", "application/json")
                    append("Accept", "application/json")
                }
            }
        }
    }

    private fun HttpClientConfig<ApacheEngineConfig>.installTokenAuth(token: String) {
        defaultRequest {
            headers {
                append("Content-Type", "application/json")
                append("Accept", "application/json")
                append("Authorization", "token $token")
            }
        }
    }
}
