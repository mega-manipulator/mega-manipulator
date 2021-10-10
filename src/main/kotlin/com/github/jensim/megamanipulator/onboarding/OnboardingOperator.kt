package com.github.jensim.megamanipulator.onboarding

import com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState.Companion.seenOnBoarding
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.layout.panel
import java.util.EnumMap
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.JTextArea

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
        if (seenOnBoarding(id)) {
            return
        }

        val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
        val popupLocation: RelativePoint? = reg[id]?.let { popupFactory.guessBestPopupLocation(it) }

        val closeButton = JButton("Ok")
        val panel = panel {
            row {
                component(JTextArea(id.text).apply { })
            }
            row {
                component(closeButton)
            }
        }
        val balloon = popupFactory.createDialogBalloonBuilder(panel, id.title)
            .createBalloon()

        closeButton.addActionListener {
            balloon.hide(true)
            id.next?.let {
                display(it)
            }
        }
        balloon.setAnimationEnabled(true)
        if (popupLocation != null) {
            balloon.show(popupLocation, Balloon.Position.above)
        } else {
            getWindowComponent2()?.let {
                balloon.showInCenterOf(it)
            }
        }
    }

    private fun getWindowComponent2(): JLayeredPane? {
        return WindowManager.getInstance().getFrame(project)?.layeredPane
    }
}
