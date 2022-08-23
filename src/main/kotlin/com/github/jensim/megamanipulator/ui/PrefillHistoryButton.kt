package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JComponent

class PrefillHistoryButton(
    project: Project,
    private val prefillString: PrefillString,
    private val focusComponent: JComponent,
    private val action: (String) -> Unit,
) : JBLabel(AllIcons.Actions.ToggleVisibility) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val prefillStringSuggestionOperator: PrefillStringSuggestionOperator by lazy { project.service() }

    init {
        toolTipText = "Show history"
        preferredSize = Dimension(32, 32)
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                try {
                    val prefills: List<String> = prefillStringSuggestionOperator.getPrefills(prefillString)

                    val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
                    val step = MyStep(prefills, action)
                    val popup = popupFactory.createListPopup(step)
                    val location: RelativePoint = popupFactory.guessBestPopupLocation(focusComponent)
                    popup.show(location)
                } catch (e: Exception) {
                    logger.error("Failed opening up the prefil suggestion popup for $prefillString", e)
                }
            }

            override fun mousePressed(e: MouseEvent?) = Unit
            override fun mouseReleased(e: MouseEvent?) = Unit
            override fun mouseEntered(e: MouseEvent?) = Unit
            override fun mouseExited(e: MouseEvent?) = Unit
        })
    }

    private class MyStep(
        prefills: List<String>,
        private val action: (String) -> Unit,
    ) : BaseListPopupStep<String>("History", prefills.toMutableList()) {
        override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? = super
            .onChosen(selectedValue, finalChoice).also {
                action(selectedValue)
            }
    }
}
