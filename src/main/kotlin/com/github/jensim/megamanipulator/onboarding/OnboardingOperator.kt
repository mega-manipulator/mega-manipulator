package com.github.jensim.megamanipulator.onboarding

import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.TabSelectorService
import com.github.jensim.megamanipulator.ui.setDefaultButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.panel
import org.slf4j.LoggerFactory
import java.util.EnumMap
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLayeredPane

class OnboardingOperator(private val project: Project) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val tabSelectorService: TabSelectorService by lazy { project.service() }
    private val state: MegaManipulatorSettingsState by lazy { project.service() }

    private val reg: MutableMap<OnboardingId, JComponent> = EnumMap(OnboardingId::class.java)

    fun registerTarget(id: OnboardingId, contentTarget: JComponent) {
        reg[id] = contentTarget
    }

    fun resetTab(tabKey: TabKey) {
        OnboardingId.values()
            .filter { it.tab == tabKey }
            .forEach { state.resetOnBoarding(it) }
    }

    @SuppressWarnings("LongMethod")
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
            logger.error("Could not get the popup factory", e)
            return
        }

        val closeButton = JButton("Ok")
        val stopButton = JButton("Stop")

        val text = if (id.autoMultiLineConvertion) id.text.convertMultiLineToHtml() else id.text
        val panel = panel {
            row {
                cell(JBLabel(text))
            }
            row {
                cell(closeButton)
                cell(stopButton)
            }
        }
        val balloon = popupFactory.createDialogBalloonBuilder(panel, id.title)
            .createBalloon()

        closeButton.addActionListener {
            balloon.hide(true)
            extraOkAction()
            state.setOnBoardingSeen(id)
            id.next?.let { next ->
                display(next)
            }
        }
        stopButton.addActionListener {
            balloon.hide()
            generateSequence(id) { it.next }.forEach {
                state.setOnBoardingSeen(it)
            }
        }
        balloon.setAnimationEnabled(true)
        id.tab?.let { tab ->
            tabSelectorService.selectTab(tab)
        }
        val popupLocation: RelativePoint? = reg[id]?.let { popupFactory.guessBestPopupLocation(it) }
        if (popupLocation != null) {
            balloon.show(popupLocation, Balloon.Position.above)
        } else {
            getWindowComponent2()?.let {
                balloon.showInCenterOf(it)
            }
        }
        balloon.setDefaultButton(panel, closeButton)
        closeButton.requestFocus()
    }

    private fun String.convertMultiLineToHtml() = "<html>${replace("\n", "<br>\n")}</html>"

    private fun getWindowComponent2(): JLayeredPane? = try {
        WindowManager.getInstance()?.getFrame(project)?.layeredPane
    } catch (e: Exception) {
        logger.warn("Was unable to get the active project frame from the windowManager. The onboarding popup will not appear.")
        null
    }
}
