package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.ui.DialogGenerator.showConfirm
import com.github.jensim.megamanipulatior.ui.mapConcurrentWithProgress
import com.intellij.notification.NotificationType
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
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
                        extraText2 = { "${it.codeHostName}/${it.project}/${it.repo} ${it.branchFrom}" },
                    ) { pullRequest ->
                        PrRouter.closePr(pullRequest)
                    }
                    postActionHook()
                }
            }
        }
        val alterMenuItem = JMenuItem("Alter PRs title and description").apply {
            addActionListener {
                val prs = prProvider()
                if (prs.isEmpty()) {
                    NotificationsOperator.show("No PRs selected", "Please select at least one PR to use this", NotificationType.WARNING)
                } else {
                    val select: ComboBox<PullRequest> = ComboBox(prs.toTypedArray())
                    val titleField = JBTextField()
                    val bodyField = JBTextArea()
                    val panel = panel {
                        row { label("Template from"); component(select) }
                        row { label("New title"); component(titleField) }
                        row { label("New body");component(bodyField) }
                    }
                    val ans = JOptionPane.showConfirmDialog(null, panel, "Alter PRs", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null)
                    if (ans == JOptionPane.OK_OPTION) {
                        prs.mapConcurrentWithProgress(
                            title = "Reword PRs",
                            extraText1 = "Setting new title and body for Pull requests",
                            extraText2 = { "${it.codeHostName}/${it.project}/${it.repo} ${it.branchFrom}" }
                        ) { pr ->
                            PrRouter.updatePr(pr.copy(title = titleField.text, body = bodyField.text))
                        }
                        postActionHook()
                    }
                }
            }
        }
        val setReviewersMenuItem = JMenuItem("Update reviewers").apply {
            addActionListener {
                showConfirm(
                    title = "Not yet implemented",
                    message = "Clicking OK will only refresh..."
                ) {
                    // TODO
                    postActionHook()
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
                        extraText2 = { "${it.codeHostName}/${it.project}/${it.repo} ${it.branchFrom}" },
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
        add(setReviewersMenuItem)
        add(defaultReviewersMenuItem)
    }
}
