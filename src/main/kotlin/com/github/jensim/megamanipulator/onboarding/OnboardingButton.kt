package com.github.jensim.megamanipulator.onboarding

import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class OnboardingButton(
    project: Project,
    private val tabKey: TabKey,
    private val onboardingId: OnboardingId,
) : JBLabel(AllIcons.General.QuestionDialog), MouseListener {

    private val onboardingOperator by lazyService<OnboardingOperator>(project, null)

    init {
        addMouseListener(this)
        toolTipText = "Onboarding"
    }

    override fun mouseClicked(e: MouseEvent?) {
        onboardingOperator.resetTab(tabKey)
        onboardingOperator.display(onboardingId)
    }

    override fun mousePressed(e: MouseEvent?) = Unit
    override fun mouseReleased(e: MouseEvent?) = Unit
    override fun mouseEntered(e: MouseEvent?) = Unit
    override fun mouseExited(e: MouseEvent?) = Unit
}
