package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.onboarding.OnboardingButton
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.onboarding.OnboardingOperator
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.github.jensim.megamanipulator.toolswindow.TabKey
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.github.jensim.megamanipulator.ui.CommitDialogFactory
import com.github.jensim.megamanipulator.ui.CreatePullRequestDialog
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.github.jensim.megamanipulator.ui.PushDialogFactory
import com.github.jensim.megamanipulator.ui.UiProtector
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign.RIGHT
import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel

private typealias StepResult = Pair<String, ApplyOutput>
private typealias DirResult = Pair<String, List<StepResult>>

@SuppressWarnings("LongParameterList")
class GitWindow(private val project: Project) : ToolWindowTab {

    private val branchNamePattern = "^[a-zA-Z][a-zA-Z0-9/_-]*[a-zA-Z0-9]$"

    private val localRepoOperator: LocalRepoOperator by lazy { project.service() }
    private val settingsFileOperator: SettingsFileOperator by lazy { project.service() }
    private val processOperator: ProcessOperator by lazy { project.service() }
    private val commitOperator: CommitOperator by lazy { project.service() }
    private val filesOperator: FilesOperator by lazy { project.service() }
    private val prRouter: PrRouter by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val onboardingOperator: OnboardingOperator by lazy { project.service() }
    private val prefillStringSuggestionOperator: PrefillStringSuggestionOperator by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }
    private val commitDialogFactory: CommitDialogFactory by lazy { project.service() }
    private val pushDialogFactory: PushDialogFactory by lazy { project.service() }

    private val repoList = JBList<DirResult>().apply {
        minimumSize = Dimension(250, 50)
    }
    private val scrollRepos = JBScrollPane(repoList).apply {
        preferredSize = Dimension(4000, 1000)
    }
    private val stepList = JBList<StepResult>().apply {
        minimumSize = Dimension(150, 50)
    }
    private val scrollSteps = JBScrollPane(stepList).apply {
        preferredSize = Dimension(4000, 1000)
    }
    private val outComeInfo = JBTextArea().apply {
        minimumSize = Dimension(250, 50)
    }
    private val scrollOutcome = JBScrollPane(outComeInfo).apply {
        preferredSize = Dimension(4000, 1000)
    }

    private val splitRight = JBSplitter().apply {
        firstComponent = scrollSteps
        secondComponent = scrollOutcome
    }
    private val splitLeft = JBSplitter().apply {
        firstComponent = scrollRepos
        secondComponent = splitRight
    }

    private val btnListBranch = JButton("List branches")
    private val btnSetBranch = JButton("Set branch")
    private val btnCommitAndPush = JButton("Commit & Push")
    private val btnJustPush = JButton("Push")
    private val btnCreatePRs = JButton("Create PRs")
    private val btnCleanLocalClones = JButton("Clean away local repos", AllIcons.Toolwindows.Problems)

    override val content: JComponent = panel {
        row {
            cell(btnListBranch)
            cell(btnSetBranch)
            cell(btnCommitAndPush)
            cell(btnJustPush)
            cell(btnCreatePRs)
            cell(btnCleanLocalClones)
            cell(OnboardingButton(project, TabKey.tabTitleClones, OnboardingId.CLONES_TAB))
                .horizontalAlign(RIGHT)
        }
        row {
            cell(splitLeft)
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

        btnListBranch.addActionListener {
            refresh()
        }
        btnSetBranch.addActionListener {
            dialogGenerator.askForInput(
                title = "Select branch name",
                message = "This will NOT reset the repos to origin/default-branch first!!",
                validationPattern = branchNamePattern,
                prefill = PrefillString.BRANCH,
                focusComponent = btnSetBranch,
            ) { branch: String ->
                if (branch.isBlank() || branch.isEmpty() || !branch.matches(Regex(branchNamePattern))) {
                    dialogGenerator.showConfirm(
                        title = "Bad branch name.",
                        message = "$branch didnt match pattern $branchNamePattern",
                        yesText = "Ok",
                        noText = "Cancel",
                        focusComponent = btnSetBranch
                    ) {}
                } else {
                    localRepoOperator.switchBranch(branch)
                    prefillStringSuggestionOperator.addPrefill(PrefillString.BRANCH, branch)
                    refresh()
                }
            }
        }
        btnCommitAndPush.addActionListener { _: ActionEvent ->
            commitDialogFactory.openCommitDialog(
                focusComponent = btnCommitAndPush,
            ) { commitMessage: String, push: Boolean, force: Boolean ->
                val result = ConcurrentHashMap<String, MutableList<Pair<String, ApplyOutput>>>()
                val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                var workTitle = "Commiting"
                if (push) workTitle += " & pushing"
                val dirs = localRepoOperator.getLocalRepoFiles()
                uiProtector.mapConcurrentWithProgress(
                    title = workTitle,
                    data = dirs,
                ) { commitOperator.commitProcess(it, result, commitMessage, push, force, settings) }
                if (result.isEmpty()) {
                    result["no result"] = mutableListOf("nothing" to ApplyOutput.dummy())
                }
                repoList.setListData(result.toList().toTypedArray())
            }
        }
        btnJustPush.addActionListener {
            pushDialogFactory.openPushDialog(
                focusComponent = btnJustPush,
            ) { force ->
                val result = mutableListOf<Pair<String, List<Pair<String, ApplyOutput>>>>()

                val dirs = localRepoOperator.getLocalRepoFiles()
                val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                result += uiProtector.mapConcurrentWithProgress(
                    title = "Pushing",
                    data = dirs
                ) { dir ->
                    commitOperator.push(settings, dir, force)
                }.map { it.first.path to it.second.orEmpty() }

                if (result.isEmpty()) {
                    result += "no result" to mutableListOf("nothing" to ApplyOutput.dummy())
                }
                repoList.setListData(result.toList().toTypedArray())
            }
        }
        btnCreatePRs.addActionListener { _ ->
            CreatePullRequestDialog(
                project = project
            ).show(
                focusComponent = btnCreatePRs,
            ) { title, description ->
                if (title.isNotBlank() && description.isNotBlank()) {
                    val repos = localRepoOperator.getLocalRepos()
                    uiProtector.mapConcurrentWithProgress(
                        title = "Creating PRs",
                        extraText1 = title,
                        extraText2 = { it.asPathString() },
                        data = repos
                    ) {
                        prRouter.createPr(title, description, it)
                    }
                } else {
                    dialogGenerator.showConfirm(
                        title = "Failed",
                        message = "Title and description must not be blank",
                        focusComponent = btnCreatePRs,
                        yesText = "Ok",
                        noText = "Cancel",
                    ) {}
                }
            }
        }
        btnCleanLocalClones.addActionListener { _ ->
            dialogGenerator.showConfirm(
                title = "Clean local repos",
                message = """
                            Are you sure?!
                            This will remove the entire clones dir from disk.
                            No recovery available!
                """.trimIndent(),
                focusComponent = btnCleanLocalClones,
            ) {
                val output: ApplyOutput = project.basePath?.let { dir ->
                    uiProtector.uiProtectedOperation(title = "Remove all local clones") {
                        processOperator.runCommandAsync(File(dir), listOf("rm", "-rf", "clones")).await()
                    }
                } ?: ApplyOutput.dummy(std = "Unable to perform clean operation")
                repoList.setListData(arrayOf(Pair("Clean", listOf("rm" to output))))
                repoList.selectedIndex = 0
                filesOperator.refreshClones()
            }
        }
    }

    override fun refresh() {
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
                    ?: listOf("Operation failed" to ApplyOutput.dummy(dir = it.first.path, std = "Failed git operation"))
                )
        }
        if (result.isNotEmpty()) {
            repoList.setListData(result.toTypedArray())
            repoList.setSelectedValue(result.first(), true)
        }
        content.validate()
        content.repaint()

        onboardingOperator.registerTarget(OnboardingId.CLONES_STEP_OUTPUT, scrollOutcome)
        onboardingOperator.registerTarget(OnboardingId.CLONES_LIST_STEPS, scrollSteps)
        onboardingOperator.registerTarget(OnboardingId.CLONES_LIST_REPOS, scrollRepos)
        onboardingOperator.registerTarget(OnboardingId.CLONES_CLEAN_REPOS, btnCleanLocalClones)
        onboardingOperator.registerTarget(OnboardingId.CLONES_PR_BUTTON, btnCreatePRs)
        onboardingOperator.registerTarget(OnboardingId.CLONES_PUSH_BUTTON, btnJustPush)
        onboardingOperator.registerTarget(OnboardingId.CLONES_COMMIT_PUSH_BUTTON, btnCommitAndPush)
        onboardingOperator.registerTarget(OnboardingId.CLONES_LIST_BRANCH, btnListBranch)
        onboardingOperator.registerTarget(OnboardingId.CLONES_TAB, content)
    }
}
