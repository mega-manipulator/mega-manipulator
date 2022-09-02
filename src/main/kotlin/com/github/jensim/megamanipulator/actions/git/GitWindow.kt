package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.apply.StepResult
import com.github.jensim.megamanipulator.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulator.actions.git.localrepo.LocalRepoOperator
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
import com.github.jensim.megamanipulator.ui.GeneralKtDataTable
import com.github.jensim.megamanipulator.ui.PushDialogFactory
import com.github.jensim.megamanipulator.ui.TableMenu
import com.github.jensim.megamanipulator.ui.TableMenu.MenuItem
import com.github.jensim.megamanipulator.ui.UiProtector
import com.github.jensim.megamanipulator.ui.trimProjectPath
import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign.RIGHT
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Dimension
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

private data class DirResult(val dir: String, val steps: List<StepResult>)

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

    private val repoList = GeneralKtDataTable(
        type = DirResult::class,
        columns = listOf(
            "Repo" to { it.dir }
        ),
        selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
    ) {
        it.steps.any { it.result.exitCode != 0 }
    }
    private val repoMenu = TableMenu<List<DirResult>?>(
        repoList,
        listOf(
            MenuItem("List branches") {
                if (it == null) {
                    refresh()
                } else {
                    refresh(it.map { File(project.basePath, it.dir) })
                }
            },
            MenuItem({ "Set branch (${it?.size ?: 0})" }, isEnabled = { !it.isNullOrEmpty() }) { selected ->
                val repos = selected?.map { File(project.basePath, it.dir) } ?: return@MenuItem
                dialogGenerator.askForInput(
                    title = "Select branch name",
                    message = "This will NOT reset the repos to origin/default-branch first!!",
                    validationPattern = branchNamePattern,
                    prefill = PrefillString.BRANCH,
                    focusComponent = repoList,
                ) { branch: String ->
                    if (branch.isBlank() || branch.isEmpty() || !branch.matches(Regex(branchNamePattern))) {
                        dialogGenerator.showConfirm(
                            title = "Bad branch name.",
                            message = "$branch didnt match pattern $branchNamePattern",
                            yesText = "Ok",
                            noText = "Cancel",
                            focusComponent = repoList,
                        ) {}
                    } else {
                        localRepoOperator.switchBranch(repos, branch)
                        prefillStringSuggestionOperator.addPrefill(PrefillString.BRANCH, branch)
                        refresh()
                    }
                }
            },
            MenuItem({ "Commit & Push (${it?.size ?: 0})" }, isEnabled = { !it.isNullOrEmpty() }) {
                val dirs = it?.map { File(project.basePath, it.dir) } ?: return@MenuItem
                commitDialogFactory.openCommitDialog(
                    focusComponent = repoList,
                ) { commitMessage: String, push: Boolean, force: Boolean ->
                    val result = ConcurrentHashMap<String, MutableList<StepResult>>()
                    val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                    var workTitle = "Commiting"
                    if (push) workTitle += " & pushing"
                    uiProtector.mapConcurrentWithProgress(
                        title = workTitle,
                        data = dirs,
                    ) { commitOperator.commitProcess(it, result, commitMessage, push, force, settings) }
                    if (result.isEmpty()) {
                        result["no result"] = mutableListOf(StepResult("nothing", ApplyOutput.dummy()))
                    }
                    repoList.setListData(result.map { DirResult(it.key, it.value) })
                }
            },
            MenuItem({ "Push (${it?.size ?: 0})" }, isEnabled = { !it.isNullOrEmpty() }) {
                val dirs = it?.map { File(project.basePath, it.dir) } ?: return@MenuItem
                pushDialogFactory.openPushDialog(
                    focusComponent = repoList,
                ) { force ->
                    val result = mutableListOf<DirResult>()

                    val settings: MegaManipulatorSettings = settingsFileOperator.readSettings()!!
                    result += uiProtector.mapConcurrentWithProgress(
                        title = "Pushing",
                        data = dirs
                    ) { dir ->
                        commitOperator.push(settings, dir, force)
                    }.map { DirResult(it.first.path, it.second.orEmpty()) }

                    if (result.isEmpty()) {
                        result += DirResult("no result", listOf(StepResult("nothing", ApplyOutput.dummy())))
                    }
                    repoList.setListData(result.toList())
                }
            },
            MenuItem({ "Create PRs (${it?.size ?: 0})" }, isEnabled = { !it.isNullOrEmpty() }) {
                val dirs = it?.map { File(project.basePath, it.dir) } ?: return@MenuItem
                CreatePullRequestDialog(
                    project = project
                ).show(
                    focusComponent = repoList,
                ) { title, description ->
                    if (title.isNotBlank() && description.isNotBlank()) {
                        val repos = localRepoOperator.getLocalRepos(dirs)
                        uiProtector.mapConcurrentWithProgress(
                            title = "Creating PRs",
                            extraText1 = title,
                            extraText2 = { it.asPathString() },
                            data = repos
                        ) { repo ->
                            prRouter.createPr(title, description, repo)
                        }
                    } else {
                        dialogGenerator.showConfirm(
                            title = "Failed",
                            message = "Title and description must not be blank",
                            focusComponent = repoList,
                            yesText = "Ok",
                            noText = "Cancel",
                        ) {}
                    }
                }
            },
        )
    )
    private val scrollRepos = JBScrollPane(repoList)
    private val stepList = GeneralKtDataTable(
        type = StepResult::class,
        selectionMode = ListSelectionModel.SINGLE_SELECTION,
        columns = listOf(
            "Step" to { it.step },
            "Output (last line)" to { it.result.lastLine },
        )
    ) {
        it.result.exitCode != 0
    }
    private val scrollSteps = JBScrollPane(stepList)
    private val outComeInfo = JBTextArea().apply {
        minimumSize = Dimension(250, 50)
    }
    private val scrollOutcome = JBScrollPane(outComeInfo)

    private val splitRight = JBSplitter().apply {
        firstComponent = scrollSteps
        secondComponent = scrollOutcome
    }
    private val splitLeft = JBSplitter().apply {
        firstComponent = scrollRepos
        secondComponent = splitRight
    }

    private val btnCleanLocalClones = JButton("Clean away local repos", AllIcons.Toolwindows.Problems)

    private val topContent: JComponent = panel {
        row {
            cell(btnCleanLocalClones)
            cell(OnboardingButton(project, TabKey.tabTitleClones, OnboardingId.CLONES_TAB))
                .horizontalAlign(RIGHT)
        }
    }
    override val content: JComponent = BorderLayoutPanel().apply {
        addToTop(topContent)
        addToCenter(splitLeft)
    }

    init {
        repoList.apply {
            addListSelectionListener {
                selectedValuesList.firstOrNull()?.let {
                    stepList.setListData(it.steps)
                    stepList.selectFirst()
                }
            }
            addClickListener { mouseEvent, _ ->
                if (SwingUtilities.isRightMouseButton(mouseEvent)) {
                    repoMenu.show(mouseEvent, repoList.selectedValuesList)
                }
            }
        }
        stepList.addListSelectionListener {
            outComeInfo.text = ""
            stepList.selectedValuesList.firstOrNull()?.let {
                outComeInfo.text = it.result.getFullDescription()
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
                repoList.setListData(listOf(DirResult("Clean", listOf(StepResult("rm", output)))))
                repoList.selectFirst()
                filesOperator.refreshClones()
            }
        }
    }

    override fun refresh() {
        val localRepoFiles: List<File> = localRepoOperator.getLocalRepoFiles()
        refresh(localRepoFiles)
    }

    private fun refresh(localRepoFiles: List<File>) {
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
            DirResult(
                it.first.trimProjectPath(project = project),
                it.second?.map { StepResult(it.first, it.second) }
                    ?: listOf(
                        StepResult("Operation failed", ApplyOutput.dummy(dir = it.first.path, std = "Failed git operation"))
                    )
            )
        }
        if (result.isNotEmpty()) {
            repoList.setListData(result)
            repoList.selectFirst()
        }
        content.validate()
        content.repaint()

        onboardingOperator.registerTarget(OnboardingId.CLONES_STEP_OUTPUT, scrollOutcome)
        onboardingOperator.registerTarget(OnboardingId.CLONES_LIST_STEPS, scrollSteps)
        onboardingOperator.registerTarget(OnboardingId.CLONES_LIST_REPOS, scrollRepos)
        onboardingOperator.registerTarget(OnboardingId.CLONES_CLEAN_REPOS, btnCleanLocalClones)
        onboardingOperator.registerTarget(OnboardingId.CLONES_TAB, content)
        onboardingOperator.registerTarget(OnboardingId.CLONES_PR_ACTION, repoList)
        onboardingOperator.registerTarget(OnboardingId.CLONES_PUSH_ACTION, repoList)
        onboardingOperator.registerTarget(OnboardingId.CLONES_COMMIT_PUSH_ACTION, repoList)
        onboardingOperator.registerTarget(OnboardingId.CLONES_LIST_BRANCH, repoList)
    }
}
