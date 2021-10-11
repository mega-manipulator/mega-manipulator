package com.github.jensim.megamanipulator.listener

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowManager

class OnStartListener : StartupActivity {

    override fun runActivity(project: Project) {
        OnboardingOperator.registerProject(project)
        if (isMM(project)) {
            ToolWindowManager.getInstance(project)?.let {
                it.getToolWindow("Project")?.show()
            }
            ToolWindowManager.getInstance(project)?.let {
                it.getToolWindow("Mega Manipulator")?.show()
            }
            OnboardingOperator.display(OnboardingId.WELCOME)
        } else {
            OnboardingOperator.display(OnboardingId.MM_PROJECT_INSTRUCTION)
        }
    }
}
