package com.github.jensim.megamanipulatior.actions.git

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import com.github.jensim.megamanipulatior.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.DialogGenerator
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.io.File
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.ListSelectionModel

object GitWindow : ToolWindowTab {

    private val repoList = JBList<Pair<String, String>>()
    private val scrollLeft = JBScrollPane(repoList)
    private val outComeInfo = JBTextArea()
    private val scrollRight = JBScrollPane(outComeInfo)

    override val content: JComponent = panel {
        row {
            buttonGroup {
                button("List branches") {
                    refresh()
                }
                button("Set branch") {
                    uiProtectedOperation(onFailMsg = { "Failed switching branches" }) {
                        val branch: String? = JOptionPane.showInputDialog("This will not reset the repos to origin/default-branch first!!\nSelect branch name")
                        if (branch == null || branch.isEmpty() || branch.contains(' ')) {
                            throw IllegalArgumentException("Invalid branch name")
                        }
                        LocalRepoOperator.getLocalRepoFiles().forEach { dir ->
                            ProcessOperator.runCommand(dir, "git checkout -b $branch")
                        }
                        refresh()
                    }
                }
                button("Commit and Push") {
                    repoList.setListData(CommitOperator.commit().toList().toTypedArray())
                }
                button("Push") {
                    repoList.setListData(CommitOperator.push().toList().toTypedArray())
                }
                button("Create PRs") {
                    // TODO
                    DialogGenerator.showConfirm("TODO", "Not yet implemented") {}
                }
            }
            button("Clean away local repos") {
                DialogGenerator.showConfirm("Are you sure?!", "This will remove the entire clones dir from disk, no recovery available!") {
                    val output: ApplyOutput = project.basePath?.let { dir ->
                        ProcessOperator.runCommand(File(dir), "rm -rf clones")?.get(20, TimeUnit.SECONDS)
                    } ?: ApplyOutput("....", "Unable to perform clean operation", "Unable to perform clean operation", 1)
                    repoList.setListData(arrayOf(Pair("RM", output.getFullDescription())))
                }
            }
        }
        row {
            component(scrollLeft)
            component(scrollRight)
        }
    }

    init {
        repoList.addCellRenderer { it.first }
        repoList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoList.addListSelectionListener {
            outComeInfo.text = ""
            repoList.selectedValuesList?.firstOrNull()?.let {
                outComeInfo.text = it.second
            }
        }
    }

    override fun refresh() {
        uiProtectedOperation(onFailMsg = { "Failed listing branches" }) {
            repoList.setListData(
                LocalRepoOperator.getLocalRepoFiles()
                    .map { Pair(it.path, ProcessOperator.runCommand(it, "git branch -v")?.get()?.getFullDescription() ?: "Failed") }
                    .toTypedArray())
        }
    }

    override val index: Int = 3
}
