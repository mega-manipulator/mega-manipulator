package com.github.jensim.megamanipulator.http

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.AuthMethod.NONE
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.settings.types.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.HostWithAuth
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import org.apache.http.conn.ssl.TrustStrategy
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

class HttpClientProvider @NonInjectable constructor(
    project: Project,
    settingsFileOperator: SettingsFileOperator?,
    passwordsOperator: PasswordsOperator?,
    notificationsOperator: NotificationsOperator?,
) {

    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)
    private val settingsFileOperator: SettingsFileOperator by lazyService(project, settingsFileOperator)
    private val passwordsOperator: PasswordsOperator by lazyService(project, passwordsOperator)

    constructor(project: Project) : this(
        project = project,
        settingsFileOperator = null,
        passwordsOperator = null,
        notificationsOperator = null
    )

    private class TrustAnythingStrategy : TrustStrategy {
        override fun isTrusted(p0: Array<out X509Certificate>?, p1: String?): Boolean = true
    }

    private fun bakeClient(installs: HttpClientConfig<CIOEngineConfig>.() -> Unit): HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(SerializationHolder.compactJson)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        installs()
    }

    private fun HttpClientConfig<CIOEngineConfig>.trustAnyClient() {
        engine {
            https {
                trustManager = object : X509TrustManager {
                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) = Unit
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                }
            }
        }
    }

    fun getClient(searchHostName: String, settings: SearchHostSettings): HttpClient {
        val httpsOverride: HttpsOverride? = settingsFileOperator.readSettings()?.resolveHttpsOverride(searchHostName)
        val password: String = getPassword(settings.authMethod, settings.baseUrl, settings.username)
        return getClient(httpsOverride, settings, password)
    }

    fun getClient(searchHostName: String, codeHostName: String, settings: CodeHostSettings): HttpClient {
        val httpsOverride: HttpsOverride? =
            settingsFileOperator.readSettings()?.resolveHttpsOverride(searchHostName, codeHostName)
        val password: String = getPassword(settings.authMethod, settings.baseUrl, settings.username ?: "token")
        return getClient(httpsOverride, settings, password)
    }

    private fun getPassword(authMethod: AuthMethod, baseUrl: String, username: String?) = try {
        when (authMethod) {
            USERNAME_TOKEN -> passwordsOperator.getPassword(username!!, baseUrl)
            JUST_TOKEN -> passwordsOperator.getPassword(username ?: "token", baseUrl)
            NONE -> ""
        }!!
    } catch (e: Exception) {
        notificationsOperator.show(
            title = "Password not set",
            body = "Password was not set for $authMethod: $username@$baseUrl",
            type = NotificationType.WARNING
        )
        throw e
    }

    fun getClient(httpsOverride: HttpsOverride?, auth: HostWithAuth, password: String): HttpClient {
        return bakeClient {

            install(HttpTimeout) {
                connectTimeoutMillis = 1_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
            when (httpsOverride) {
                HttpsOverride.ALLOW_ANYTHING -> trustAnyClient()
                else -> {}
            }
            auth.getAuthHeaderValue(password)?.let {
                installBasicAuth(it)
            }
        }
    }

    private fun HttpClientConfig<CIOEngineConfig>.installBasicAuth(headerValue: String) {
        defaultRequest {
            headers {
                header("Authorization", headerValue)
            }
        }
    }
}

suspend inline fun <reified T> HttpResponse.unwrap(): T {
    if (status.isSuccess()) {
        return body()
    } else {
        val body: String = bodyAsText()
        throw RuntimeException("Respone status ${status.value} from ${request.url} with message: $body")
    }
}
