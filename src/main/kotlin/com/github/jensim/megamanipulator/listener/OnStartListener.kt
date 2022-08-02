package com.github.jensim.megamanipulator.listener

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.MegaManipulatorTabContentCreator
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File

class OnStartListener : StartupActivity {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun runActivity(project: Project) {
        if (isMM(project)) {
            val factory: MegaManipulatorTabContentCreator = project.service()
            factory.createContentMegaManipulator()
            val osProperty = System.getProperty("os.name").lowercase()
            if (osProperty.startsWith("darwin") || osProperty.startsWith("mac os x")) {
                val output: List<String> = checkCommands(listOf("brew", "git", "find"), project.service())
                if (output.isNotEmpty()) {
                    val dialoger = project.service<DialogGenerator>()
                    dialoger.showConfirm(
                        title = "Borked PATH, probably.",
                        yesText = "Ok",
                        noText = "Cancel",
                        focusComponent = null,
                        onYes = {},
                        message = """
                        Cannot detect the following commands on the system PATH: $output<br>
                        You must open IntelliJ from a command line, or the Java process will have a totally borked PATH.<br>
                        This will affect you when you do scripted changes using the Apply tab.<br>
                        Try closing all IntelliJ windows, and restart it one of these ways (from the <u>terminal</u>):<br>
                        <ul>
                        <li style="border: dashed;">
                        <pre>$ open -a IntelliJ\ IDEA\ Ultimate</pre>
                        </li>
                        <li style="border: dashed;">
                        <pre>$ idea</pre>
                        If you have the idea toolbox and shellscript installed
                        </li>
                        </ul>
                        """.trimIndent()
                    )
                } else {
                    logger.info("All seems okay with the path")
                }
            }
        } else {
            val factory: MegaManipulatorTabContentCreator = project.service()
            factory.createHelloContent()
            val settings: MegaManipulatorSettingsState = project.service()
            if (!settings.seenGlobalOnboarding) {
                val onboardingOperator: OnboardingOperator = project.service()
                onboardingOperator.display(OnboardingId.MM_PROJECT_INSTRUCTION) {
                    settings.seenGlobalOnboarding = true
                }
            }
        }
    }

    private fun checkCommands(commands: List<String>, processOperator: ProcessOperator): List<String> {
        return runBlocking {
            commands.mapNotNull {
                val result = processOperator.runCommandAsync(File("/"), listOf("command", "-v", it)).await()
                if (result.exitCode != 0) it else null
            }
        }
    }
}
