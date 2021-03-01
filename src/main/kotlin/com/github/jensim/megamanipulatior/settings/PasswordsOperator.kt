package com.github.jensim.megamanipulatior.settings

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.notification.NotificationType
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.annotation.concurrent.NotThreadSafe
import javax.swing.JOptionPane

@NotThreadSafe
object PasswordsOperator {

    private const val service = "mega-manipulator"
    private val serviceUsername: String by lazy { System.getProperty("user.name") ?: service }

    fun promptForPassword(username: String? = null, baseUrl: String): String {
        val usernameField = JBTextField(30)
        val passwordField = JBPasswordField().apply { columns = 30 }
        val content = panel {
            row {
                label("Please provide credentials for $baseUrl")
            }
            row {
                when (username) {
                    null -> {
                        label("Username:")
                        component(usernameField)
                    }
                    "token" -> label("TOKEN login method")
                    else -> label("Username: $username")
                }
            }
            row {
                label("Password:")
                component(passwordField)
            }
        }
        while ((username == null || usernameField.text.isNullOrEmpty()) && passwordField.password.concatToString().isNullOrEmpty()) {
            JOptionPane.showConfirmDialog(null, content)
        }
        val username = username ?: usernameField.text
        val password = passwordField.password.concatToString().trim()

        setPassword(username, password, baseUrl)
        return password
    }

    fun isPasswordSet(username: String, baseUrl: String): Boolean = getPassword(username, baseUrl) != null

    fun deletePasswords(username: String, baseUrl: String) {
        val username = "${serviceUsername}___${username}___${baseUrl}"
        val credentialAttributes: CredentialAttributes? = createCredentialAttributes(service, username)
        if (credentialAttributes == null) {
            NotificationsOperator.show("Failed deleting password", "Could not create CredentialAttributes", NotificationType.WARNING)
        } else {
            PasswordSafe.instance.set(credentialAttributes, null)
        }
    }

    fun getPassword(username: String, baseUrl: String): String? {
        val username = "${serviceUsername}___${username}___${baseUrl}"
        val credentialAttributes: CredentialAttributes? = createCredentialAttributes(service, username)
        return if (credentialAttributes == null) {
            NotificationsOperator.show("Failed setting password", "Could not create CredentialAttributes", NotificationType.WARNING)
            null
        } else {
            PasswordSafe.instance.getPassword(credentialAttributes)
        }
    }

    fun setPassword(username: String, password: String, baseUrl: String) {
        val username = "${serviceUsername}___${username}___${baseUrl}"
        val credentialAttributes: CredentialAttributes? = createCredentialAttributes(service, username)
        if (credentialAttributes == null) {
            NotificationsOperator.show("Failed setting password", "Could not create CredentialAttributes", NotificationType.WARNING)
        } else {
            val credentials = Credentials(username, password)
            PasswordSafe.instance.set(credentialAttributes, credentials)
        }
    }
}
