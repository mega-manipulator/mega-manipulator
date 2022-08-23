package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.project.CoroutinesHolder.scope
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.ide.DataManager
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.UIUtil.ComponentStyle.SMALL
import com.intellij.util.ui.UIUtil.FontColor.BRIGHTER
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.await
import org.slf4j.LoggerFactory
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.text.JTextComponent

class DialogGenerator(private val project: Project) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }

    fun showConfirm(
        title: String,
        message: String,
        focusComponent: JComponent?,
        position: Balloon.Position = Balloon.Position.above,
        convertMultiLine: Boolean = true,
        yesText: String = "Yes",
        noText: String = "No",
        onNo: () -> Unit = {},
        onYes: () -> Unit,
    ) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val yesBtn = JButton(yesText)
            val noBtn = JButton(noText)
            val message1 = if (convertMultiLine) message.convertMultiLineToHtml() else message
            val panel = panel {
                row {
                    cell(JBScrollPane(JBLabel(message1)))
                }
                row {
                    cell(yesBtn)
                    cell(noBtn)
                }
            }
            val popup: Balloon = popupFactory.createDialogBalloonBuilder(panel, title)
                .setHideOnClickOutside(true)
                .createBalloon()
            yesBtn.addActionListener {
                popup.hide()
                onYes()
            }
            noBtn.addActionListener {
                popup.hide()
                onNo()
            }
            if (focusComponent != null) {
                val location = popupFactory.guessBestPopupLocation(focusComponent)
                popup.show(location, position)
            } else {
                scope.launch {
                    val dataContext = DataManager.getInstance().dataContextFromFocusAsync.await()
                    val location = popupFactory.guessBestPopupLocation(dataContext)
                    popup.show(location, position)
                }
            }
            popup.setDefaultButton(panel, yesBtn)
            yesBtn.requestFocus(true)
        } catch (e: Exception) {
            val msg = "Failed creating dialog \"$title\""
            reportBadStuff(msg, e)
        }
    }

    private fun reportBadStuff(msg: String, e: Exception) {
        logger.error(msg, e)
        notificationsOperator.show(msg, "Something failed horribly<br>${e.javaClass.simpleName}<br>${e.message}", ERROR)
    }

    private fun String.convertMultiLineToHtml() = "<html>${replace("\n", "<br>\n")}</html>"

    fun askForInput(
        title: String,
        message: String,
        prefill: PrefillString?,
        field: JTextComponent = JBTextField(40),
        focusComponent: JComponent,
        position: Balloon.Position = Balloon.Position.above,
        validationPattern: String? = null,
        yesText: String = "Ok",
        noText: String = "Cancel",
        onNo: () -> Unit = {},
        onYes: (String) -> Unit
    ) {
        try {
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val btnYes = JButton(yesText)
            val btnNo = JButton(noText)
            val rex = validationPattern?.let { Regex(it) }

            val validationPanel = rex?.let { pattern ->
                panel {
                    row {
                        cell(JBLabel("Invalid input, must match pattern: $pattern", SMALL, BRIGHTER))
                    }
                }
            }
            val validation: (() -> Unit)? = rex?.let {
                {
                    val isValid = field.text.matches(rex)
                    btnYes.isEnabled = isValid
                    validationPanel?.isVisible = !isValid
                }
            }
            validationPanel?.apply {
                field.addKeyListener(object : KeyListener {
                    override fun keyTyped(e: KeyEvent?) = Unit
                    override fun keyPressed(e: KeyEvent?) = Unit
                    override fun keyReleased(e: KeyEvent?): Unit = validation?.invoke() ?: Unit
                })
                isVisible = true
                btnYes.isEnabled = false
            }

            val prefillButton = prefill?.let { pre ->
                PrefillHistoryButton(project, pre, field) {
                    field.text = it
                    validation?.invoke()
                }
            }
            val panel = panel {
                row {
                    label(message)
                }
                validationPanel?.let {
                    row {
                        cell(it)
                    }
                }
                row {
                    scrollCell(field)
                    prefillButton?.let {
                        cell(it)
                    }
                }
                row {
                    cell(btnYes)
                    cell(btnNo)
                }
            }
            val popup = popupFactory.createDialogBalloonBuilder(panel, title)
                .createBalloon()
            btnYes.addActionListener {
                popup.hide()
                prefill?.let {
                    prefillOperator.addPrefill(it, field.text)
                }
                onYes(field.text)
            }
            btnNo.addActionListener {
                popup.hide()
                onNo()
            }
            val location: RelativePoint = popupFactory.guessBestPopupLocation(focusComponent)

            popup.show(location, position)
            popup.setDefaultButton(panel, btnYes)
            field.requestFocus(true)
        } catch (e: Exception) {
            reportBadStuff("Failed asking for input \"$title\"", e)
        }
    }
}
