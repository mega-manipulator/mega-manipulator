package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.PullRequestLoaderDialogGenerator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign.RIGHT
import com.intellij.util.ui.components.BorderLayoutPanel
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PullRequestWindow(project: Project) : ToolWindowTab {

    private val prRouter: PrRouter by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }

    private val log = LoggerFactory.getLogger(javaClass)

    private val filterField = JBTextField(50)
    private val pullRequests: MutableList<PullRequestWrapper> = mutableListOf()
    private val codeHostSelect = CodeHostSelector(settingsFileOperator)
    private val prTable = GeneralKtDataTable(
        PullRequestWrapper::class,
        listOf(
            "Project" to { it.project() },
            "Base repo" to { it.baseRepo() },
            "Title" to { it.title() },
            "Description" to { it.body() },
            "Author" to { it.author() ?: "?" },
        )
    )
    private val prScroll = JBScrollPane(prTable)
    private val peekArea = JBTextArea()
    private val peekScroll = JBScrollPane(peekArea)
    private val menuOpenButton = JButton("Actions")
    private val fetchPRsButton = JButton("Fetch PRs")

    private val split = JBSplitter(false, 0.7f).apply {
        firstComponent = prScroll
        secondComponent = peekScroll
    }

    private val topContent: JComponent = panel {
        row {
            cell(codeHostSelect)
            cell(fetchPRsButton)
            label("Filter:")
            cell(filterField)
            cell(OnboardingButton(project, TabKey.tabTitlePRsManage, OnboardingId.PR_TAB))
                .horizontalAlign(RIGHT)
            cell(menuOpenButton)
                .horizontalAlign(RIGHT)
        }
    }
    override val content: JComponent = BorderLayoutPanel().apply {
        addToTop(topContent)
        addToCenter(split)
    }

    init {
        peekArea.text = ""
        prScroll.preferredSize = Dimension(4000, 1000)
        peekScroll.preferredSize = Dimension(4000, 1000)

        menuOpenButton.isEnabled = false
        menuOpenButton.addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                val pullRequestActionsMenu = PullRequestActionsMenu(
                    project = project,
                    focusComponent = menuOpenButton,
                    prProvider = { prTable.selectedValuesList }
                )
                pullRequestActionsMenu.show(menuOpenButton, e.x, e.y)
            }

            override fun mousePressed(e: MouseEvent?) = Unit
            override fun mouseReleased(e: MouseEvent?) = Unit
            override fun mouseEntered(e: MouseEvent?) = Unit
            override fun mouseExited(e: MouseEvent?) = Unit
        })

        filterField.minimumSize = Dimension(200, 30)
        filterField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                updateFilteredPrs()
            }

            override fun removeUpdate(e: DocumentEvent?) {
                updateFilteredPrs()
            }

            override fun changedUpdate(e: DocumentEvent?) {
                updateFilteredPrs()
            }
        })

        prTable.addListSelectionListener {
            menuOpenButton.isEnabled = false
            val selected: List<PullRequestWrapper> = prTable.selectedValuesList
            if (selected.isNotEmpty()) {
                if (selected.size == 1) {
                    peekArea.text = SerializationHolder.objectMapper.writeValueAsString(selected.first())
                }
                menuOpenButton.isEnabled = true
            } else {
                peekArea.text = ""
            }
        }
        fetchPRsButton.addActionListener {
            fetchPRs()
        }
        codeHostSelect.addActionListener {
            val hasSelection = codeHostSelect.selectedItem != null
            fetchPRsButton.isEnabled = hasSelection
        }
        val hasSelection = codeHostSelect.selectedItem != null
        fetchPRsButton.isEnabled = hasSelection
    }

    override fun refresh() {
        codeHostSelect.load()

        onboardingOperator.registerTarget(OnboardingId.PR_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.PR_LIST_FILTER_FIELD, filterField)
        onboardingOperator.registerTarget(OnboardingId.PR_FETCH_AUTHOR_PR_BUTTON, fetchPRsButton)
        onboardingOperator.registerTarget(OnboardingId.PR_CODE_HOST_SELECT, codeHostSelect)
        onboardingOperator.registerTarget(OnboardingId.PR_ACTIONS_BUTTON, menuOpenButton)
        onboardingOperator.registerTarget(OnboardingId.PR_ACTIONS_RESULT_AREA, split)
    }

    private fun fetchPRs() {
        (codeHostSelect.selectedItem)?.let { selected: CodeHostSelector.CodeHostSelect ->
            settingsFileOperator.readSettings()?.let {
                it.resolveSettings(selected.searchHostName, selected.codeHostName)?.let { (_, settings) ->
                    PullRequestLoaderDialogGenerator.generateDialog(
                        focus = fetchPRsButton,
                        type = settings.codeHostType,
                    ) { state: String?, role: String?, limit: Int ->
                        prTable.setListData(emptyList())
                        filterField.text = ""
                        val prs: List<PullRequestWrapper>? = uiProtector.uiProtectedOperation("Fetching PRs") {
                            prRouter.getAllPrs(searchHost = selected.searchHostName, codeHost = selected.codeHostName, role = role, state = state, limit = limit)
                        }
                        setPrs(prs)
                    }
                }
            }
        }
    }

    private fun setPrs(prs: List<PullRequestWrapper>?) {
        log.info("Setting prs, new count is ${prs?.size}")
        pullRequests.clear()
        prs?.let { pullRequests.addAll(it) }
        val filtered = updateFilteredPrs()
        if (filtered.isNotEmpty()) {
            prTable.selectFirst()
        }
        content.validate()
        content.repaint()
    }

    private fun updateFilteredPrs(): List<PullRequestWrapper> {
        val filtered = pullRequests.filter {
            filterField.text.isBlank() || FuzzySearch.tokenSetRatio(it.raw, filterField.text) == 100
        }.sortedBy { "${it.project()}/${it.baseRepo()} ${it.title()}" }
        prTable.setListData(filtered)
        return filtered
    }
}
