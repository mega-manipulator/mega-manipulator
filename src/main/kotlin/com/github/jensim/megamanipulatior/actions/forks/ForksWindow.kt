package com.github.jensim.megamanipulatior.actions.forks

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.actions.vcs.PrRouter
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.CodeHostSelector
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
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
                uiProtectedOperation("") {
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
