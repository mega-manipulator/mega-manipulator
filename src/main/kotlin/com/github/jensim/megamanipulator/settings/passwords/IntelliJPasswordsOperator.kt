package com.github.jensim.megamanipulator.settings.passwords

import com.fasterxml.jackson.core.type.TypeReference
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SerializationHolder.objectMapper
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.ui.PasswordDialogFactory
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import com.intellij.serviceContainer.NonInjectable
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.concurrent.NotThreadSafe
import javax.swing.JComponent

@NotThreadSafe
class IntelliJPasswordsOperator @NonInjectable constructor(
    project: Project,
    notificationsOperator: NotificationsOperator?,
) : PasswordsOperator {

    companion object {
        private const val service = "mega-manipulator"
        private val typeRef: TypeReference<HashMap<String, String?>> = object : TypeReference<HashMap<String, String?>>() {}
    }

    constructor(project: Project) : this(project, null)

    private val logger = LoggerFactory.getLogger(javaClass)
    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)

    private val serviceUsername: String by lazy { System.getProperty("user.name") ?: service }
    private val passwordSetMap: MutableMap<String, Boolean> = ConcurrentHashMap()

    private fun usernameToKey(username: String, baseUrl: String) = "${username}___$baseUrl"

    @SuppressWarnings(value = ["ComplexCondition"])
    override fun promptForPassword(focusComponent: JComponent, authMethod: AuthMethod, username: String, baseUrl: String, callback: () -> Unit) {

        PasswordDialogFactory.askForPassword(focusComponent, authMethod, username, baseUrl) { password ->
            if (password.isNotBlank()) {
                setPassword(username, password, baseUrl)
            } else {
                deletePassword(username, baseUrl)
            }
            try {
                callback()
            } catch (e: Exception) {
                val msg = "Something went wrong in the callback after setting password"
                logger.error(msg, e)
                notificationsOperator.show(msg, "See IDE logs for more details", NotificationType.ERROR)
            }
        }
    }

    override fun isPasswordSet(username: String, baseUrl: String): Boolean =
        usernameToKey(username, baseUrl).let { userKey ->
            passwordSetMap.computeIfAbsent(userKey) { getPassword(username, baseUrl) != null }
        }

    private fun deletePassword(username: String, baseUrl: String) {
        setPassword(username, null, baseUrl)
    }

    override fun getPassword(username: String, baseUrl: String): String? {
        return getPassword(usernameToKey(username, baseUrl))
    }

    private fun getPassword(usernameKey: String): String? {
        val credentialAttributes: CredentialAttributes? = createCredentialAttributes(service, serviceUsername)
        return if (credentialAttributes == null) {
            notificationsOperator.show(
                "Failed setting password",
                "Could not create CredentialAttributes",
                NotificationType.WARNING
            )
            null
        } else {
            PasswordSafe.instance.getPassword(credentialAttributes)?.let { passMapStr ->
                val passMap: Map<String, String?> = objectMapper.readValue(passMapStr, typeRef)
                passMap[usernameKey]
            }
        }
    }

    private fun setPassword(username: String, password: String?, baseUrl: String) {
        setPassword(usernameToKey(username, baseUrl), password)
    }

    private fun setPassword(usernameKey: String, password: String?) {
        val credentialAttributes: CredentialAttributes? = createCredentialAttributes(service, serviceUsername)
        if (credentialAttributes == null) {
            notificationsOperator.show(
                "Failed setting password",
                "Could not create CredentialAttributes",
                NotificationType.WARNING
            )
        } else {
            val preexisting: String? = PasswordSafe.instance.getPassword(credentialAttributes)
            if (preexisting == null) {
                val passwordsMap = mapOf(usernameKey to password)
                val passMapStr: String = objectMapper.writeValueAsString(passwordsMap)
                val credentials = Credentials(serviceUsername, passMapStr)
                PasswordSafe.instance.set(credentialAttributes, credentials)
            } else {
                PasswordSafe.instance.getPassword(credentialAttributes)?.let { passMapStr: String ->
                    val passMap: MutableMap<String, String?> = objectMapper.readValue(passMapStr, typeRef)
                    passMap[usernameKey] = password
                    val passMapStrMod = objectMapper.writeValueAsString(passMap)
                    val credentials = Credentials(serviceUsername, passMapStrMod)
                    PasswordSafe.instance.set(credentialAttributes, credentials)
                }
            }
        }
        passwordSetMap.clear()
    }
}
