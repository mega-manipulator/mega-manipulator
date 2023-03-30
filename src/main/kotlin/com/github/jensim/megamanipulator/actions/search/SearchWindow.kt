package com.github.jensim.megamanipulator.actions.search

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.searchhost.HoundSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.TabKey.tabTitleCloneHistory
import com.github.jensim.megamanipulator.toolswindow.TabSelectorService
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CloneDialogFactory
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.PrefillHistoryButton
import com.github.jensim.megamanipulator.ui.TableMenu
import com.github.jensim.megamanipulator.ui.TableMenu.MenuItem
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.components.BorderLayoutPanel
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JButton
import javax.swing.SwingUtilities

class SearchWindow(
    private val project: Project
) : ToolWindowTab {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val searchOperator: SearchOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val cloneOperator: CloneOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }
    private val prefillOperator: PrefillStringSuggestionOperator by lazy { project.service() }
    private val cloneDialogFactory: CloneDialogFactory by lazy { project.service() }
    private val tabSelectorService: TabSelectorService by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }

    private val empty = "-" to HoundSettings("http://0.0.0.0", null, emptyMap())
    private val searchHostSelect = ComboBox<Pair<String, SearchHostSettings>>()
    private val searchHostLink = JButton("SearchDocs", AllIcons.Toolwindows.Documentation)
    private val searchButton = JButton("Search", AllIcons.Actions.Search)
    private val cloneButton = JButton("Clone selected", AllIcons.Vcs.Clone)
    private val searchField = JBTextField(50)
    private val historyButton = PrefillHistoryButton(project, PrefillString.SEARCH, searchField) {
        searchField.text = it
        search()
    }
    private val table = GeneralKtDataTable(
        type = SearchResult::class,
        columns = listOf(
            "Code host" to { it.codeHostName },
            "Project" to { it.project },
            "Repo" to { it.repo },
        ),
    )
    private val tableMenu = TableMenu<Set<SearchResult>>(
        table,
        listOf(
            MenuItem(header = { "Clone selected (${it.size})" }, isEnabled = { it.isNotEmpty() }) {
                cloneSelected(it)
            }
        )
    )
    private val scroll = JBScrollPane(table).apply {
        preferredSize = Dimension(10_000, 1000)
    }

    private val topContent = panel {
        row {
            cell(searchHostLink)
            cell(searchHostSelect)
            cell(historyButton)
            cell(searchField)
            cell(searchButton)
            cell(cloneButton)
            cell(OnboardingButton(project, TabKey.tabTitleSearch, OnboardingId.SEARCH_TAB))
                .align(AlignX.RIGHT)
        }
    }

    override val content = BorderLayoutPanel().apply {
        addToCenter(scroll)
        addToTop(topContent)
    }

    init {
        searchHostSelect.addItem(empty)
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
        table.apply {
            addListSelectionListener {
                cloneButton.isEnabled = table.selectedValuesList.isNotEmpty()
            }
            addClickListener { mouseEvent, _ ->
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    tableMenu.show(mouseEvent, table.selectedValuesList.toSet())
                }
            }
        }
        searchField.addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent?) {
                if (e?.extendedKeyCode == KeyEvent.VK_ENTER) {
                    search()
                }
            }

            override fun keyPressed(e: KeyEvent?) = Unit
            override fun keyReleased(e: KeyEvent?) = Unit
        })
        searchButton.addActionListener {
            search()
        }
        cloneButton.addActionListener {
            cloneSelected(table.selectedValuesList.toSet())
        }
    }

    private fun cloneSelected(selected: Set<SearchResult>) {
        if (selected.isNotEmpty()) {
            cloneDialogFactory.showCloneDialog(cloneButton) { branch: String, shallow: Boolean, sparseDef: String? ->
                cloneOperator.clone(repos = selected, branchName = branch, shallow = shallow, sparseDef = sparseDef)
                tabSelectorService.selectTab(tabTitleCloneHistory)
                table.clearSelection()
                prefillOperator.addPrefill(PrefillString.BRANCH, branch)
            }
        }
    }

    private fun search() {
        searchButton.isEnabled = false
        cloneButton.isEnabled = false
        table.setListData(emptyList())
        val searchText = searchField.text
        val result: List<SearchResult> = searchHostSelect.selectedItem?.let { searchHost: Any ->
            (searchHost as? Pair<String, SearchHostSettings>)?.first?.let { searchHostName ->
                uiProtector.uiProtectedOperation("Seraching") {
                    searchOperator.search(searchHostName, searchText)
                }
            }
        }.orEmpty().toList()
        table.setListData(result)
        searchButton.isEnabled = true
        prefillOperator.addPrefill(PrefillString.SEARCH, searchText)
    }

    override fun refresh() {
        onboardingOperator.registerTarget(OnboardingId.SEARCH_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_DOC_BUTTON, searchHostLink)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_HOST_SELECT, searchHostSelect)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_INPUT, searchField)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_BUTTON, searchButton)
        onboardingOperator.registerTarget(OnboardingId.SEARCH_CLONE_BUTTON, cloneButton)

        searchHostSelect.removeAllItems()
        settingsFileOperator.readSettings()?.searchHostSettings?.forEach { (name: String, group: SearchHostSettingsGroup) ->
            searchHostSelect.addItem(name to group.value())
        }
        if (searchHostSelect.itemCount == 0) {
            searchButton.isEnabled = false
            searchHostSelect.addItem(empty)
        } else {
            searchButton.isEnabled = true
        }

        try {
            if (searchField.text.isNullOrBlank()) {
                prefillOperator.getPrefill(PrefillString.SEARCH)?.let {
                    searchField.text = it
                }
            }
        } catch (e: Exception) {
            val msg = "Failed setting initial prefill for search"
            logger.error(msg)
            notificationsOperator.show(msg, "Something failed horribly<br>${e.javaClass.simpleName}<br>${e.message}", ERROR)
        }
    }
}
