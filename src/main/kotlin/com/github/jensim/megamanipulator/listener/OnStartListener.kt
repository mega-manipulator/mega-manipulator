package com.github.jensim.megamanipulator.listener

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.ApplicationLevelPropertiesOperator
import com.github.jensim.megamanipulator.project.ApplicationLevelPropertiesOperator.ApplicationLevelFlag.GLOBAL_ONBOARDING
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
import com.github.jensim.megamanipulator.toolswindow.MyToolWindowFactory
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager

class OnStartListener : StartupActivity {

    override fun runActivity(project: Project) {
        if (isMM(project)) {
            ToolWindowManager.getInstance(project)?.let {
                val toolWindow = it.registerToolWindow("Mega Manipulator") {
                    this.icon = AllIcons.General.Modified
                    this.anchor = ToolWindowAnchor.BOTTOM
                }
                val factory = MyToolWindowFactory()
                factory.createToolWindowContent(project, toolWindow)
                toolWindow.show()
                it.getToolWindow("Project")?.show()
            }
        } else {
            if (!ApplicationLevelPropertiesOperator.get(GLOBAL_ONBOARDING)) {
                val onboardingOperator: OnboardingOperator = project.service()
                onboardingOperator.display(OnboardingId.MM_PROJECT_INSTRUCTION) {
                    ApplicationLevelPropertiesOperator.set(GLOBAL_ONBOARDING)
                }
            }
        }
    }
}
