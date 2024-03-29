package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.ui.CloneDialogFactory
import com.github.jensim.megamanipulator.ui.ClosePRDialogFactory
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.EditPullRequestDialog
import com.github.jensim.megamanipulator.ui.TableMenu
import com.github.jensim.megamanipulator.ui.TableMenu.MenuItem
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.components.JBTextArea
import org.slf4j.LoggerFactory
import java.awt.Desktop
import javax.swing.JComponent

@SuppressWarnings("LongParameterList", "ConstructorParameterNaming")
class PullRequestActionsMenu(
    project: Project,
    private val focusComponent: JComponent,
) {

    private val prRouter: PrRouter by lazy { project.service() }
    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val cloneOperator: CloneOperator by lazy { project.service() }
    private val uiProtector: UiProtector by lazy { project.service() }
    private val cloneDialogFactory: CloneDialogFactory by lazy { project.service() }
    private val dialogGenerator: DialogGenerator by lazy { project.service() }

    private val logger = LoggerFactory.getLogger(javaClass)

    val menu = TableMenu<List<PullRequestWrapper>>(
        focusComponent,
        menus = listOf(
            MenuItem({ "Decline PRs (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                ClosePRDialogFactory.openCommitDialog(relativeComponent = focusComponent) { removeBranches, removeStaleForks ->
                    uiProtector.mapConcurrentWithProgress(
                        title = "Declining prs",
                        extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                        data = prs,
                    ) { pullRequest: PullRequestWrapper ->
                        prRouter.closePr(
                            dropFork = removeStaleForks,
                            dropBranch = removeBranches,
                            pullRequest,
                        )
                    }
                }
            },

            MenuItem({ "Reword PRs (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                if (prs.isEmpty()) {
                    notificationsOperator.show(
                        "No PRs selected", "Please select at least one PR to use this", NotificationType.WARNING
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
            },
            MenuItem({ "Add default reviewers (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
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
                            data = prs,
                        ) { pr ->
                            prRouter.addDefaultReviewers(pr)
                        }
                    )
                }
            },
            MenuItem({ "Clone PRs (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                cloneDialogFactory.showCloneFromPrDialog(focusComponent) { sparseDef ->
                    uiProtector.uiProtectedOperation("Clone from PRs") {
                        cloneOperator.clone(prs, sparseDef = sparseDef)
                    }
                }
            },
            MenuItem({ "Open in browser (${it.size})" }, isEnabled = { isBrowsingAllowed() && it.isNotEmpty() }) { prs ->
                val failed = mutableMapOf<PullRequestWrapper, String>()
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
                    val failMsg = failed.map { (k, v) -> "${k.project()}/${k.baseRepo()} ${k.title().take(10)} :: $v" }.joinToString("\n")
                    notificationsOperator.show(
                        title = "Failed opening ${failed.size}/${prs.size} pull requests", body = failMsg, type = NotificationType.ERROR
                    )
                }
            },
            MenuItem({ "Add comment (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                if (prs.isNotEmpty()) {
                    dialogGenerator.askForInput(
                        title = "Comment selected pull requests", message = "Comment", field = JBTextArea(8, 80), focusComponent = focusComponent, position = Balloon.Position.atLeft, prefill = PrefillString.COMMENT
                    ) { comment ->
                        uiProtector.mapConcurrentWithProgress(
                            title = "Add comments", data = prs
                        ) {
                            prRouter.commentPR(comment, it)
                        }
                    }
                }
            },
            MenuItem({ "Mark Approved (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                dialogGenerator.showConfirm(
                    title = "Mark Approved",
                    message = "Mark the selected pull requests as Approved",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "setStatus(approved)",
                        uiProtector.mapConcurrentWithProgress(title = "Mark Approved", data = prs) {
                            prRouter.approvePr(it)
                        }
                    )
                }
            },
            MenuItem({ "Mark Needs work (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                dialogGenerator.showConfirm(
                    title = "Mark Needs work",
                    message = "Mark the selected pull requests as Needs work",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "setStatus(needsWork)",
                        uiProtector.mapConcurrentWithProgress(title = "Mark Needs work", data = prs) {
                            prRouter.disapprovePr(it)
                        }
                    )
                }
            },
            MenuItem({ "Merge (${it.size})" }, isEnabled = { it.isNotEmpty() }) { prs ->
                dialogGenerator.showConfirm(
                    title = "Merge",
                    message = "Merge the selected pull requests",
                    focusComponent = focusComponent,
                ) {
                    prFeedback(
                        "merge",
                        uiProtector.mapConcurrentWithProgress(title = "Merge", data = prs) {
                            prRouter.mergePr(it)
                        }
                    )
                }
            },
        )
    )

    private fun prFeedback(action: String, list: List<Pair<PullRequestWrapper, PrActionStatus?>>) {
        val (failed, succeeded) = list.partition { it.second?.success != true }
        val succeededNames = succeeded.joinToString("\n") { it.first.asPathString() }
        if (succeeded.isNotEmpty()) {
            notificationsOperator.show(
                title = "${succeeded.size}/${list.size} $action succeeded", body = succeededNames, type = NotificationType.INFORMATION
            )
        }
        val failInfo = failed.map {
            it.first.asPathString() to (it.second?.msg ?: "<NO_INFO>")
        }
        if (failed.isNotEmpty()) {
            logger.error("${failed.size}/${list.size} failed.")
            failInfo.forEach {
                logger.error("Failed PR $action: ${it.first} -> ${it.second}")
            }
            notificationsOperator.show(
                title = "${failed.size}/${list.size} $action failed",
                body = """
Check log for more info
${failInfo.joinToString("\n") { it.first }}
""",
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
