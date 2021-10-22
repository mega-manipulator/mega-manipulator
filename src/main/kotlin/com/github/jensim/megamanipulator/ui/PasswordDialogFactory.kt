package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

object PasswordDialogFactory {

    fun askForPassword(
        focusComponent: JComponent,
        username: String,
        baseUrl: String,
        onYes: (String) -> Unit
    ) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val passwordField = JBPasswordField().apply { columns = 30 }
            val btnYes = JButton("Ok")
            val btnNo = JButton("Cancel")
            val content = panel {
                row {
                    label("Please provide credentials for $baseUrl")
                }
                row {
                    when (username) {
                        "token" -> label("TOKEN login method")
                        else -> label("Username: $username")
                    }
                }
                row {
                    label("Password:")
                    component(passwordField)
                }
                row {
                    component(btnYes)
                    component(btnNo)
                }
            }
            val popup = popupFactory.createDialogBalloonBuilder(content, "Password")
                .createBalloon()
            btnYes.addActionListener {
                popup.hide()
                onYes(passwordField.password.concatToString())
            }
            btnNo.addActionListener {
                popup.hide()
            }
            val location: RelativePoint = popupFactory.guessBestPopupLocation(focusComponent)

            popup.show(location, Balloon.Position.above)
            passwordField.requestFocus(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
