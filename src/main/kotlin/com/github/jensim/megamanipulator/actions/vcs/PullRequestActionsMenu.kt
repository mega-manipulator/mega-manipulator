package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.EditPullRequestDialog
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JOptionPane.CANCEL_OPTION
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE
import javax.swing.JPopupMenu

@SuppressWarnings("LongParameterList")
class PullRequestActionsMenu(
    private val prProvider: () -> List<PullRequestWrapper>,
    private val postActionHook: () -> Unit,
    private val prRouter: PrRouter,
    private val notificationsOperator: NotificationsOperator,
    private val dialogGenerator: DialogGenerator,
    private val cloneOperator: CloneOperator,
    private val filesOperator: FilesOperator,
    private val uiProtector: UiProtector,
) : JPopupMenu() {

    companion object {

        fun instance(
            prProvider: () -> List<PullRequestWrapper>,
            postActionHook: () -> Unit,
        ) = PullRequestActionsMenu(
            prProvider = prProvider,
            postActionHook = postActionHook,
            prRouter = PrRouter.instance,
            notificationsOperator = NotificationsOperator.instance,
            dialogGenerator = DialogGenerator.instance,
            cloneOperator = CloneOperator.instance,
            filesOperator = FilesOperator.instance,
            uiProtector = UiProtector.instance,
        )
    }

    var codeHostName: String? = null
    var searchHostName: String? = null

    init {
        val declineMenuItem = JMenuItem("Decline PRs").apply {
            addActionListener { _ ->
                dialogGenerator.showConfirm("Decline selected PRs", "No undo path available im afraid..\nDecline selected PRs?") {
                    val ans = JOptionPane.showConfirmDialog(null, "Also drop source branches and forks?", "Drop source?", OK_CANCEL_OPTION, QUESTION_MESSAGE, null)
                    val doDrop = ans == OK_OPTION
                    val exit = !listOf(OK_OPTION, CANCEL_OPTION).contains(ans)
                    if (!exit) {
                        uiProtector.mapConcurrentWithProgress(
                            title = "Declining prs",
                            extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                            data = prProvider(),
                        ) { pullRequest: PullRequestWrapper ->
                            prRouter.closePr(doDrop, pullRequest)
                        }
                    }
                    postActionHook()
                }
            }
        }
        val alterMenuItem = JMenuItem("Reword PRs").apply {
            addActionListener {
                val prs = prProvider()
                if (prs.isEmpty()) {
                    notificationsOperator.show("No PRs selected", "Please select at least one PR to use this", NotificationType.WARNING)
                } else {
                    val dialog = EditPullRequestDialog(prs)
                    if (dialog.showAndGet()) {
                        uiProtector.mapConcurrentWithProgress(
                            title = "Reword PRs",
                            extraText1 = "Setting new title and body for Pull requests",
                            extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                            data = prs,
                        ) { pr ->
                            prRouter.updatePr(dialog.prTitle!!, dialog.prDescription!!, pr)
                        }
                        postActionHook()
                    } else {
                        notificationsOperator.show("No PRs edited", "Cancelled or missing data")
                    }
                }
            }
        }
        val defaultReviewersMenuItem = JMenuItem("Add default reviewers").apply {
            addActionListener { _ ->
                dialogGenerator.showConfirm(
                    title = "Add default reviewers",
                    message = "Add default reviewers"
                ) {
                    uiProtector.mapConcurrentWithProgress(
                        title = "Add default reviewers",
                        extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                        data = prProvider(),
                    ) { pr ->
                        val codeHostName = codeHostName
                        val searchHostName = searchHostName
                        if (codeHostName == null || searchHostName == null) {
                            return@mapConcurrentWithProgress
                        }
                        prRouter.addDefaultReviewers(pr)
                    }
                    postActionHook()
                }
            }
        }
        val cloneMenuItem = JMenuItem("Clone PRs").apply {
            addActionListener {
                val prs = prProvider()
                dialogGenerator.showConfirm(title = "Clone...", message = "Clone ${prs.size} selected PR branches") {
                    uiProtector.mapConcurrentWithProgress(
                        title = "Clone PRs",
                        data = prs,
                    ) { pullRequest ->
                        cloneOperator.clone(pullRequest)
                    }
                    filesOperator.refreshClones()
                }
            }
        }

        add(declineMenuItem)
        add(alterMenuItem)
        add(defaultReviewersMenuItem)
        add(cloneMenuItem)
    }
}
