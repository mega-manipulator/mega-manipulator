package com.github.jensim.megamanipulatior.settings

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

object SettingsWindow : ToolWindowTab {

    private enum class HostType {
        SEARCH,
        CODE,
    }

    private data class ConfigHostHolder(
        val hostType: HostType,
        val authMethod: AuthMethod,
        val baseUri: String,
        val username: String?,
        val hostNaming: String,

        ) {
        override fun toString(): String = "$hostType: $hostNaming"
    }

    private val label = JBLabel()
    private val configButton = JButton("Validate config")
    private val hostConfigSelect = ComboBox<ConfigHostHolder>()
    private val passwordTestButton = JButton("Test password").apply { isEnabled = false }
    private val passwordSetButton = JButton("Set password").apply { isEnabled = false }

    override val content: JComponent = panel {
        row {
            component(configButton)
            component(hostConfigSelect)
        }
        row {
            component(label)
            component(passwordTestButton)
        }
        row {
            label(" ")
            component(passwordSetButton)
        }
    }

    init {
        //hostConfigSelect.addCellRenderer({ "${it.hostType}: ${it.hostNaming}" }, { it.toString() })
        hostConfigSelect.addItemListener {
            if (hostConfigSelect.selectedItem == null) {
                passwordTestButton.isEnabled = false
                passwordSetButton.isEnabled = false
            } else {
                passwordTestButton.isEnabled = true
                passwordSetButton.isEnabled = true
            }
        }
        configButton.addActionListener {
            refresh()
        }
        passwordTestButton.addActionListener {
            val conf = hostConfigSelect.selectedItem as ConfigHostHolder
            when (Pair(conf.authMethod, conf.username != null)) {
                Pair(AuthMethod.TOKEN, false),
                Pair(AuthMethod.TOKEN, true) -> {
                    if (PasswordsOperator.isPasswordSet("token", conf.baseUri)) {
                        NotificationsOperator.show("Password is set", "Password is set for ${conf.baseUri}", NotificationType.INFORMATION)
                    } else {
                        NotificationsOperator.show("Password not set", "Password not set for ${conf.baseUri}", NotificationType.WARNING)
                    }
                }
                Pair(AuthMethod.USERNAME_PASSWORD, true) -> {
                    if (PasswordsOperator.isPasswordSet(conf.username!!, conf.baseUri)) {
                        NotificationsOperator.show("Password is set", "Password is set for ${conf.baseUri}", NotificationType.INFORMATION)
                    } else {
                        NotificationsOperator.show("Password not set", "Password not set for ${conf.baseUri}", NotificationType.WARNING)
                    }
                }
                Pair(AuthMethod.USERNAME_PASSWORD, false) -> {
                    NotificationsOperator.show("Username not set", "Username not set for ${conf.baseUri}", NotificationType.WARNING)
                }
            }
        }
        passwordSetButton.addActionListener {
            val conf = hostConfigSelect.selectedItem as ConfigHostHolder
            when (Pair(conf.authMethod, conf.username != null)) {
                Pair(AuthMethod.TOKEN, false),
                Pair(AuthMethod.TOKEN, true) -> {
                    PasswordsOperator.promptForPassword("token", conf.baseUri)
                }
                Pair(AuthMethod.USERNAME_PASSWORD, true) -> {
                    PasswordsOperator.promptForPassword(conf.username!!, conf.baseUri)
                }
                Pair(AuthMethod.USERNAME_PASSWORD, false) -> {
                    NotificationsOperator.show("Username not set", "Username not set for ${conf.baseUri}", NotificationType.WARNING)
                }
            }
        }
    }

    override fun refresh() {
        configButton.isEnabled = false
        passwordTestButton.isEnabled = false
        passwordSetButton.isEnabled = false

        hostConfigSelect.removeAllItems()
        val settings: MegaManipulatorSettings? = SettingsFileOperator.readSettings()
        label.text = SettingsFileOperator.validationText
        configButton.isEnabled = true
        if (settings != null) {
            settings.searchHostSettings.forEach {
                hostConfigSelect.addItem(
                    ConfigHostHolder(
                        hostType = HostType.SEARCH,
                        authMethod = it.value.settings.authMethod,
                        baseUri = it.value.settings.baseUrl,
                        username = it.value.settings.username,
                        hostNaming = it.key
                    )
                )
            }
            settings.searchHostSettings.values.forEach {
                it.codeHostSettings.forEach {
                    hostConfigSelect.addItem(
                        ConfigHostHolder(
                            hostType = HostType.CODE,
                            authMethod = it.value.settings.authMethod,
                            baseUri = it.value.settings.baseUrl,
                            username = it.value.settings.username,
                            hostNaming = it.key
                        )
                    )
                }
            }
        }
    }

    override val index: Int = 0
}
