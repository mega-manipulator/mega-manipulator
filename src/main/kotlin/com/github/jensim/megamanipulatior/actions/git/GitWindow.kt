package com.github.jensim.megamanipulatior.actions.git

import com.github.jensim.megamanipulatior.actions.ProcessOperator
import com.github.jensim.megamanipulatior.actions.apply.ApplyOutput
import com.github.jensim.megamanipulatior.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulatior.actions.localrepo.LocalRepoOperator.getLocalRepos
import com.github.jensim.megamanipulatior.actions.vcs.PrRouter
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.github.jensim.megamanipulatior.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulatior.ui.CreatePullRequestDialog
import com.github.jensim.megamanipulatior.ui.DialogGenerator
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import com.github.jensim.megamanipulatior.ui.trimProjectPath
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Color
import java.io.File
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.ListSelectionModel

private typealias StepResult = Pair<String, ApplyOutput>
private typealias DirResult = Pair<String, List<StepResult>>

object GitWindow : ToolWindowTab {

    private val repoList = JBList<DirResult>()
    private val scrollLeft = JBScrollPane(repoList)
    private val stepList = JBList<StepResult>()
    private val scrollMid = JBScrollPane(stepList)
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
                            ProcessOperator.runCommandAsync(dir, listOf("git", "checkout", "-b", "$branch"))
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
                    val dialog = CreatePullRequestDialog()
                    if (dialog.showAndGet()) {
                        val prTitle = dialog.prTitle
                        val prDescription = dialog.prDescription
                        if (!prTitle.isNullOrBlank() && !prDescription.isNullOrBlank()) {
                            getLocalRepos().mapConcurrentWithProgress(
                                title = "Creating PRs",
                                extraText1 = prTitle,
                                extraText2 = { it.asPathString() }
                            ) {
                                PrRouter.createPr(prTitle, prDescription, it)
                            }
                        }
                    }
                }
            }
            button("Clean away local repos") {
                DialogGenerator.showConfirm(title = "Are you sure?!", message = "This will remove the entire clones dir from disk, no recovery available!") {
                    val output: ApplyOutput = project.basePath?.let { dir ->
                        uiProtectedOperation(title = "Remove all local clones") {
                            ProcessOperator.runCommandAsync(File(dir), listOf("rm", "-rf", "clones")).await()
                        }
                    } ?: ApplyOutput(dir = ".", std = "Unable to perform clean operation", err = "Unable to perform clean operation", exitCode = 1)
                    repoList.setListData(arrayOf(Pair("Clean", listOf("rm" to output))))
                }
            }
        }
        row {
            component(scrollLeft)
            component(scrollMid)
            component(scrollRight)
        }
    }

    init {
        repoList.addCellRenderer({ if (it.second.any { it.second.exitCode != 0 }) Color.ORANGE else null }) { it.first }
        repoList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoList.addListSelectionListener {
            repoList.selectedValue?.let {
                stepList.setListData(it.second.toTypedArray())
            }
        }
        stepList.addCellRenderer({ if (it.second.exitCode != 0) Color.ORANGE else null }) { it.first }
        stepList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        stepList.addListSelectionListener {
            outComeInfo.text = ""
            stepList.selectedValuesList?.firstOrNull()?.let {
                outComeInfo.text = it.second.getFullDescription()
            }
        }
    }

    override fun refresh() {
        val result: List<DirResult> = LocalRepoOperator.getLocalRepoFiles().mapConcurrentWithProgress(
            title = "Listing branches"
        ) {
            ProcessOperator.runCommandAsync(it, listOf("git", "branch", "-v")).await()
        }.map { it.first.trimProjectPath() to listOf("list branches" to (it.second ?: ApplyOutput.dummy(dir = it.first.path, err = "Failed reading branch"))) }
        repoList.setListData(result.toTypedArray())
    }

    override val index: Int = 3
}
