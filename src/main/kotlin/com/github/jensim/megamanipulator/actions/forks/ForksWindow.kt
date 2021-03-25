package com.github.jensim.megamanipulator.actions.forks

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.vcs.ForkRepoWrapper
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.mapConcurrentWithProgress
import com.github.jensim.megamanipulator.ui.uiProtectedOperation
import com.intellij.notification.NotificationType
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.layout.panel
import javax.swing.JComponent

object ForksWindow : ToolWindowTab {

    override val index: Int = 5

    private val codeHostSelect = CodeHostSelector()
    private val staleForkList = JBList<ForkRepoWrapper>()
    private val scroll = JBScrollPane(staleForkList)

    init {
        staleForkList.addCellRenderer { it.asDisplayString() }
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
                    uiProtectedOperation("Load forks without OPEN PRs") {
                        PrRouter.getPrivateForkReposWithoutPRs(item.searchHostName, item.codeHostName)
                    }?.let { result: List<ForkRepoWrapper> ->
                        staleForkList.setListData(result.toTypedArray())
                        if (result.isEmpty()) {
                            NotificationsOperator.show(
                                    title = "No result",
                                    body = "Maybe you have zero forks without PRs?",
                                    type = NotificationType.INFORMATION
                            )
                        }
                    }
                }
            }
            button("Delete remote forks") {
                staleForkList.selectedValuesList.mapConcurrentWithProgress(
                        title = "Delete forks",
                        extraText2 = { it.asDisplayString() }
                ) { fork ->
                    PrRouter.deletePrivateRepo(fork)
                }
            }
        }
        row {
            component(scroll)
        }
    }
}
