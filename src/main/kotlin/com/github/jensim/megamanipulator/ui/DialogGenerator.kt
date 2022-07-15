package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.ide.DataManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jetbrains.concurrency.await
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.text.JTextComponent

class DialogGenerator(private val project: Project) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }

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
                    component(JBScrollPane(JBLabel(message1)))
                }
                row {
                    component(yesBtn)
                    component(noBtn)
                }
            }
            val popup = popupFactory.createDialogBalloonBuilder(panel, title)
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
            yesBtn.requestFocus(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun String.convertMultiLineToHtml() = "<html>${replace("\n", "<br>\n")}</html>"

    fun askForInput(
        title: String,
        message: String,
        prefill: PrefillString?,
        field: JTextComponent = JBTextField(),
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
                        label("Invalid input, must match pattern: $pattern", JBFont.small(), UIUtil.FontColor.BRIGHTER)
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
                        component(it)
                    }
                }
                row {
                    cell {
                        scrollPane(field)
                        prefillButton?.let {
                            component(it)
                        }
                    }
                }
                row {
                    cell {
                        component(btnYes)
                        component(btnNo)
                    }
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
            field.requestFocus(true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
