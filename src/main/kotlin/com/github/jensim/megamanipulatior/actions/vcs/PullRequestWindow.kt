package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.settings.SerializationHolder
import com.github.jensim.megamanipulatior.settings.SettingsFileOperator
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.DialogGenerator
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.MouseInfo
import javax.swing.JButton
import javax.swing.JComponent

object PullRequestWindow : ToolWindowTab {

    private data class CodeHostSelect(
        val searchHostName: String,
        val codeHostName: String,
    ) {
        override fun toString(): String = "$searchHostName / $codeHostName"
    }


    override val index: Int = 3
    private val codeHostSelect = ComboBox<CodeHostSelect>()
    private val updateButton = JButton("Update PR")
    private val declineButton = JButton("Decline PR")
    private val prList = JBList<PullRequest>()
    private val prScroll = JBScrollPane(prList)
    private val peekArea = JBTextArea()
    private val peekScroll = JBScrollPane(peekArea)
    private val menu = PullRequestActionsMenu { prList.selectedValuesList }
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
        menuOpenButton.addActionListener {
            val point = MouseInfo.getPointerInfo().location
            menu.show(menuOpenButton, point.x, point.y)
        }
        declineButton.isEnabled = false
        prList.addCellRenderer { "${it.project}/${it.repo} ${it.title}" }
        prList.addListSelectionListener {
            prList.selectedValuesList.firstOrNull()?.let {
                peekArea.text = SerializationHolder.yamlObjectMapper.writeValueAsString(it)
                declineButton.isEnabled = true
                menuOpenButton.isEnabled = true
            }
        }
        declineButton.addActionListener {
            DialogGenerator.showConfirm("Decline PRs", "Decline ${prList.selectedValuesList.size} PRs") {
                prList.selectedValuesList.forEach {
                    PrRouter.closePr(it)
                }
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
            PrRouter.getAllPrs(selected.searchHostName, selected.codeHostName)?.let {
                prList.setListData(it.toTypedArray())
            }
        }
    }
}
