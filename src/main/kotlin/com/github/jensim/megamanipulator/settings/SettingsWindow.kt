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
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.panel
import kotlinx.coroutines.Deferred
import java.awt.Color
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

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
    private val megaManipulatorSettingsState: MegaManipulatorSettingsState by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

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
    private val resetPrefillButton = JButton("Reset history", AllIcons.Toolwindows.Problems)
    private val validateTokensButton = JButton("Validate tokens")
    private val toggleClonesButton = JButton("Toggle clones index")
    private val validationOutputLabel = JBLabel()
    private val openConfigButton = JButton("Open config")
    private val validateConfigButton = JButton("Validate config")
    private val hostConfigSelect = JBList<ConfigHostHolder>()
    private val confButtonsPanel = panel {
        group("Config") {
            row {
                scrollCell(
                    com.intellij.ui.dsl.builder.panel {
                        row {
                            cell(openConfigButton)
                        }
                        row {
                            cell(validateConfigButton)
                        }
                        row {
                            cell(toggleClonesButton)
                        }
                        row {
                            cell(docsButton)
                        }
                        row {
                            cell(resetOnboardingButton)
                        }
                        row {
                            cell(resetPrefillButton)
                        }
                    }
                )
            }
        }
    }
    private val tokensPanel = panel {
        group("Tokens") {
            row {
                cell(validateTokensButton)
                scrollCell(hostConfigSelect)
            }
        }
    }
    private val validationOutputPanel = panel {
        group("Validation output") {
            row {
                scrollCell(validationOutputLabel)
            }
        }
    }
    private val split2 = JBSplitter(0.5f).apply {
        firstComponent = tokensPanel
        secondComponent = validationOutputPanel
    }
    private val split = JBSplitter(0.33f).apply {
        firstComponent = confButtonsPanel
        secondComponent = split2
    }

    override val content: JComponent = panel {
        row {
            cell(split)
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
        validateConfigButton.toolTipText = """
            <html>
            <h1>Validate config</h1>
            <p>
            Run validation of the config file, and visualize any errors.<br>
            The config file has a schema-file to assist you on your way<br>
            to fix most problems interactively.
            </p>
            <html>
        """.trimIndent()
        validateConfigButton.addActionListener {
            refresh()
        }
        openConfigButton.addActionListener {
            try {
                VirtualFileManager.getInstance().let {
                    it.findFileByNioPath(File("${project.basePath}/config/mega-manipulator.json").toPath())
                        ?.let { file: VirtualFile ->
                            FileEditorManager.getInstance(project).openFile(file, true)
                        }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        docsButton.toolTipText = """
            <html>
            <h1>Open docs in browser</h1>
            </html>
        """.trimIndent()
        docsButton.addActionListener {
            com.intellij.ide.BrowserUtil.browse("https://mega-manipulator.github.io")
        }
        validateTokensButton.addActionListener {
            uiProtector.uiProtectedOperation("Validating tokens") {
                val deferredTokens: Map<String, Deferred<String>> = searchOperator.validateTokens() + prRouter.validateAccess()
                val tokens: MutableMap<String, String> = deferredTokens.mapValues {
                    try {
                        it.value.await()
                    } catch (e: Exception) {
                        e.stackTraceToString()
                    }
                }.toMutableMap()
                if (tokens.isEmpty()) {
                    tokens["-"] = "-"
                }
                validationOutputLabel.text = tokens.map { (k, v) -> "<tr><td>$k</td><td>$v</td></tr>" }
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
        resetOnboardingButton.toolTipText = """
            <html>
            <h1>Reset onboarding</h1>
            <p>
            Complete reset of the onboarding flow,<br>
            to allow you to revisit all seen info.
            </p> 
            </html>
        """.trimIndent()
        resetOnboardingButton.addActionListener {
            dialogGenerator.showConfirm(
                title = "Reset onboarding",
                message = """
                    Complete reset of the onboarding flow,
                    to allow you to revisit all seen info.
                """.trimIndent(),
                focusComponent = resetOnboardingButton
            ) {
                megaManipulatorSettingsState.resetOnBoarding()
                refresh()
            }
        }
        toggleClonesButton.toolTipText = """
            <html>
            <h1>Toggle clones index</h1>
            <p>
            Set project settings to toggle index for the local clones, or to ignore.
            </p>
            </html>
        """.trimIndent()
        toggleClonesButton.addActionListener {
            dialogGenerator.showConfirm(
                title = "Toggle clones index",
                message = "Set project settings to toggle index for the local clones, or to ignore.",
                focusComponent = toggleClonesButton
            ) {
                toggleClonesButton.isEnabled = false
                try {
                    projectOperator.toggleExcludeClones()
                    filesOperator.refreshClones()
                } finally {
                    toggleClonesButton.isEnabled = true
                }
            }
        }
        resetPrefillButton.toolTipText = """
            <html>
            <h1>Reset history</h1>
            <p>
            Reset the previously used values, <br>
            that are made available in the GUI for reuse
            </p>
            </html>
        """.trimIndent()
        resetPrefillButton.addActionListener {
            dialogGenerator.showConfirm(
                title = "Reset history",
                message = """
                Reset the previously used values,
                that are made available in the GUI for reuse
                """.trimIndent(),
                focusComponent = resetPrefillButton
            ) {
                megaManipulatorSettingsState.resetPrefill()
            }
        }
    }

    private fun testPassword(conf: ConfigHostHolder): Boolean =
        passwordsOperator.isPasswordSet(conf.username, conf.baseUri)

    private fun setPassword(conf: ConfigHostHolder) =
        passwordsOperator.promptForPassword(focusComponent = hostConfigSelect, username = conf.username, baseUrl = conf.baseUri)

    override fun refresh() {
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_TOGGLE_CLONES, toggleClonesButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_VALIDATE_TOKENS, validateTokensButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_RESET_ONBOARDING, resetOnboardingButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BTN_VALIDATE_CONF, validateConfigButton)
        onboardingOperator.registerTarget(OnboardingId.SETTINGS_TAB_BUTTON_DOCS, docsButton)

        validateConfigButton.isEnabled = false
        filesOperator.makeUpBaseFiles()
        filesOperator.refreshConf()
        hostConfigSelect.setListData(emptyArray())
        val settings: MegaManipulatorSettings? = settingsFileOperator.readSettings()
        validationOutputLabel.text = settingsFileOperator.validationText
        validateConfigButton.isEnabled = true
        if (settings != null) {
            val arrayOf: Array<ConfigHostHolder> = (
                settings.searchHostSettings.map { (name, group) ->
                    ConfigHostHolder(
                        hostType = HostType.SEARCH,
                        authMethod = group.value().authMethod,
                        baseUri = group.value().baseUrl,
                        username = group.value().username,
                        hostNaming = name
                    )
                } + settings.searchHostSettings.values.flatMap {
                    it.value().codeHostSettings.map { (name, group) ->

                        ConfigHostHolder(
                            hostType = HostType.CODE,
                            authMethod = group.value().authMethod,
                            baseUri = group.value().baseUrl,
                            username = group.value().username ?: "token",
                            hostNaming = name
                        )
                    }
                }
                ).toTypedArray()
            hostConfigSelect.setListData(arrayOf)
        }
        onboardingOperator.display(OnboardingId.WELCOME)
    }
}
