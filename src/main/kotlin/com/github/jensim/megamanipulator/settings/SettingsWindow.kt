package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.project.ProjectOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import java.awt.Color
import java.awt.Component
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import kotlinx.coroutines.Deferred

@SuppressWarnings("LongParameterList")
class SettingsWindow(project: Project) : ToolWindowTab {

    private val passwordsOperator: PasswordsOperator by lazy { project.service() }
    private val projectOperator: ProjectOperator by lazy { project.service() }
    private val filesOperator: FilesOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val prRouter: PrRouter by lazy { project.service() }
    private val searchOperator: SearchOperator by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }

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

    private val docsButton = JButton("Docs", AllIcons.Toolwindows.Documentation)
    private val resetOnboardingButton = JButton("Reset onboarding", AllIcons.Toolwindows.Problems)
    private val validateTokensButton = JButton("Validate tokens")
    private val toggleClonesButton = JButton("Toggle clones index")
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
                        component(toggleClonesButton)
                    }
                    row {
                        component(docsButton)
                    }
                    row {
                        component(resetOnboardingButton)
                    }
                }
            )
            component(
                panel(title = "Tokens", constraints = arrayOf(LCFlags.flowY)) {
                    row {
                        component(validateTokensButton)
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
        docsButton.addActionListener {
            com.intellij.ide.BrowserUtil.browse("https://mega-manipulator.github.io")
        }
        validateTokensButton.addActionListener {
            uiProtector.uiProtectedOperation("Validating tokens") {
                val tokens: Map<String, Deferred<String>> =
                    searchOperator.validateTokens() + prRouter.validateAccess()
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
        resetOnboardingButton.addActionListener {
            MegaManipulatorSettingsState.resetOnBoarding()
            refresh()
        }
        toggleClonesButton.addActionListener {
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

    private fun testPassword(conf: ConfigHostHolder): Boolean =
        passwordsOperator.isPasswordSet(conf.username, conf.baseUri)

    private fun setPassword(conf: ConfigHostHolder) =
        passwordsOperator.promptForPassword(username = conf.username, baseUrl = conf.baseUri)

    override fun refresh() {
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_TOGGLE_CLONES, toggleClonesButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_VALIDATE_TOKENS, validateTokensButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_RESET_ONBOARDING, resetOnboardingButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BTN_VALIDATE_CONF, configButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_DOCS, docsButton)

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
        onboardingOperator.display(OnboardingId.WELCOME)
    }
}
