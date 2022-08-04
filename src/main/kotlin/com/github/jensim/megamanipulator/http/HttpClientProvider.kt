package com.github.jensim.megamanipulator.http

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder.confCompact
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.AuthMethod.JUST_TOKEN
import com.github.jensim.megamanipulator.settings.types.AuthMethod.NONE
import com.github.jensim.megamanipulator.settings.types.AuthMethod.USERNAME_TOKEN
import com.github.jensim.megamanipulator.settings.types.HostWithAuth
import com.github.jensim.megamanipulator.settings.types.HttpLoggingLevel
import com.github.jensim.megamanipulator.settings.types.HttpsOverride
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.orDefault
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettings
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.apache.Apache
import io.ktor.client.engine.apache.ApacheEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.serialization.jackson.jackson
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.TrustSelfSignedStrategy
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.ssl.SSLContextBuilder
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext

class HttpClientProvider @NonInjectable constructor(
    project: Project,
    settingsFileOperator: SettingsFileOperator?,
    passwordsOperator: PasswordsOperator?,
    notificationsOperator: NotificationsOperator?,
) {

    private val noopHostnameVerifier = NoopHostnameVerifier()
    private val trustAnySslContext: SSLContext = SSLContextBuilder
        .create()
        .loadTrustMaterial(TrustAnythingStrategy())
        .build()
    private val selfSignedSslContext: SSLContext = SSLContextBuilder
        .create()
        .loadTrustMaterial(TrustSelfSignedStrategy())
        .build()
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

    private fun bakeClient(
        httpLoggingLevel: HttpLoggingLevel,
        installs: HttpClientConfig<ApacheEngineConfig>.() -> Unit,
    ): HttpClient = HttpClient(Apache) {
        install(ContentNegotiation) {
            jackson {
                confCompact()
            }
        }
        install(Logging) {
            logger = io.ktor.client.plugins.logging.Logger.DEFAULT
            level = httpLoggingLevel.toKtorLevel()
        }
        installs()
    }

    private fun HttpLoggingLevel.toKtorLevel() = when (this) {
        HttpLoggingLevel.ALL -> LogLevel.ALL
        HttpLoggingLevel.HEADERS -> LogLevel.HEADERS
        HttpLoggingLevel.BODY -> LogLevel.BODY
        HttpLoggingLevel.INFO -> LogLevel.INFO
        HttpLoggingLevel.NONE -> LogLevel.NONE
    }

    private fun HttpClientConfig<ApacheEngineConfig>.trustAny() {
        engine {
            customizeClient {
                setSSLContext(trustAnySslContext)
                setSSLHostnameVerifier(noopHostnameVerifier)
            }
        }
    }

    private fun HttpClientConfig<ApacheEngineConfig>.trustSelfSigned() {
        engine {
            customizeClient {
                setSSLContext(selfSignedSslContext)
                setSSLHostnameVerifier(noopHostnameVerifier)
            }
        }
    }

    fun getClient(
        searchHostName: String,
        searchHostSettings: SearchHostSettings,
    ): HttpClient {
        val settings = settingsFileOperator.readSettings()
        val httpLoggingLevel = settings?.httpLoggingLevel.orDefault()
        val httpsOverride: HttpsOverride? = settings?.resolveHttpsOverride(searchHostName)
        val password: String = getPassword(searchHostSettings.authMethod, searchHostSettings.baseUrl, searchHostSettings.username)
        return getClient(httpLoggingLevel, httpsOverride, searchHostSettings, password)
    }

    fun getClient(
        searchHostName: String,
        codeHostName: String,
        codeHostSettings: CodeHostSettings,
    ): HttpClient {
        val settings = settingsFileOperator.readSettings()
        val httpLoggingLevel = settings?.httpLoggingLevel.orDefault()
        val httpsOverride: HttpsOverride? =
            settings?.resolveHttpsOverride(searchHostName, codeHostName)
        val password: String = getPassword(codeHostSettings.authMethod, codeHostSettings.baseUrl, codeHostSettings.username ?: "token")
        return getClient(httpLoggingLevel, httpsOverride, codeHostSettings, password)
    }

    private fun getPassword(authMethod: AuthMethod, baseUrl: String, username: String?) = try {
        when (authMethod) {
            USERNAME_TOKEN -> passwordsOperator.getPassword(username!!, baseUrl)
            JUST_TOKEN -> passwordsOperator.getPassword(username ?: "token", baseUrl)
            NONE -> ""
        } ?: throw NullPointerException("Password not set")
    } catch (e: Exception) {
        notificationsOperator.show(
            title = "Password not set",
            body = "Password was not set for $authMethod: $username@$baseUrl",
            type = NotificationType.WARNING
        )
        throw e
    }

    fun getClient(
        httpLoggingLevel: HttpLoggingLevel,
        httpsOverride: HttpsOverride?,
        auth: HostWithAuth,
        password: String,
    ): HttpClient {
        return bakeClient(httpLoggingLevel) {
            install(HttpTimeout) {
                connectTimeoutMillis = 1_000
                requestTimeoutMillis = 60_000
                socketTimeoutMillis = 60_000
            }
            when (httpsOverride) {
                HttpsOverride.ALLOW_ANYTHING -> trustAny()
                HttpsOverride.ALLOW_SELF_SIGNED -> trustSelfSigned()
                else -> {}
            }
            auth.getAuthHeaderValue(password)?.let {
                installBasicAuth(it)
            }
        }
    }

    private fun HttpClientConfig<ApacheEngineConfig>.installBasicAuth(headerValue: String) {
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
        // return receive()
    } else {
        val body: String = bodyAsText()
        throw RuntimeException("Response status ${status.value} from ${request.url} with message: $body")
    }
}
