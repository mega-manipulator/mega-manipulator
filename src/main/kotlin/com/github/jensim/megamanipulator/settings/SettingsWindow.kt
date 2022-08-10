package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.ProjectOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.github.jensim.megamanipulator.settings.types.HostWithAuth
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.rd.util.firstOrNull
import kotlinx.coroutines.Deferred
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

@SuppressWarnings("LongParameterList")
class SettingsWindow(project: Project) : ToolWindowTab {

    private val logger = LoggerFactory.getLogger(javaClass)

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
        ERROR,
    }

    private data class ConfigHostHolder(
        val hostType: HostType,
        val authMethod: AuthMethod,
        val baseUrl: String,
        val username: String,
        val searchHost: String,
        val codeHost: String? = null,
        var validationResult: String?,
    ) {
        override fun toString(): String = if (hostType == HostType.SEARCH) {
            "$hostType: $searchHost"
        } else {
            "$hostType: $searchHost/${codeHost ?: "???"}"
        }
    }

    private val docsButton = JButton("Docs", AllIcons.Toolwindows.Documentation)
    private val resetOnboardingButton = JButton("Reset onboarding", AllIcons.Toolwindows.Problems)
    private val resetPrefillButton = JButton("Reset history", AllIcons.Toolwindows.Problems)
    private val toggleClonesButton = JButton("Toggle clones index")
    private val openConfigButton = JButton("Open config")
    private val validateConfigButton = JButton("Validate config")
    private val validateTokensButton = JButton("Validate tokens")
    private val hostConfigSelect = GeneralKtDataTable(
        ConfigHostHolder::class,
        columns = listOf(
            "Config" to { it.toString() },
            "Result" to { it.validationResult ?: "OK" }
        ),
        selectionMode = ListSelectionModel.SINGLE_SELECTION,
    ) {
        it.validationResult != null
    }
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
                            cell(validateTokensButton)
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

    override val content: JComponent = BorderLayoutPanel()
        // .addToTop(JBLabel("Click the "))
        .addToLeft(confButtonsPanel)
        .addToCenter(JBScrollPane(hostConfigSelect))

    init {
        hostConfigSelect.setListData(
            listOf(
                ConfigHostHolder(
                    hostType = HostType.ERROR,
                    authMethod = AuthMethod.NONE,
                    baseUrl = "?",
                    username = "?",
                    searchHost = "*",
                    codeHost = "*",
                    validationResult = "Not loaded yet, click the validate config button",
                )
            )
        )
        hostConfigSelect.addListSelectionListener {
            hostConfigSelect.selectedValuesList.firstOrNull()?.let { conf: ConfigHostHolder ->
                if (conf.hostType != HostType.ERROR) {
                    setPassword(conf)
                }
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
                logger.warn("Failed to open config file.", e)
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
                val deferredTokens: Map<Pair<String, String?>, Deferred<String?>> = searchOperator.validateTokens() + prRouter.validateTokens()
                val tokens: Map<Pair<String, String?>, String?> = deferredTokens.mapValues {
                    try {
                        it.value.await()
                    } catch (e: Exception) {
                        logger.warn("Failed validating ${it.key.first}/${it.key.second}", e)
                        "${e.javaClass.simpleName} ${e.message}"
                    }
                }
                hostConfigSelect.items.forEach { config ->
                    val match = tokens.filter { it.key.first == config.searchHost && it.key.second == config.codeHost }.firstOrNull()
                    if (match == null) {
                        config.validationResult = "No token validation results"
                    } else {
                        config.validationResult = match.value
                    }
                }
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
                focusComponent = resetPrefillButton,
            ) {
                megaManipulatorSettingsState.resetPrefill()
            }
        }
    }

    private fun setPassword(conf: ConfigHostHolder) {
        passwordsOperator.promptForPassword(focusComponent = hostConfigSelect, username = conf.username, baseUrl = conf.baseUrl)
        if (conf.validationResult == passwordNotSetString) {
            conf.validationResult = initialValidationText(conf)
        }
    }

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
        hostConfigSelect.setListData(emptyList())
        val settings: MegaManipulatorSettings? = settingsFileOperator.readSettings()
        validateConfigButton.isEnabled = true
        if (settings != null && settingsFileOperator.validationIsOkay) {
            val arrayOf: List<ConfigHostHolder> = (
                settings.searchHostSettings.map { (searchHostName, group) ->
                    ConfigHostHolder(
                        hostType = HostType.SEARCH,
                        authMethod = group.value().authMethod,
                        baseUrl = group.value().baseUrl,
                        username = group.value().username,
                        searchHost = searchHostName,
                        validationResult = initialValidationText(group.value()),
                    )
                } + settings.searchHostSettings.flatMap { (searchHostName: String, searchHostSettingsGroup: SearchHostSettingsGroup) ->
                    searchHostSettingsGroup.value().codeHostSettings.map { (codeHostName, codeHostSettingsGroup) ->
                        ConfigHostHolder(
                            hostType = HostType.CODE,
                            authMethod = codeHostSettingsGroup.value().authMethod,
                            baseUrl = codeHostSettingsGroup.value().baseUrl,
                            username = codeHostSettingsGroup.value().username ?: "token",
                            searchHost = searchHostName,
                            codeHost = codeHostName,
                            validationResult = initialValidationText(codeHostSettingsGroup.value()),
                        )
                    }
                }
                )
            hostConfigSelect.setListData(arrayOf)
        } else {
            hostConfigSelect.setListData(
                listOf(
                    ConfigHostHolder(
                        hostType = HostType.ERROR,
                        authMethod = AuthMethod.NONE,
                        baseUrl = "error",
                        username = "error",
                        searchHost = "*",
                        codeHost = "*",
                        validationResult = settingsFileOperator.validationText
                    )
                )
            )
        }
        onboardingOperator.display(OnboardingId.WELCOME)
    }

    private val passwordNotSetString = "Password is not set, CLICK HERE to set it"
    private fun initialValidationText(configHostHolder: ConfigHostHolder): String = initialValidationText(configHostHolder.username, configHostHolder.baseUrl)
    private fun initialValidationText(hostWithAuth: HostWithAuth?): String = initialValidationText(hostWithAuth?.username, hostWithAuth?.baseUrl)
    private fun initialValidationText(username: String?, baseUrl: String?): String = if (username == null || baseUrl == null) {
        "Unable to resolve settings"
    } else if (passwordsOperator.isPasswordSet(username, baseUrl)) {
        "Click the validate tokens button to validate"
    } else {
        passwordNotSetString
    }
}
