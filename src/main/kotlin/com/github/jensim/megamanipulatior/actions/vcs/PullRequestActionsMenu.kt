package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.settings.SerializationHolder
import com.github.jensim.megamanipulatior.ui.DialogGenerator.showConfirm
import com.github.jensim.megamanipulatior.ui.uiProtectedOperation
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
            addActionListener {
                showConfirm("Decline selected PRs", "No undo path available im afraid..\nDecline selected PRs?") {
                    prProvider().forEach {
                        // TODO
                        uiProtectedOperation(onFailMsg = { "Failed declining pr\n${SerializationHolder.yamlObjectMapper.writeValueAsString(it)}" }, action = {
                            PrRouter.closePr(it)
                            println("Declined ${it.project} ${it.repo} ${it.title}")
                        })
                    }
                }
            }
        }
        val alterMenuItem = JMenuItem("Alter PRs title and descripton").apply {
            addActionListener {
                showConfirm("Not yet implemented", "Not yet implemented") {
                    prProvider().forEach {
                        // TODO
                        println("Alter ${it.project} ${it.repo} ${it.title}")
                    }
                }
            }
        }
        val setReviewersMenuItem = JMenuItem("Update reviewers").apply {
            addActionListener {
                showConfirm("Not yet implemented", "Not yet implemented") {
                    prProvider().forEach {
                        // TODO
                        println("Update reviewers ${it.project} ${it.repo} ${it.title}")
                    }
                }
            }
        }
        val defaultReviewersMenuItem = JMenuItem("Add default reviewers").apply {
            addActionListener { _ ->
                showConfirm("Not fully implemented", "Not fully implemented") {
                    prProvider().forEach { pr ->
                        val codeHostName = codeHostName
                        val searchHostName = searchHostName
                        if (codeHostName == null || searchHostName == null) {
                            return@forEach
                        }
                        println("Add default reviewers ${pr.project} ${pr.repo} ${pr.title}")
                        val existingReviewers: List<String> = pr.reviewers.map { it.name }
                        val defaultReviewers: List<String> = PrRouter.getDefaultReviewers(pr)

                        val diffMissing = defaultReviewers - existingReviewers
                        if (diffMissing.isNotEmpty()) {
                            // TODO
                            showConfirm(
                                "Not yet implemented =)",
                                "missing:\n${diffMissing.joinToString("\n") { "* $it" }}"
                            ) {}
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
