package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.settings.types.AuthMethod
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.dsl.builder.panel
import org.slf4j.LoggerFactory
import javax.swing.JButton
import javax.swing.JComponent

object PasswordDialogFactory {

    private val logger = LoggerFactory.getLogger(PasswordDialogFactory::class.java)

    fun askForPassword(
        focusComponent: JComponent,
        authMethod: AuthMethod,
        username: String,
        baseUrl: String,
        onYes: (String) -> Unit
    ) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val passwordField = JBPasswordField().apply {
                columns = 30
            }

            val btnYes = JButton("Ok")
            val btnNo = JButton("Cancel")
            val content = panel {
                row {
                    label("Please provide credentials for $baseUrl")
                }
                row {
                    when (authMethod) {
                        AuthMethod.JUST_TOKEN -> label("TOKEN login method")
                        else -> label("Username: $username")
                    }
                }
                row {
                    label("Password/token:")
                    cell(passwordField)
                }
                row {
                    cell(btnYes)
                    cell(btnNo)
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
            popup.setDefaultButton(content, btnYes)
            passwordField.requestFocus(true)
        } catch (e: Exception) {
            logger.error("Caught an exception asking for passwords", e)
        }
    }
}
