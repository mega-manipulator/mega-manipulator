package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.ui.DialogGenerator.showConfirm
import com.github.jensim.megamanipulator.ui.EditPullRequestDialog
import com.github.jensim.megamanipulator.ui.mapConcurrentWithProgress
import com.intellij.notification.NotificationType
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class PullRequestActionsMenu(
    private val prProvider: () -> List<PullRequest>,
    private val postActionHook: () -> Unit,
) : JPopupMenu() {

    var codeHostName: String? = null
    var searchHostName: String? = null

    init {
        val declineMenuItem = JMenuItem("Decline PRs").apply {
            addActionListener { _ ->
                showConfirm("Decline selected PRs", "No undo path available im afraid..\nDecline selected PRs?") {
                    prProvider().mapConcurrentWithProgress(
                        title = "Declining prs",
                        extraText2 = { "${it.codeHostName()}/${it.project()}/${it.repo()} ${it.fromBranch()}" },
                    ) { pullRequest ->
                        PrRouter.closePr(pullRequest)
                    }
                    postActionHook()
                }
            }
        }
        val alterMenuItem = JMenuItem("Reword PRs").apply {
            addActionListener {
                val prs = prProvider()
                if (prs.isEmpty()) {
                    NotificationsOperator.show("No PRs selected", "Please select at least one PR to use this", NotificationType.WARNING)
                } else {
                    val dialog = EditPullRequestDialog(prs)
                    if (dialog.showAndGet()) {
                        prs.mapConcurrentWithProgress(
                            title = "Reword PRs",
                            extraText1 = "Setting new title and body for Pull requests",
                            extraText2 = { "${it.codeHostName()}/${it.project()}/${it.repo()} ${it.fromBranch()}" }
                        ) { pr ->
                            PrRouter.updatePr(pr.alterCopy(title = dialog.prTitle!!, body = dialog.prDescription!!))
                        }
                        postActionHook()
                    } else {
                        NotificationsOperator.show("No PRs edited", "Cancelled or missing data")
                    }
                }
            }
        }
        val defaultReviewersMenuItem = JMenuItem("Add default reviewers").apply {
            addActionListener { _ ->
                showConfirm(
                    title = "Add default reviewers",
                    message = "Add defult reviewers"
                ) {
                    prProvider().mapConcurrentWithProgress(
                        title = "Add default reviewers",
                        extraText2 = { "${it.codeHostName()}/${it.project()}/${it.repo()} ${it.fromBranch()}" },
                    ) { pr ->
                        val codeHostName = codeHostName
                        val searchHostName = searchHostName
                        if (codeHostName == null || searchHostName == null) {
                            return@mapConcurrentWithProgress
                        }
                        PrRouter.addDefaultReviewers(pr)
                    }
                    postActionHook()
                }
            }
        }

        add(declineMenuItem)
        add(alterMenuItem)
        add(defaultReviewersMenuItem)
    }
}
