package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

class PushDialogFactory {

    private val forceBox = JBCheckBox("Force push?")
    private val okBtn = JButton("Push")
    private val cancelBtn = JButton("Cancel")
    private val panel = panel {
        row {
            label("Push commits to upstream")
        }
        row {
            component(forceBox)
        }
        row {
            component(okBtn)
            component(cancelBtn)
        }
    }

    fun openPushDialog(
        focusComponent: JComponent,
        onOk: (force: Boolean) -> Unit,
    ) {
        val popupFactory: JBPopupFactory = try {
            JBPopupFactory.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
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
    }
}
