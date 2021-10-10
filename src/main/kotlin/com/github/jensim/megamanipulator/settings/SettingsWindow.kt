package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState
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
    private val configButton = JButton("Validate config").apply {
        OnboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BTN_VALIDATE_CONF, this)
    }
    private val hostConfigSelect = JBList<ConfigHostHolder>()

    override val content: JComponent = panel(constraints = arrayOf(LCFlags.noGrid)) {
        row {
            component(
                panel(title = "Config", constraints = arrayOf(LCFlags.flowY)) {
                    row {
                        component(configButton)
                    }
                    row {
                        component(
                            JButton("Toggle clones index").apply {
                                OnboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_TOGGLE_CLONES, this)
                                addActionListener {
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
                        )
                    }
                    row {
                        component(
                            JButton("Docs", AllIcons.Toolwindows.Documentation).apply {
                                OnboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_DOCS, this)
                                addActionListener {
                                    com.intellij.ide.BrowserUtil.browse("https://mega-manipulator.github.io")
                                }
                            }
                        )
                    }
                    row {
                        component(
                            JButton("Reset onboarding", AllIcons.Toolwindows.Problems).apply {
                                OnboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_RESET_ONBOARDING, this)
                                addActionListener {
                                    MegaManipulatorSettingsState.resetOnBoarding()
                                    refresh()
                                }
                            }
                        )
                    }
                }
            )
            component(
                panel(title = "Tokens", constraints = arrayOf(LCFlags.flowY)) {
                    row {
                        component(
                            JButton("Validate tokens").apply {
                                OnboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_VALIDATE_TOKENS, this)
                                addActionListener {
                                    uiProtector.uiProtectedOperation("Validating tokens") {
                                        val tokens: Map<String, Deferred<String>> = searchOperator.validateTokens() + prRouter.validateAccess()
                                        validationOutputLabel.text = tokens
                                            .map { "<tr><td>${it.key}</td><td>${it.value.await()}</td></tr>" }
                                            .joinToString(
                                                separator = "\n",
                                                prefix = """
                                            <html>
                                            <head>
                                            <style>
                                            table, th, td {
                                              border: 1px solid;
                                              border-collapse: collapse;
                                            }
                                            </style>
                                            </head>
                                            <body>
                                            <table>
                                            <tr><th>Config</th><th>Status</th></tr>
                                                """.trimIndent(),
                                                postfix = "</table></body></html>"
                                            )
                                    }
                                }
                            }
                        )
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
        OnboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB, content)
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
        OnboardingOperator.display(OnboardingId.SETTINGS_TAB)
    }

    override val index: Int = 0
}
