package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.SearchHostSettings
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.intellij.util.castSafelyTo
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton

class SearchWindow(
    private val project: Project
) : ToolWindowTab {

    private val searchOperator: SearchOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val cloneOperator: CloneOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }

    private val searchHostSelect = ComboBox<Pair<String, SearchHostSettings>>()
    private val searchHostLink = JButton("SearchDocs", AllIcons.Toolwindows.Documentation)
    private val searchButton = JButton("Search", AllIcons.Actions.Search)
    private val cloneButton = JButton("Clone selected", AllIcons.Vcs.Clone)
    private val searchField = JBTextField(50)
    private val selector = JBList<SearchResult>()
    private val scroll = JBScrollPane(selector)

    override val content = panel {
        row {
            cell {
                component(searchHostLink)
                component(searchHostSelect)
                component(searchField)
                component(searchButton)
                component(cloneButton)
            }
        }
        row {
            component(scroll)
        }
    }

    init {
        searchHostSelect.addCellRenderer { it.first }
        searchHostSelect.addActionListener {
            searchHostLink.isEnabled = searchHostSelect.selectedItem != null
        }
        searchHostLink.addActionListener {
            searchHostSelect.selectedItem?.let {
                val link = (it as Pair<String, SearchHostSettings>).second.docLinkHref
                com.intellij.ide.BrowserUtil.browse(link)
            }
        }
        searchHostLink.preferredSize = Dimension(30, 30)
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
            val searchText = searchField.text
            val result: Array<SearchResult> = searchHostSelect.selectedItem?.let { searchHost: Any ->
                searchHost.castSafelyTo<Pair<String, SearchHostSettings>>()?.first?.let { searchHostName ->
                    uiProtector.uiProtectedOperation("Seraching") {
                        searchOperator.search(searchHostName, searchText)
                    }
                }
            }.orEmpty().toTypedArray()
            selector.setListData(result)
            searchButton.isEnabled = true
            MegaManipulatorSettingsState.getInstance()?.let {
                it.state.lastSearch = searchText
            }
        }
        cloneButton.addActionListener {
            val selected = selector.selectedValuesList.toSet()
            if (selected.isNotEmpty()) {
                val prefill: String? = PrefillStringSuggestionOperator.getPrefill(PrefillString.BRANCH)
                DialogGenerator.askForInput(
                    title = "Branch",
                    message = "branch name",
                    prefill = prefill,
                    focusComponent = cloneButton
                ) { branch ->
                    cloneOperator.clone(selected, branch)
                    selector.clearSelection()
                    PrefillStringSuggestionOperator.setPrefill(PrefillString.BRANCH, branch)
                }
            }
        }
    }

    override fun refresh() {
        onboardingOperator.registerTarget(OnboardingId.SEARCH_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_DOC_BUTTON, searchHostLink)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_HOST_SELECT,searchHostSelect)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_INPUT, searchField)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_BUTTON,searchButton)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_CLONE_BUTTON, cloneButton)

        searchHostSelect.removeAllItems()
        settingsFileOperator.readSettings()?.searchHostSettings?.forEach {
            searchHostSelect.addItem(it.toPair())
        }

        searchButton.isEnabled = searchHostSelect.itemCount > 0
        if (searchField.text.isNullOrBlank()) {
            MegaManipulatorSettingsState.getInstance()?.let {
                searchField.text = it.state.lastSearch
            }
        }
    }
}
