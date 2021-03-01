package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.ui.DialogGenerator.showConfirm
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
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
                    prProvider().mapConcurrentWithProgress(title = "Declining prs") { pullRequest ->
                        PrRouter.closePr(pullRequest)
                    }
                }
            }
        }
        val alterMenuItem = JMenuItem("Alter PRs title and description").apply {
            addActionListener {
                showConfirm("Not yet implemented", "Not yet implemented") {
                    // TODO
                }
            }
        }
        val setReviewersMenuItem = JMenuItem("Update reviewers").apply {
            addActionListener {
                showConfirm("Not yet implemented", "Not yet implemented") {
                    // TODO
                }
            }
        }
        val defaultReviewersMenuItem = JMenuItem("Add default reviewers").apply {
            addActionListener { _ ->
                showConfirm("Not fully implemented", "Not fully implemented") {
                    prProvider().mapConcurrentWithProgress(title = "Add default reviewers") { pr ->
                        val codeHostName = codeHostName
                        val searchHostName = searchHostName
                        if (codeHostName == null || searchHostName == null) {
                            return@mapConcurrentWithProgress
                        }
                        println("Add default reviewers ${pr.project} ${pr.repo} ${pr.title}")
                        val existingReviewers: List<String> = pr.reviewers.map { it.name }
                        val defaultReviewers: List<String> = PrRouter.getDefaultReviewers(pr)

                        val diffMissing = defaultReviewers - existingReviewers
                        if (diffMissing.isNotEmpty()) {
                            // TODO
                            NotificationsOperator.show(
                                title = "Not yet implemented =)",
                                body = "missing:\n${diffMissing.joinToString("\n") { "* $it" }}",
                                type = NotificationType.WARNING
                            )
                        }
                    }
                }
            }
        }

        add(declineMenuItem)
        add(alterMenuItem)
        add(setReviewersMenuItem)
        add(defaultReviewersMenuItem)
    }
}
