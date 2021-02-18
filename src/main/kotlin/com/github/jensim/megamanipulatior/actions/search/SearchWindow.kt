package com.github.jensim.megamanipulatior.actions.search

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.time.Duration
import javax.swing.JButton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.withTimeout

object SearchWindow {

    private val searchButton = JButton("Search")
    private val cloneButton = JButton("Clone selected")
    private val searchField = JBTextField(50)
    private val selector = JBList<SearchResult>()
    private val scroll = JBScrollPane(selector)

    val content = panel {
        row {
            component(searchButton)
            component(searchField)
            component(cloneButton)
        }
        row {
            component(scroll)
        }
    }

    init {
        cloneButton.isEnabled = false
        selector.addListSelectionListener {
            cloneButton.isEnabled = selector.selectedValuesList.isNotEmpty()
        }
        searchField.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
                if (e?.keyCode == KeyEvent.VK_ENTER) {
                    searchButton.doClick()
                }
            }

            override fun keyPressed(e: KeyEvent?) = Unit
            override fun keyReleased(e: KeyEvent?) = Unit
        })
        searchButton.addActionListener {
            println("IM KLIKKED!")//TODO
            searchButton.isEnabled = false
            cloneButton.isEnabled = false
            selector.setListData(emptyArray())
            GlobalScope.launch {
                withTimeout(Duration.ofMinutes(1)) {
                    val result = SearchOperator.search(searchField.text).toTypedArray()
                    selector.setListData(result)
                }
                searchButton.isEnabled = true
            }
        }
    }
}