package com.github.jensim.megamanipulator.http

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.settings.AuthMethod
import com.github.jensim.megamanipulator.settings.CodeHostSettings
import com.github.jensim.megamanipulator.settings.HttpsOverride
import com.github.jensim.megamanipulator.settings.IntelliJPasswordsOperator
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.SearchHostSettings
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.intellij.notification.NotificationType
import com.intellij.util.Base64
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.request.headers
import kotlinx.serialization.json.Json
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.ssl.SSLContextBuilder
import java.security.cert.X509Certificate

class HttpClientProvider(
    private val settingsFileOperator: SettingsFileOperator,
    private val passwordsOperator: PasswordsOperator,
    private val notificationsOperator: NotificationsOperator,
) {

    companion object {
        val instance by lazy {
            HttpClientProvider(
                settingsFileOperator = SettingsFileOperator.instance,
                passwordsOperator = IntelliJPasswordsOperator.instance,
                notificationsOperator = NotificationsOperator.instance,
            )
        }
    }

    private class TrustAnythingStrategy : TrustStrategy {
        override fun isTrusted(p0: Array<out X509Certificate>?, p1: String?): Boolean = true
    }

    private fun bakeClient(installs: HttpClientConfig<ApacheEngineConfig>.() -> Unit): HttpClient = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = KotlinxSerializer(
                json = Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = true
                }
            )
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

    fun getClient(searchHostName: String, settings: SearchHostSettings): HttpClient {
        val httpsOverride: HttpsOverride? = settingsFileOperator.readSettings()?.resolveHttpsOverride(searchHostName)
        val password: String = getPassword(settings.authMethod, settings.baseUrl, settings.username)
        return getClient(httpsOverride, settings.authMethod, settings.username, password)
    }

    fun getClient(searchHostName: String, codeHostName: String, settings: CodeHostSettings): HttpClient {
        val httpsOverride: HttpsOverride? = settingsFileOperator.readSettings()?.resolveHttpsOverride(searchHostName, codeHostName)
        val password: String = getPassword(settings.authMethod, settings.baseUrl, settings.username ?: "token")
        return getClient(httpsOverride, settings.authMethod, settings.username, password)
    }

    private fun getPassword(authMethod: AuthMethod, baseUrl: String, username: String?) = try {
        when (authMethod) {
            AuthMethod.ACCESS_TOKEN -> passwordsOperator.getPassword(username!!, baseUrl)
            AuthMethod.JUST_TOKEN -> passwordsOperator.getPassword("token", baseUrl)
        }!!
    } catch (e: Exception) {
        notificationsOperator.show(
            title = "Password not set",
            body = "Password was not set for $authMethod:$username@$baseUrl",
            type = NotificationType.WARNING
        )
        throw e
    }

    fun getClient(httpsOverride: HttpsOverride?, authMethod: AuthMethod, username: String? = null, password: String): HttpClient {
        return bakeClient {
            install(HttpTimeout)
            when (httpsOverride) {
                HttpsOverride.ALLOW_SELF_SIGNED_CERT -> trustSelfSignedClient()
                HttpsOverride.ALLOW_ANYTHING -> trustAnyClient()
            }
            when (authMethod) {
                AuthMethod.ACCESS_TOKEN -> installBasicAuth(username!!, password)
                AuthMethod.JUST_TOKEN -> installTokenAuth(password)
            }
        }
    }

    private fun HttpClientConfig<ApacheEngineConfig>.installBasicAuth(username: String, password: String) {
        defaultRequest {
            headers {
                append("Content-Type", "application/json")
                append("Accept", "application/json")
                append("Authorization", "Basic ${Base64.encode("$username:$password".toByteArray())}")
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
