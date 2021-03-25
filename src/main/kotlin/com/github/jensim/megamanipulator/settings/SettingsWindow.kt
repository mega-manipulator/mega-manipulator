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

object SettingsWindow : ToolWindowTab {

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
        fun test(): Boolean = PasswordsOperator.isPasswordSet(username, baseUri)
        fun set() {
            PasswordsOperator.promptForPassword(username = username, baseUrl = baseUri)
        }
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
                    ProjectOperator.toggleExcludeClones()
                    FilesOperator.refreshClones()
                } finally {
                    b.isEnabled = true
                }
            }
        }
    }

    init {
        hostConfigSelect.addCellRenderer({ if (it.test()) null else Color.ORANGE }, { it.toString() })
        hostConfigSelect.selectionMode = ListSelectionModel.SINGLE_SELECTION
        hostConfigSelect.addListSelectionListener {
            hostConfigSelect.selectedValue?.let { conf ->
                conf.set()
                refresh()
            }
        }
        configButton.addActionListener {
            refresh()
        }
    }

    override fun refresh() {
        configButton.isEnabled = false

        hostConfigSelect.setListData(emptyArray())
        val settings: MegaManipulatorSettings? = SettingsFileOperator.readSettings()
        label.text = SettingsFileOperator.validationText
        configButton.isEnabled = true
        if (settings != null) {
            val arrayOf: Array<ConfigHostHolder> =
                (
                    settings.searchHostSettings.map {
                        ConfigHostHolder(
                            hostType = HostType.SEARCH,
                            authMethod = it.value.settings.authMethod,
                            baseUri = it.value.settings.baseUrl,
                            username = it.value.settings.username ?: "token",
                            hostNaming = it.key
                        )
                    } + settings.searchHostSettings.values.flatMap {
                        it.codeHostSettings.map {

                            ConfigHostHolder(
                                hostType = HostType.CODE,
                                authMethod = it.value.settings.authMethod,
                                baseUri = it.value.settings.baseUrl,
                                username = it.value.settings.username ?: "token",
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
