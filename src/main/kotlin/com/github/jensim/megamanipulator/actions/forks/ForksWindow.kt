package com.github.jensim.megamanipulator.actions.forks

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CodeHostSelector
import com.github.jensim.megamanipulator.ui.uiProtectedOperation
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.layout.panel
import javax.swing.JComponent

object ForksWindow : ToolWindowTab {

    private val codeHostSelect = CodeHostSelector()
    private val staleForkList = JBList<String>()
    private val scroll = JBScrollPane(staleForkList)

    override fun refresh() {
        staleForkList.setListData(emptyArray())
        codeHostSelect.load()
    }

    override val index: Int = 5
    override val content: JComponent = panel {
        row {
            component(codeHostSelect)
            button("Load forks without OPEN PRs") {
                // TODO
                uiProtectedOperation("Load forks without OPEN PRs") {
                    PrRouter.getPrivateForkReposWithoutPRs(TODO(), TODO())
                }
                NotificationsOperator.show("To do", "Not built yet")
            }
            button("Delete remote forks") {
                // TODO
                NotificationsOperator.show("To do", "Not built yet")
            }
        }
        row {
            component(scroll)
        }
    }
}
