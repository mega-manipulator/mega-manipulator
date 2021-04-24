package com.github.jensim.megamanipulator.actions.forks

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.RepoWrapper
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class ForksWindow(
    private val prRouter: PrRouter,
    private val notificationsOperator: NotificationsOperator,
    private val uiProtector: UiProtector,
    settingsFileOperator: SettingsFileOperator,
) : ToolWindowTab {

    override val index: Int = 5

    private val codeHostSelect = CodeHostSelector(settingsFileOperator)
    private val staleForkList = JBList<RepoWrapper>()
    private val scroll = JBScrollPane(staleForkList)

    init {
        staleForkList.addCellRenderer { it.asPathString() }
    }

    override fun refresh() {
        staleForkList.setListData(emptyArray())
        codeHostSelect.load()
    }

    override val content: JComponent = panel {
        row {
            component(codeHostSelect)
            button("Load forks without OPEN PRs") {
                staleForkList.setListData(emptyArray())
                codeHostSelect.selectedItem?.let { item ->
                    uiProtector.uiProtectedOperation("Load forks without OPEN PRs") {
                        prRouter.getPrivateForkReposWithoutPRs(item.searchHostName, item.codeHostName)
                    }?.let { result: List<RepoWrapper> ->
                        staleForkList.setListData(result.toTypedArray())
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
            button("Delete remote forks") {
                uiProtector.mapConcurrentWithProgress(
                    title = "Delete forks",
                    extraText2 = { it.asPathString() },
                    data = staleForkList.selectedValuesList,
                ) { fork ->
                    prRouter.deletePrivateRepo(fork)
                }
            }
        }
        row {
            component(scroll)
        }
    }
}
