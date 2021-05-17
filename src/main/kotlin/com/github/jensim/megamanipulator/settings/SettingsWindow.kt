package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import kotlinx.coroutines.Deferred
import java.awt.Color
import java.awt.Component
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

@SuppressWarnings("LongParameterList")
class SettingsWindow(
        private val passwordsOperator: PasswordsOperator,
        private val projectOperator: ProjectOperator,
        private val filesOperator: FilesOperator,
        private val settingsFileOperator: SettingsFileOperator,
        private val uiProtector: UiProtector,
        private val prRouter: PrRouter,
        private val searchOperator: SearchOperator,
) : ToolWindowTab {

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

    private val validationOutputLabel = JBLabel()
    private val configButton = JButton("Validate config")
    private val hostConfigSelect = JBList<ConfigHostHolder>()

    override val content: JComponent = panel(constraints = arrayOf(LCFlags.noGrid)) {
        row {
            component(
                panel(title = "Config", constraints = arrayOf(LCFlags.flowY)) {
                    row {
                        component(configButton)
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
                    row {
                        component(
                            JButton("Docs", AllIcons.Toolwindows.Documentation).apply {
                                addActionListener {
                                    com.intellij.ide.BrowserUtil.browse("https://jensim.github.io/mega-manipulator/")
                                }
                            }
                        )
                    }
                }
            )
            component(
                panel(title = "Tokens", constraints = arrayOf(LCFlags.flowY)) {
                    row {
                        button("Validate tokens") {
                            uiProtector.uiProtectedOperation("Validating tokens") {
                                val tokens: Map<String, Deferred<String>> = searchOperator.validateTokens() + prRouter.validateAccess()
                                validationOutputLabel.text = tokens
                                    .map { "<tr><td>${it.key}</td><td>${it.value.await()}</td></tr>" }
                                    .joinToString(separator = "\n", prefix = "<html><body><table><tr><th>Config</th><th>Status</th>", postfix = "</table></body></html>")
                            }
                        }
                        component(hostConfigSelect)
                    }
                }
            )
            component(
                panel(title = "Validation output", constraints = arrayOf(LCFlags.flowY)) {
                    row {
                        component(validationOutputLabel)
                    }
                }
            )
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
        validationOutputLabel.text = settingsFileOperator.validationText
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
