package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton

class SearchWindow(
    private val searchOperator: SearchOperator,
    private val settingsFileOperator: SettingsFileOperator,
    private val cloneOperator: CloneOperator,
    private val uiProtector: UiProtector,
) : ToolWindowTab {

    private val searchHostSelect = ComboBox<String>()
    private val searchButton = JButton("Search")
    private val cloneButton = JButton("Clone selected")
    private val searchField = JBTextField(50)
    private val selector = JBList<SearchResult>()
    private val scroll = JBScrollPane(selector)

    override val content = panel {
        row {
            component(searchHostSelect)
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
            val result: Array<SearchResult> = searchHostSelect.selectedItem?.let { searchHostName ->
                uiProtector.uiProtectedOperation("Seraching") {
                    searchOperator.search(searchHostName as String, searchField.text)
                }
            }.orEmpty().toTypedArray()
            selector.setListData(result)
            searchButton.isEnabled = true
        }
        cloneButton.addActionListener {
            val selected = selector.selectedValuesList.toSet()
            if (selected.isNotEmpty()) {
                cloneOperator.clone(selected)
                selector.clearSelection()
            }
        }
    }

    override val index: Int = 1
    override fun refresh() {
        searchHostSelect.removeAllItems()
        settingsFileOperator.readSettings()?.searchHostSettings?.keys?.forEach {
            searchHostSelect.addItem(it)
        }
        searchButton.isEnabled = searchHostSelect.itemCount > 0
    }
}
