package com.github.jensim.megamanipulator.onboarding

import com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.TabSelectorService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.serviceContainer.NonInjectable
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.panel
import java.util.EnumMap
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLayeredPane

class OnboardingOperator @NonInjectable constructor(
    private val project: Project,
    tabSelectorService: TabSelectorService?,
    state: MegaManipulatorSettingsState?,
) {

    constructor(project: Project) : this(project, null, null)

    private val tabSelectorService: TabSelectorService by lazyService(project, tabSelectorService)
    private val state: MegaManipulatorSettingsState by lazyService(project, state)

    private val reg: MutableMap<OnboardingId, JComponent> = EnumMap(OnboardingId::class.java)

    fun registerTarget(id: OnboardingId, contentTarget: JComponent) {
        reg[id] = contentTarget
    }

    fun resetTab(tabKey: TabKey) {
        OnboardingId.values()
            .filter { it.tab == tabKey }
            .forEach { state.resetOnBoarding(it) }
    }

    fun display(id: OnboardingId, extraOkAction: () -> Unit = {}) {
        if (state.seenOnBoarding(id)) {
            id.next?.let { next ->
                display(next)
            }
            return
        }

        val popupFactory: JBPopupFactory = try {
            JBPopupFactory.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        val closeButton = JButton("Ok")
        val stopButton = JButton("Stop")

        val text = if (id.autoMultiLineConvertion) id.text.convertMultiLineToHtml() else id.text
        val panel = panel {
            row {
                component(JBLabel(text))
            }
            row {
                component(closeButton)
                component(stopButton)
            }
        }
        val balloon = popupFactory.createDialogBalloonBuilder(panel, id.title)
            .createBalloon()

        val pane = getWindowComponent2()
        closeButton.addActionListener {
            balloon.hide(true)
            pane?.let {
                it.rootPane.defaultButton = null
            }
            extraOkAction()
            state.setOnBoardingSeen(id)
            id.next?.let { next ->
                display(next)
            }
        }
        stopButton.addActionListener {
            balloon.hide()
        }
        balloon.setAnimationEnabled(true)
        id.tab?.let { tab ->
            tabSelectorService.selectTab(tab)
        }
        val popupLocation: RelativePoint? = reg[id]?.let { popupFactory.guessBestPopupLocation(it) }
        if (popupLocation != null) {
            balloon.show(popupLocation, Balloon.Position.above)
        } else {
            pane?.let {
                balloon.showInCenterOf(it)
            }
        }
        pane?.rootPane?.defaultButton = closeButton
        closeButton.requestFocus()
    }

    private fun String.convertMultiLineToHtml() = "<html>${replace("\n", "<br>\n")}</html>"

    private fun getWindowComponent2(): JLayeredPane? = try {
        WindowManager.getInstance().getFrame(project)?.layeredPane
    } catch (e: Exception) {
        null
    }
}
