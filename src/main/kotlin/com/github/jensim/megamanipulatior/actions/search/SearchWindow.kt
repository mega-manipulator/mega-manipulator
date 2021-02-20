package com.github.jensim.megamanipulatior.actions.search

import com.github.jensim.megamanipulatior.actions.git.clone.CloneOperator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.JOptionPane
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
            searchButton.isEnabled = false
            cloneButton.isEnabled = false
            selector.setListData(emptyArray())

            val result = SearchOperator.search(searchField.text).toTypedArray()
            selector.setListData(result)
            searchButton.isEnabled = true
        }
        cloneButton.addActionListener {
            val selected = selector.selectedValuesList.toSet()
            if (selected.isNotEmpty()) {
                GlobalScope.launch {
                    var branch: String? = null
                    while (branch == null || branch.isEmpty() || branch.contains(' ')) {
                        branch = JOptionPane.showInputDialog("Select branch name")
                    }
                    CloneOperator.clone(branch, selected)
                }
                selector.clearSelection()
            }
        }
    }
}
