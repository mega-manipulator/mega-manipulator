package com.github.jensim.megamanipulator.actions.forks

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.TableMenu
import com.github.jensim.megamanipulator.ui.TableMenu.MenuItem
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign.RIGHT
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.SwingUtilities

class ForksWindow(project: Project) : ToolWindowTab {

    private val prRouter: PrRouter by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val deleteButton = JButton("Delete selected forks", AllIcons.Toolwindows.Problems)
    private val codeHostSelect = CodeHostSelector(settingsFileOperator)
    private val staleForkTable = GeneralKtDataTable(
        RepoWrapper::class,
        listOf(
            "BaseProject" to { it.getForkOriginProject() ?: "?" },
            "Project" to { it.getProject() },
            "Repo" to { it.getRepo() },
        )
    )
    private val contextMenu = TableMenu<List<RepoWrapper>>(
        staleForkTable,
        menus = listOf(
            MenuItem("Load forks without OPEN PRs") {
                load()
            },
            MenuItem({ "Delete selected forks (${it.size})" }, { it.isNotEmpty() }) { _ ->
                deleteSelected()
            },
        )
    )
    private val scroll = JBScrollPane(staleForkTable)
    private val loadStaleForksButton = JButton("Load forks without OPEN PRs")

    private val topContent: JComponent = panel {
        row {
            cell(codeHostSelect)
            cell(loadStaleForksButton)
            cell(deleteButton)
            cell(OnboardingButton(project, TabKey.tabTitleForks, OnboardingId.FORK_TAB))
                .horizontalAlign(RIGHT)
        }
    }

    override val content = BorderLayoutPanel().apply {
        addToTop(topContent)
        addToCenter(scroll)
    }

    init {
        deleteButton.isEnabled = false
        deleteButton.apply {
            addActionListener {
                deleteSelected()
            }
        }
        loadStaleForksButton.addActionListener {
            load()
        }
        codeHostSelect.addActionListener {
            val hasSelection = codeHostSelect.selectedItem != null
            loadStaleForksButton.isEnabled = hasSelection
        }
        val hasSelection = codeHostSelect.selectedItem != null
        loadStaleForksButton.isEnabled = hasSelection
        staleForkTable.addClickListener { mouseEvent, _ ->
            if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                contextMenu.show(mouseEvent, staleForkTable.selectedValuesList)
            }
        }
    }

    private fun deleteSelected() {
        dialogGenerator.showConfirm(
            title = "Delete selected forks?",
            message = """
            Are you sure?
            Really, really, sure?
            """.trimIndent(),
            focusComponent = deleteButton
        ) {
            uiProtector.mapConcurrentWithProgress(
                title = "Delete forks",
                extraText2 = { it.asPathString() },
                data = staleForkTable.selectedValuesList,
            ) { fork ->
                prRouter.deletePrivateRepo(fork)
            }
            load()
        }
    }

    private fun load() {
        staleForkTable.setListData(emptyList())
        codeHostSelect.selectedItem?.let { item ->
            uiProtector.uiProtectedOperation("Load forks without OPEN PRs") {
                prRouter.getPrivateForkReposWithoutPRs(item.searchHostName, item.codeHostName)
            }?.let { result: List<RepoWrapper> ->
                staleForkTable.setListData(result)
                staleForkTable.selectFirst()
                deleteButton.isEnabled = result.isNotEmpty()
                if (result.isEmpty()) {
                    notificationsOperator.show(
                        title = "No result",
                        body = "Maybe you have zero forks without PRs?",
                        type = NotificationType.INFORMATION
                    )
                }
            }
        }
    }

    override fun refresh() {
        codeHostSelect.load()

        onboardingOperator.registerTarget(OnboardingId.FORK_LIST_AREA, scroll)
        onboardingOperator.registerTarget(OnboardingId.FORK_DELETE_STALE_FORK_BUTTON, deleteButton)
        onboardingOperator.registerTarget(OnboardingId.FORK_LOAD_STALE_FORK_BUTTON, loadStaleForksButton)
        onboardingOperator.registerTarget(OnboardingId.FORK_TAB, content)
    }
}
