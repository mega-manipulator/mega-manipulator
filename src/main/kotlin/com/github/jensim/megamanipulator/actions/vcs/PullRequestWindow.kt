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
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlinx.serialization.encodeToString
import me.xdrop.fuzzywuzzy.FuzzySearch
import org.slf4j.LoggerFactory

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
    private val fetchAuthorButton = JButton("Fetch author PRs")
    private val fetchAssigneeButton = JButton("Fetch assigned PRs")

    private val split = JBSplitter(false, 0.7f).apply {
        firstComponent = prScroll
        secondComponent = peekScroll
        preferredSize = Dimension(4000, 1000)
    }

    override val content: JComponent = panel {
        row {
            cell {
                component(codeHostSelect)
                component(fetchAuthorButton)
                component(fetchAssigneeButton)
                label("Filter:")
                component(filterField)
            }
            right {
                cell {
                    buttonGroup {
                        component(OnboardingButton(project, TabKey.tabTitlePRsManage, OnboardingId.PR_TAB))
                        component(menuOpenButton)
                    }
                }
            }
        }
        row {
            component(split)
        }
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
                    peekArea.text = SerializationHolder.readableJson.encodeToString(selected.first())
                }
                menuOpenButton.isEnabled = true
            } else {
                peekArea.text = ""
            }
        }
        fetchAuthorButton.addActionListener {
            fetchAuthoredPRs()
        }
        fetchAssigneeButton.addActionListener {
            fetchAssignedPRs()
        }
    }

    override fun refresh() {
        codeHostSelect.load()

        onboardingOperator.registerTarget(OnboardingId.PR_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.PR_LIST_FILTER_FIELD, filterField)
        onboardingOperator.registerTarget(OnboardingId.PR_FETCH_ASSIGNEE_PR_BUTTON, fetchAssigneeButton)
        onboardingOperator.registerTarget(OnboardingId.PR_FETCH_AUTHOR_PR_BUTTON, fetchAuthorButton)
        onboardingOperator.registerTarget(OnboardingId.PR_CODE_HOST_SELECT, codeHostSelect)
        onboardingOperator.registerTarget(OnboardingId.PR_ACTIONS_BUTTON, menuOpenButton)
        onboardingOperator.registerTarget(OnboardingId.PR_ACTIONS_RESULT_AREA, split)
    }

    private fun fetchAssignedPRs() {
        prTable.setListData(emptyList())
        (codeHostSelect.selectedItem)?.let { selected ->
            filterField.text = ""
            val prs: List<PullRequestWrapper>? = uiProtector.uiProtectedOperation("Fetching PRs") {
                prRouter.getAllReviewPrs(selected.searchHostName, selected.codeHostName)
            }
            setPrs(prs)
        } ?: log.warn("No codeHost selected")
    }

    private fun fetchAuthoredPRs() {
        prTable.setListData(emptyList())
        (codeHostSelect.selectedItem)?.let { selected ->
            filterField.text = ""
            val prs: List<PullRequestWrapper>? = uiProtector.uiProtectedOperation("Fetching PRs") {
                prRouter.getAllAuthorPrs(selected.searchHostName, selected.codeHostName)
            }
            setPrs(prs)
        } ?: log.warn("No codeHost selected")
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
