package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.settings.SerializationHolder
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent

object PullRequestWindow : ToolWindowTab {

    private data class CodeHostSelect(
        val searchHostName: String,
        val codeHostName: String,
    ) {
        override fun toString(): String = "$searchHostName / $codeHostName"
    }

    override val index: Int = 4
    private val codeHostSelect = ComboBox<CodeHostSelect>()
    private val prList = JBList<PullRequest>()
    private val prScroll = JBScrollPane(prList)
    private val peekArea = JBTextArea()
    private val peekScroll = JBScrollPane(peekArea)
    private val menu = PullRequestActionsMenu(prProvider = { prList.selectedValuesList }, postActionHook = this::refresh)
    private val menuOpenButton = JButton("Actions")
    override val content: JComponent = panel {
        row {
            component(codeHostSelect)
            button("Fetch PRs") {
                fetchPRs()
            }
            right {
                component(menuOpenButton)
            }
        }
        row {
            component(prScroll)
            component(peekScroll)
        }
    }

    init {
        menuOpenButton.isEnabled = false
        menuOpenButton.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                val selected = codeHostSelect.selectedItem as CodeHostSelect?
                menu.codeHostName = selected?.codeHostName
                menu.searchHostName = selected?.searchHostName
                menu.show(menuOpenButton, e.x, e.y)
            }

            override fun mousePressed(e: MouseEvent?) = Unit
            override fun mouseReleased(e: MouseEvent?) = Unit
            override fun mouseEntered(e: MouseEvent?) = Unit
            override fun mouseExited(e: MouseEvent?) = Unit
        })

        prList.addCellRenderer { "${it.project}/${it.repo} ${it.title}" }
        prList.addListSelectionListener {
            menuOpenButton.isEnabled = false
            prList.selectedValuesList.firstOrNull()?.let {
                peekArea.text = SerializationHolder.yamlObjectMapper.writeValueAsString(it)
                menuOpenButton.isEnabled = true
            }
        }
    }

    override fun refresh() {
        peekArea.text = ""
        codeHostSelect.removeAllItems()
        prList.setListData(emptyArray())
        SettingsFileOperator.readSettings()?.searchHostSettings?.forEach { (searchName, searchWrapper) ->
            searchWrapper.codeHostSettings.keys.map { codeHost ->
                codeHostSelect.addItem(CodeHostSelect(searchName, codeHost))
            }
        }
        fetchPRs()
    }

    private fun fetchPRs() {
        prList.setListData(emptyArray())
        (codeHostSelect.selectedItem as CodeHostSelect?)?.let { selected ->
            val prs: List<PullRequest>? = uiProtectedOperation("Fetching PRs") {
                PrRouter.getAllPrs(selected.searchHostName, selected.codeHostName)
            }
            prList.setListData(prs?.toTypedArray())
        }
    }
}
