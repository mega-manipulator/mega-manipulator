package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.dsl.builder.panel
import org.slf4j.LoggerFactory
import javax.swing.JButton
import javax.swing.JComponent

class PushDialogFactory(project: Project) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val forceBox = JBCheckBox("Force push?")
    private val okBtn = JButton("Push")
    private val cancelBtn = JButton("Cancel")
    private val panel = panel {
        row {
            label("Push commits to upstream")
        }
        row {
            cell(forceBox)
        }
        row {
            cell(okBtn)
            cell(cancelBtn)
        }
    }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }

    fun openPushDialog(
        focusComponent: JComponent,
        onOk: (force: Boolean) -> Unit,
    ) {
        val popupFactory: JBPopupFactory = try {
            JBPopupFactory.getInstance()
        } catch (e: Exception) {
            val msg = "Failed getting JBPopupFactory instance"
            logger.error(msg, e)
            notificationsOperator.show(msg, "Something failed horribly<br>${e.javaClass.simpleName}<br>${e.message}", ERROR)
            return
        }
        val balloon = popupFactory.createDialogBalloonBuilder(panel, "Push").createBalloon()
        okBtn.addActionListener {
            balloon.hide()
            onOk(forceBox.isSelected)
        }
        cancelBtn.addActionListener {
            balloon.hide()
        }
        val location = popupFactory.guessBestPopupLocation(focusComponent)
        balloon.show(location, Balloon.Position.above)
        balloon.setDefaultButton(panel, okBtn)
    }
}
