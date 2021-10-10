package com.github.jensim.megamanipulator.onboarding

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import java.util.EnumMap
import javax.swing.JComponent

object OnboardingOperator {

    private val reg: MutableMap<OnboardingId, JComponent> = EnumMap(OnboardingId::class.java)
    private lateinit var project: Project

    fun registerProject(project: Project) {
        this.project = project
    }

    fun registerTarget(id: OnboardingId, contentTarget: JComponent) {
        reg[id] = contentTarget
    }

    fun display(id: OnboardingId) {
        val targetComponent = reg[id]
        val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
        val popupLocation: RelativePoint = targetComponent
            ?.let { popupFactory.guessBestPopupLocation(targetComponent) }
            ?: try {
                getWindowComponent()?.let { popupFactory.guessBestPopupLocation(it) }
            } catch (e: Exception) {
                null
            }
            ?: return

        val popup = popupFactory.createMessage(id.text)
        popup.setCaption(id.title)
        popup.showInFocusCenter()
        popup.show(popupLocation)
    }

    private fun getWindowComponent(): JComponent? {
        return WindowManager.getInstance().getIdeFrame(project)?.component
    }
}
