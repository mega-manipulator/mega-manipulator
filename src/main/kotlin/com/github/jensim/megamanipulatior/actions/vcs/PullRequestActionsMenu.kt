package com.github.jensim.megamanipulatior.actions.vcs

import com.github.jensim.megamanipulatior.actions.search.SearchResult
import com.github.jensim.megamanipulatior.ui.DialogGenerator.showConfirm
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class PullRequestActionsMenu(
    private val searchHostName: String,
    private val codeHostName: String,
    private val prProvider: () -> List<PullRequest>
) : JPopupMenu() {

    init {
        val declineMenuItem = JMenuItem("Decline PRs").apply {
            addActionListener {
                prProvider().forEach {
                    // TODO
                    println("Decline ${it.project} ${it.repo} ${it.title}")
                    showConfirm("Not yet implemented", "Not yet implemented") {}
                }
            }
        }
        val alterMenuItem = JMenuItem("Alter PRs title and descripton").apply {
            addActionListener {
                prProvider().forEach {
                    // TODO
                    println("Alter ${it.project} ${it.repo} ${it.title}")
                    showConfirm("Not yet implemented", "Not yet implemented") {}
                }
            }
        }
        val setReviewersMenuItem = JMenuItem("Update reviewers").apply {
            addActionListener {
                prProvider().forEach {
                    // TODO
                    println("Update reviewers ${it.project} ${it.repo} ${it.title}")
                    showConfirm("Not yet implemented", "Not yet implemented") {}
                }
            }
        }
        val defaultReviewersMenuItem = JMenuItem("Add default reviewers").apply {
            addActionListener { _ ->
                prProvider().forEach { pr ->
                    val repo = SearchResult(pr.project, pr.repo, codeHostName, searchHostName)
                    println("Add default reviewers ${pr.project} ${pr.repo} ${pr.title}")
                    val existingReviewers: List<String> = pr.reviewers.map { it.name }
                    val defaultReviewers: List<String> = PrRouter.getDefaultReviewers(repo)

                    // TODO
                    val diffExtra = existingReviewers - defaultReviewers
                    val diffMissing = defaultReviewers - existingReviewers
                    println("${pr}\nextra:${diffExtra} missing:${diffMissing}")
                    showConfirm("Not yet implemented =)", "${pr}\nextra:$diffExtra missing:$diffMissing") {}
                }
            }
        }

        add(declineMenuItem)
        add(alterMenuItem)
        add(setReviewersMenuItem)
        add(defaultReviewersMenuItem)
    }
}
