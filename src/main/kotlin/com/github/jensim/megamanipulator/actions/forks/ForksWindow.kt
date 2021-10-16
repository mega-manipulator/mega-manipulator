package com.github.jensim.megamanipulator.actions.forks

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.layout.panel
import javax.swing.JButton
import javax.swing.JComponent

class ForksWindow(project: Project) : ToolWindowTab {

    private val prRouter: PrRouter by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }

    private val deleteButton = JButton("Delete selected forks")
    private val codeHostSelect = CodeHostSelector(settingsFileOperator)
    private val staleForkList = JBList<RepoWrapper>()
    private val scroll = JBScrollPane(staleForkList)

    init {
        staleForkList.setListData(emptyArray())
        staleForkList.addCellRenderer { it.asPathString() }
        deleteButton.isEnabled = false
        deleteButton.apply {
            addActionListener {
                DialogGenerator.showConfirm(
                    title = "Delete selected forks?",
                    message = """
                                Are you sure?
                                Really, really, sure?
                                """.trimIndent(),
                    focusComponent = this
                ) {
                    uiProtector.mapConcurrentWithProgress(
                        title = "Delete forks",
                        extraText2 = { it.asPathString() },
                        data = staleForkList.selectedValuesList,
                    ) { fork ->
                        prRouter.deletePrivateRepo(fork)
                    }
                }
            }
        }
    }

    override fun refresh() {
        codeHostSelect.load()
    }

    override val content: JComponent = panel {
        row {
            cell {
                component(codeHostSelect)
                button("Load forks without OPEN PRs") {
                    staleForkList.setListData(emptyArray())
                    codeHostSelect.selectedItem?.let { item ->
                        uiProtector.uiProtectedOperation("Load forks without OPEN PRs") {
                            prRouter.getPrivateForkReposWithoutPRs(item.searchHostName, item.codeHostName)
                        }?.let { result: List<RepoWrapper> ->
                            staleForkList.setListData(result.toTypedArray())
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
                component(deleteButton)
            }
        }
        row {
            component(scroll)
        }
    }
}
