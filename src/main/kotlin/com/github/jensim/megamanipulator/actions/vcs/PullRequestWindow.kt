package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import kotlinx.serialization.encodeToString
import me.xdrop.fuzzywuzzy.FuzzySearch
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PullRequestWindow(
    private val prRouter: PrRouter,
    private val serializationHolder: SerializationHolder,
    private val uiProtector: UiProtector,
) : ToolWindowTab {

    companion object {

        val instance by lazy {
            PullRequestWindow(
                prRouter = PrRouter.instance,
                serializationHolder = SerializationHolder.instance,
                uiProtector = UiProtector.instance,
            )
        }
    }

    override val index: Int = 4

    private val search = JBTextField(50)
    private val pullRequests: MutableList<PullRequestWrapper> = mutableListOf()
    private val codeHostSelect = CodeHostSelector()
    private val prList = JBList<PullRequestWrapper>()
    private val prScroll = JBScrollPane(prList)
    private val peekArea = JBTextArea()
    private val peekScroll = JBScrollPane(peekArea)
    private val menu = PullRequestActionsMenu.instance(prProvider = { prList.selectedValuesList }, postActionHook = {})
    private val menuOpenButton = JButton("Actions")
    override val content: JComponent = panel {
        row {
            component(codeHostSelect)
            button("Fetch PRs") {
                fetchPRs()
            }
            component(search)
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
                val selected = codeHostSelect.selectedItem
                menu.codeHostName = selected?.codeHostName
                menu.searchHostName = selected?.searchHostName
                menu.show(menuOpenButton, e.x, e.y)
            }

            override fun mousePressed(e: MouseEvent?) = Unit
            override fun mouseReleased(e: MouseEvent?) = Unit
            override fun mouseEntered(e: MouseEvent?) = Unit
            override fun mouseExited(e: MouseEvent?) = Unit
        })

        search.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateFilteredPrs()
            override fun removeUpdate(e: DocumentEvent?) = updateFilteredPrs()
            override fun changedUpdate(e: DocumentEvent?) = updateFilteredPrs()
        })

        prList.addCellRenderer { "${it.project()}/${it.baseRepo()} ${it.title()}" }
        prList.addListSelectionListener {
            menuOpenButton.isEnabled = false
            prList.selectedValuesList.firstOrNull()?.let {
                peekArea.text = serializationHolder.readableJson.encodeToString(it)
                menuOpenButton.isEnabled = true
            }
        }
    }

    override fun refresh() {
        peekArea.text = ""
        prList.setListData(emptyArray())
        codeHostSelect.load()
    }

    private fun fetchPRs() {
        prList.setListData(emptyArray())
        (codeHostSelect.selectedItem)?.let { selected ->
            val prs: List<PullRequestWrapper>? = uiProtector.uiProtectedOperation("Fetching PRs") {
                prRouter.getAllPrs(selected.searchHostName, selected.codeHostName)
            }
            pullRequests.clear()
            prs?.let { pullRequests.addAll(it) }
            updateFilteredPrs()
        }
    }

    private fun updateFilteredPrs() {
        val filtered = pullRequests.filter {
            search.text.isBlank() || FuzzySearch.tokenSetRatio(it.raw, search.text) == 100
        }.sortedBy { "${it.project()}/${it.baseRepo()} ${it.title()}" }
        prList.setListData(filtered.toTypedArray())
    }
}
