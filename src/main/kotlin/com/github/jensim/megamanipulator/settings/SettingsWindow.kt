package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.layout.panel
import java.awt.Color
import java.awt.Component
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

class SettingsWindow(
    private val passwordsOperator: PasswordsOperator,
    private val projectOperator: ProjectOperator,
    private val filesOperator: FilesOperator,
    private val settingsFileOperator: SettingsFileOperator,
) : ToolWindowTab {

    companion object {

        val instance by lazy {
            SettingsWindow(
                passwordsOperator = PasswordsOperator.instance,
                projectOperator = ProjectOperator.instance,
                filesOperator = FilesOperator.instance,
                settingsFileOperator = SettingsFileOperator.instance
            )
        }
    }

    private enum class HostType {
        SEARCH,
        CODE,
    }

    private data class ConfigHostHolder(
        val hostType: HostType,
        val authMethod: AuthMethod,
        val baseUri: String,
        val username: String,
        val hostNaming: String,

    ) {

        override fun toString(): String = "$hostType: $hostNaming"
    }

    private val label = JBLabel()
    private val configButton = JButton("Validate config")
    private val hostConfigSelect = JBList<ConfigHostHolder>()

    override val content: JComponent = panel {
        row {
            component(configButton)
            label("Passwords")
        }
        row {
            component(label)
            component(hostConfigSelect)
        }
        row {
            button("Toggle clones index") {
                val b: Component = it.source as Component
                b.isEnabled = false
                try {
                    projectOperator.toggleExcludeClones()
                    filesOperator.refreshClones()
                } finally {
                    b.isEnabled = true
                }
            }
        }
    }

    init {
        hostConfigSelect.addCellRenderer({ if (testPassword(it)) null else Color.ORANGE }, { it.toString() })
        hostConfigSelect.selectionMode = ListSelectionModel.SINGLE_SELECTION
        hostConfigSelect.addListSelectionListener {
            hostConfigSelect.selectedValue?.let { conf: ConfigHostHolder ->
                setPassword(conf)
                refresh()
            }
        }
        configButton.addActionListener {
            refresh()
        }
    }

    private fun testPassword(conf: ConfigHostHolder): Boolean = passwordsOperator.isPasswordSet(conf.username, conf.baseUri)
    private fun setPassword(conf: ConfigHostHolder) = passwordsOperator.promptForPassword(username = conf.username, baseUrl = conf.baseUri)

    override fun refresh() {
        configButton.isEnabled = false
        filesOperator.makeUpBaseFiles()
        filesOperator.refreshConf()
        hostConfigSelect.setListData(emptyArray())
        val settings: MegaManipulatorSettings? = settingsFileOperator.readSettings()
        label.text = settingsFileOperator.validationText
        configButton.isEnabled = true
        if (settings != null) {
            val arrayOf: Array<ConfigHostHolder> =
                (
                    settings.searchHostSettings.map {
                        ConfigHostHolder(
                            hostType = HostType.SEARCH,
                            authMethod = it.value.authMethod,
                            baseUri = it.value.baseUrl,
                            username = it.value.username,
                            hostNaming = it.key
                        )
                    } + settings.searchHostSettings.values.flatMap {
                        it.codeHostSettings.map {

                            ConfigHostHolder(
                                hostType = HostType.CODE,
                                authMethod = it.value.authMethod,
                                baseUri = it.value.baseUrl,
                                username = it.value.username ?: "token",
                                hostNaming = it.key
                            )
                        }
                    }
                    ).toTypedArray()
            hostConfigSelect.setListData(arrayOf)
        }
    }

    override val index: Int = 0
}
