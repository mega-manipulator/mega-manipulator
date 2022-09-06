package com.github.jensim.megamanipulator.listener

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
import com.github.jensim.megamanipulator.toolswindow.MegaManipulatorTabContentCreator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class OnStartListener : StartupActivity {

    override fun runActivity(project: Project) {
        if (isMM(project)) {
            val factory: MegaManipulatorTabContentCreator = project.service()
            factory.createContentMegaManipulator()
        } else {
            val factory: MegaManipulatorTabContentCreator = project.service()
            factory.createHelloContent()

            val onboardingOperator: OnboardingOperator = project.service()
            if (!onboardingOperator.seenGlobalOnboarding()) {
                onboardingOperator.display(OnboardingId.MM_PROJECT_INSTRUCTION) {
                    onboardingOperator.setSeenGlobalOnboarding()
                }
            }
        }
    }
}
