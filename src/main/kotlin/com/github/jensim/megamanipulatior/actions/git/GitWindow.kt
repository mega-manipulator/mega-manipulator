package com.github.jensim.megamanipulatior.actions.git

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import com.github.jensim.megamanipulatior.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator.getLocalRepos
import com.github.jensim.megamanipulatior.actions.vcs.PrRouter
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.DialogGenerator
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import java.awt.Color
import java.io.File
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE
import javax.swing.ListSelectionModel
import kotlinx.coroutines.future.asDeferred

object GitWindow : ToolWindowTab {

    private val prTitle = JBTextField()
    private val prDescription = JBTextArea()
    private val prPanel = panel {
        row {
            label("Title")
            component(prTitle)
        }
        row {
            label("Description")
            component(prDescription)
        }
    }
    private val repoList = JBList<Pair<String, ApplyOutput>>()
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
                    uiProtectedOperation(title = "Switching branches") {
                        val branch: String? = JOptionPane.showInputDialog("This will not reset the repos to origin/default-branch first!!\nSelect branch name")
                        if (branch == null || branch.isEmpty() || branch.contains(' ')) {
                            throw IllegalArgumentException("Invalid branch name")
                        }
                        LocalRepoOperator.getLocalRepoFiles().mapConcurrentWithProgress("Che") { dir ->
                            ProcessOperator.runCommand(dir, arrayOf("git", "checkout", "-b", "$branch"))
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
                    uiProtectedOperation(title = "Creating PRs") {
                        val response: Int = JOptionPane.showConfirmDialog(null, prPanel, "Define PR", OK_CANCEL_OPTION, QUESTION_MESSAGE)
                        if (response == OK_OPTION) {
                            getLocalRepos().mapConcurrentWithProgress(title = "Creating PRs") {
                                PrRouter.createPr(prTitle.text, prDescription.text, it)
                            }
                        }
                    }
                }
            }
            button("Clean away local repos") {
                DialogGenerator.showConfirm(title = "Are you sure?!", message = "This will remove the entire clones dir from disk, no recovery available!") {
                    val output: ApplyOutput = project.basePath?.let { dir ->
                        uiProtectedOperation(title = "Remove all local clones") {
                            ProcessOperator.runCommand(File(dir), arrayOf("rm", "-rf", "clones/*"))?.asDeferred()?.await()
                        }
                    } ?: ApplyOutput(dir = ".", std = "Unable to perform clean operation", err = "Unable to perform clean operation", exitCode = 1)
                    repoList.setListData(arrayOf(Pair("RM", output)))
                }
            }
        }
        row {
            component(scrollLeft)
            component(scrollRight)
        }
    }

    init {
        repoList.addCellRenderer({ (_, output) ->
            if (output.exitCode != 0) {
                Color.ORANGE
            } else {
                null
            }
        }) { it.first }
        repoList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoList.addListSelectionListener {
            outComeInfo.text = ""
            repoList.selectedValuesList?.firstOrNull()?.let {
                outComeInfo.text = it.second.getFullDescription()
            }
        }
    }

    override fun refresh() {
        val result: List<Pair<String, ApplyOutput>> = LocalRepoOperator.getLocalRepoFiles().mapConcurrentWithProgress(
            title = "Listing branches"
        ) {
            ProcessOperator.runCommand(it, arrayOf("git", "branch", "-v"))?.asDeferred()?.await()
        }.map { it.first.path to (it.second ?: ApplyOutput.dummy(dir = it.first.path, err = "Failed reading branch")) }
        repoList.setListData(result.toTypedArray())
    }

    override val index: Int = 3
}
