package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.RemoteCloneOperator
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.ui.CloneDialogFactory
import com.github.jensim.megamanipulator.ui.ClosePRDialogFactory
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.EditPullRequestDialog
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.components.JBTextArea
import org.slf4j.LoggerFactory
import java.awt.Desktop
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

@SuppressWarnings("LongParameterList", "ConstructorParameterNaming")
class PullRequestActionsMenu(
    project: Project,
    private val focusComponent: JComponent,
    private val prProvider: () -> List<PullRequestWrapper>,
) : JPopupMenu() {

    private val prRouter: PrRouter by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val remoteCloneOperator: RemoteCloneOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val cloneDialogFactory: CloneDialogFactory by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        val declineMenuItem = JMenuItem("Decline PRs").apply {
            addActionListener { _ ->
                ClosePRDialogFactory.openCommitDialog(relativeComponent = focusComponent) { removeBranches, removeStaleForks ->
                    uiProtector.mapConcurrentWithProgress(
                        title = "Declining prs",
                        extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                        data = prProvider(),
                    ) { pullRequest: PullRequestWrapper ->
                        prRouter.closePr(
                            dropFork = removeStaleForks,
                            dropBranch = removeBranches,
                            pullRequest,
                        )
                    }
                }
            }
        }
        val alterMenuItem = JMenuItem("Reword PRs").apply {
            addActionListener {
                val prs = prProvider()
                if (prs.isEmpty()) {
                    notificationsOperator.show(
                        "No PRs selected",
                        "Please select at least one PR to use this",
                        NotificationType.WARNING
                    )
                } else {
                    EditPullRequestDialog(
                        pullRequests = prs,
                        project = project,
                    ).show(focusComponent) { title, description ->

                        prFeedback(
                            "rewordPRs",
                            uiProtector.mapConcurrentWithProgress(
                                title = "Reword PRs",
                                extraText1 = "Setting new title and body for Pull requests",
                                extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                                data = prs,
                            ) { pr ->
                                prRouter.updatePr(title, description, pr)
                            }
                        )
                    }
                }
            }
        }
        val defaultReviewersMenuItem = JMenuItem("Add default reviewers").apply {
            addActionListener { _ ->
                dialogGenerator.showConfirm(
                    title = "Add default reviewers",
                    message = "Add default reviewers",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "setDefaultReviewers",
                        uiProtector.mapConcurrentWithProgress(
                            title = "Add default reviewers",
                            extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                            data = prProvider(),
                        ) { pr ->
                            prRouter.addDefaultReviewers(pr)
                        }
                    )
                }
            }
        }
        val cloneMenuItem = JMenuItem("Clone PRs").apply {
            addActionListener {
                val prs = prProvider()
                cloneDialogFactory.showCloneFromPrDialog(focusComponent) { sparseDef ->
                    uiProtector.uiProtectedOperation("Clone from PRs") {
                        remoteCloneOperator.clone(prs, sparseDef = sparseDef)
                    }
                }
            }
        }
        val openInBrowserMenuItem = JMenuItem("Open in browser").apply {
            addActionListener {
                val failed = mutableMapOf<PullRequestWrapper, String>()
                val prs = prProvider()
                prs.forEach { prWrapper ->
                    val browseUrl = prWrapper.browseUrl()
                    if (browseUrl == null) {
                        failed[prWrapper] = "Missing BrowseURL"
                    } else {
                        try {
                            com.intellij.ide.BrowserUtil.browse(browseUrl)
                        } catch (e: Exception) {
                            failed[prWrapper] = "Exception opening link ${e.javaClass.name} ${e.message}"
                        }
                    }
                }
                if (failed.isNotEmpty()) {
                    val failMsg = failed.map { (k, v) -> "${k.project()}/${k.baseRepo()} ${k.title().take(10)} :: $v" }
                        .joinToString("\n")
                    notificationsOperator.show(
                        title = "Failed opening ${failed.size}/${prs.size} pull requests",
                        body = failMsg,
                        type = NotificationType.ERROR
                    )
                }
            }
        }

        val commentMenuItem = JMenuItem("Add comment").apply {
            addActionListener {
                val prs = prProvider()
                if (prs.isNotEmpty()) {
                    dialogGenerator.askForInput(
                        title = "Comment selected pull requests",
                        message = "Comment",
                        field = JBTextArea(8, 80),
                        focusComponent = focusComponent,
                        position = Balloon.Position.atLeft,
                        prefill = PrefillString.COMMENT
                    ) { comment ->
                        uiProtector.mapConcurrentWithProgress(
                            title = "Add comments",
                            data = prs
                        ) {
                            prRouter.commentPR(comment, it)
                        }
                    }
                }
            }
        }
        val approveMenuItem = JMenuItem("Mark Approved").apply {
            addActionListener { _ ->
                dialogGenerator.showConfirm(
                    title = "Mark Approved",
                    message = "Mark the selected pull requests as Approved",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "setStatus(approved)",
                        uiProtector.mapConcurrentWithProgress(title = "Mark Approved", data = prProvider()) {
                            prRouter.approvePr(it)
                        }
                    )
                }
            }
        }
        val needsWorkMenuItem = JMenuItem("Mark Needs work").apply {
            addActionListener {
                dialogGenerator.showConfirm(
                    title = "Mark Needs work",
                    message = "Mark the selected pull requests as Needs work",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "setStatus(needsWork)",
                        uiProtector.mapConcurrentWithProgress(title = "Mark Needs work", data = prProvider()) {
                            prRouter.disapprovePr(it)
                        }
                    )
                }
            }
        }
        val mergeMenuItem = JMenuItem("Merge").apply {
            addActionListener {
                dialogGenerator.showConfirm(
                    title = "Merge",
                    message = "Merge the selected pull requests",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "merge",
                        uiProtector.mapConcurrentWithProgress(title = "Merge", data = prProvider()) {
                            prRouter.mergePr(it)
                        }
                    )
                }
            }
        }

        add(declineMenuItem)
        add(alterMenuItem)
        add(defaultReviewersMenuItem)
        add(cloneMenuItem)
        if (isBrowsingAllowed()) {
            add(openInBrowserMenuItem)
        }
        add(commentMenuItem)
        add(approveMenuItem)
        add(needsWorkMenuItem)
        add(mergeMenuItem)
    }

    private fun prFeedback(action: String, list: List<Pair<PullRequestWrapper, PrActionStatus?>>) {
        val failed = list.filter { it.second?.success != true }.map {
            it.first.asPathString() to (it.second?.msg ?: "<NO_INFO>")
        }
        if (failed.isNotEmpty()) {
            log.error("${failed.size}/${list.size} failed. ${failed.joinToString()}")
            notificationsOperator.show(
                title = "${failed.size}/${list.size} $action failed",
                body = "Check log for more info",
                type = NotificationType.WARNING
            )
        }
    }

    private fun isBrowsingAllowed(): Boolean = try {
        Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)
    } catch (e: java.awt.HeadlessException) {
        false
    }
}
