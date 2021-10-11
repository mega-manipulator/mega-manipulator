package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CommitDialog
import com.github.jensim.megamanipulator.ui.CreatePullRequestDialog
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.UiProtector
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.layout.panel
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JOptionPane
import javax.swing.ListSelectionModel

private typealias StepResult = Pair<String, ApplyOutput>
private typealias DirResult = Pair<String, List<StepResult>>

@SuppressWarnings("LongParameterList")
class GitWindow(
    private val localRepoOperator: LocalRepoOperator,
    private val settingsFileOperator: SettingsFileOperator,
    private val processOperator: ProcessOperator,
    private val commitOperator: CommitOperator,
    private val dialogGenerator: DialogGenerator,
    private val filesOperator: FilesOperator,
    private val projectOperator: ProjectOperator,
    private val prRouter: PrRouter,
    private val uiProtector: UiProtector,
) : ToolWindowTab {

    private val repoList = JBList<DirResult>().apply {
        minimumSize = Dimension(250, 50)
    }
    private val scrollLeft = JBScrollPane(repoList)
    private val stepList = JBList<StepResult>().apply {
        minimumSize = Dimension(150, 50)
    }
    private val scrollMid = JBScrollPane(stepList)
    private val outComeInfo = JBTextArea().apply {
        minimumSize = Dimension(250, 50)
    }
    private val scrollRight = JBScrollPane(outComeInfo)

    private val splitRight = JBSplitter().apply {
        firstComponent = scrollMid
        secondComponent = scrollRight
    }
    private val splitLeft = JBSplitter().apply {
        firstComponent = scrollLeft
        secondComponent = splitRight
    }

    override val content: JComponent = panel {
        row {
            buttonGroup {
                button("List branches") {
                    refresh()
                }
                button("Set branch") {
                    val branch: String? = JOptionPane.showInputDialog("This will not reset the repos to origin/default-branch first!!\nSelect branch name")
                    if (branch != null && branch.isNotEmpty() || !branch!!.contains(' ')) {
                        localRepoOperator.switchBranch(branch)
                        refresh()
                    }
                }
                component(
                    JButton("Commit & Push").apply {
                        addActionListener { _: ActionEvent ->
                            CommitDialog.openCommitDialog(this) { commitMessage: String, push: Boolean ->
                                val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
                                val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                                var workTitle = "Commiting"
                                if (push) workTitle += " & pushing"
                                val dirs = localRepoOperator.getLocalRepoFiles()
                                uiProtector.mapConcurrentWithProgress(
                                    title = workTitle,
                                    data = dirs,
                                ) { commitOperator.commitProcess(it, result, commitMessage, push, settings) }
                                if (result.isEmpty()) {
                                    result["no result"] =
                                        mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
                                }
                                repoList.setListData(result.toList().toTypedArray())
                            }
                        }
                    }
                )
                component(
                    JButton("Push").apply {
                        addActionListener {
                            dialogGenerator.showConfirm(title = "Push local commits to remote?", focusComponent = this) {
                                val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()

                                val dirs = localRepoOperator.getLocalRepoFiles()
                                val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                                uiProtector.mapConcurrentWithProgress(
                                    title = "Pushing",
                                    data = dirs
                                ) { dir ->
                                    commitOperator.push(settings, dir, result.computeIfAbsent(dir.path) { ArrayList() })
                                }

                                if (result.isEmpty()) {
                                    result["no result"] = mutableListOf("nothing" to ApplyOutput(".", std = "", err = "", exitCode = 1))
                                }
                                repoList.setListData(result.toList().toTypedArray())
                            }
                        }
                    }
                )
                button("Create PRs") {
                    val dialog = CreatePullRequestDialog()
                    if (dialog.showAndGet()) {
                        val prTitle = dialog.prTitle
                        val prDescription = dialog.prDescription
                        if (!prTitle.isNullOrBlank() && !prDescription.isNullOrBlank()) {
                            val repos = localRepoOperator.getLocalRepos()
                            uiProtector.mapConcurrentWithProgress(
                                title = "Creating PRs",
                                extraText1 = prTitle,
                                extraText2 = { it.asPathString() },
                                data = repos
                            ) {
                                prRouter.createPr(prTitle, prDescription, it)
                            }
                        }
                    }
                }
            }
            button("Clean away local repos") {
                dialogGenerator.showConfirm(
                    title = """
                        Are you sure?!
                        This will remove the entire clones dir from disk.
                        No recovery available!
                    """.trimIndent()
                ) {
                    val output: ApplyOutput = projectOperator.project.basePath?.let { dir ->
                        uiProtector.uiProtectedOperation(title = "Remove all local clones") {
                            processOperator.runCommandAsync(File(dir), listOf("rm", "-rf", "clones")).await()
                        }
                    } ?: ApplyOutput(
                        dir = ".",
                        std = "Unable to perform clean operation",
                        err = "Unable to perform clean operation",
                        exitCode = 1
                    )
                    repoList.setListData(arrayOf(Pair("Clean", listOf("rm" to output))))
                    filesOperator.refreshClones()
                }
            }
        }
        row {
            component(splitLeft)
        }
    }

    init {
        repoList.addCellRenderer({ if (it.second.last().second.exitCode != 0) Color.ORANGE else null }) { it.first }
        repoList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        repoList.addListSelectionListener {
            repoList.selectedValue?.let {
                stepList.setListData(it.second.toTypedArray())
                if (it.second.isNotEmpty()) stepList.selectedIndex = 0
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
        val project = projectOperator.project
        val localRepoFiles = localRepoOperator.getLocalRepoFiles()
        val result: List<DirResult> = uiProtector.mapConcurrentWithProgress(
            title = "Listing branches",
            data = localRepoFiles
        ) { dir ->
            listOf(
                "log" to processOperator.runCommandAsync(dir, listOf("git", "log", "--graph", "--pretty=format:'%h -%d %s (%cr) <%an>'", "--abbrev-commit", "--date=relative", "-10")),
                "branches" to processOperator.runCommandAsync(dir, listOf("git", "branch", "-v")),
                "remotes" to processOperator.runCommandAsync(dir, listOf("git", "remote", "-v")),
            ).map { it.first to it.second.await() }
        }.map {
            it.first.trimProjectPath(project = project) to (
                it.second
                    ?: listOf("Operation failed" to ApplyOutput.dummy(dir = it.first.path, err = "Failed git operation"))
                )
        }
        repoList.setListData(result.toTypedArray())
        if (result.isNotEmpty()) {
            repoList.setSelectedValue(result.first(), true)
        }
        content.validate()
        content.repaint()
    }

    override val index: Int = 3
}
