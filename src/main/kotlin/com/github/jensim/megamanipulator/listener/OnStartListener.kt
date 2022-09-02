package com.github.jensim.megamanipulator.listener

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.MegaManipulatorTabContentCreator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.slf4j.LoggerFactory

class OnStartListener : StartupActivity {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun runActivity(project: Project) {
        if (isMM(project)) {
            val factory: MegaManipulatorTabContentCreator = project.service()
            factory.createContentMegaManipulator()
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
}
