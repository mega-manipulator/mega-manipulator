package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.EditPullRequestDialog
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.ui.components.JBTextArea
import java.awt.Desktop
import javax.swing.JComponent
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JOptionPane.CANCEL_OPTION
import javax.swing.JOptionPane.OK_CANCEL_OPTION
import javax.swing.JOptionPane.OK_OPTION
import javax.swing.JOptionPane.QUESTION_MESSAGE
import javax.swing.JPopupMenu
import org.slf4j.LoggerFactory

@SuppressWarnings("LongParameterList", "ConstructorParameterNaming")
class PullRequestActionsMenu(
    project: Project,
    _prRouter: PrRouter?,
    _notificationsOperator: NotificationsOperator?,
    _cloneOperator: CloneOperator?,
    _uiProtector: UiProtector?,
    _prefillOperator: PrefillStringSuggestionOperator?,

    private val focusComponent: JComponent,
    private val prProvider: () -> List<PullRequestWrapper>,
) : JPopupMenu() {

    constructor(
        project: Project,
        focusComponent: JComponent,
        prProvider: () -> List<PullRequestWrapper>
    ) : this(
        project = project,
        _prRouter = null,
        _notificationsOperator = null,
        _cloneOperator = null,
        _uiProtector = null,
        _prefillOperator = null,
        focusComponent = focusComponent,
        prProvider = prProvider,
    )

    private val prRouter: PrRouter by lazyService(project, _prRouter)
    private val notificationsOperator: NotificationsOperator by lazyService(project, _notificationsOperator)
    private val cloneOperator: CloneOperator by lazyService(project, _cloneOperator)
    private val uiProtector: UiProtector by lazyService(project, _uiProtector)
    private val prefillOperator: PrefillStringSuggestionOperator by lazyService(project, _prefillOperator)

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        val declineMenuItem = JMenuItem("Decline PRs").apply {
            addActionListener { _ ->
                DialogGenerator.showConfirm(
                    title = "Decline PRs",
                    message = """
                        No undo path available I'm afraid..
                        Decline selected PRs?
                    """.trimIndent(),
                    focusComponent = focusComponent,
                ) {
                    val dropBranchesAns = JOptionPane.showConfirmDialog(
                        null,
                        "Drop source branches?",
                        "Drop branches?",
                        OK_CANCEL_OPTION,
                        QUESTION_MESSAGE,
                        null
                    )
                    if (!listOf(OK_OPTION, CANCEL_OPTION).contains(dropBranchesAns)) return@showConfirm
                    val dropForksAns = JOptionPane.showConfirmDialog(
                        null,
                        "Also drop source forks?",
                        "Drop forks?",
                        OK_CANCEL_OPTION,
                        QUESTION_MESSAGE,
                        null
                    )
                    if (!listOf(OK_OPTION, CANCEL_OPTION).contains(dropForksAns)) return@showConfirm
                    uiProtector.mapConcurrentWithProgress(
                        title = "Declining prs",
                        extraText2 = { "${it.codeHostName()}/${it.project()}/${it.baseRepo()} ${it.fromBranch()}" },
                        data = prProvider(),
                    ) { pullRequest: PullRequestWrapper ->
                        prRouter.closePr(
                            dropFork = dropForksAns == OK_OPTION,
                            dropBranch = dropBranchesAns == OK_OPTION,
                            pullRequest
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
                        prefillOperator = prefillOperator
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
                DialogGenerator.showConfirm(
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
                DialogGenerator.showConfirm(
                    title = "Clone",
                    message = "Clone ${prs.size} selected PR branches",
                    focusComponent = focusComponent,
                ) {
                    cloneOperator.clone(prs)
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
                    val prefill: String? = prefillOperator.getPrefill(PrefillString.COMMENT)
                    DialogGenerator.askForInput(
                        title = "Comment selected pull requests",
                        message = "Comment",
                        field = JBTextArea(8, 80),
                        focusComponent = focusComponent,
                        position = Balloon.Position.atLeft,
                        prefill = prefill
                    ) { comment ->
                        uiProtector.mapConcurrentWithProgress(
                            title = "Add comments",
                            data = prs
                        ) {
                            prRouter.commentPR(comment, it)
                        }
                        prefillOperator.setPrefill(PrefillString.COMMENT, comment)
                    }
                }
            }
        }
        val approveMenuItem = JMenuItem("Mark Approved").apply {
            addActionListener { _ ->
                DialogGenerator.showConfirm(
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
                DialogGenerator.showConfirm(
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
                DialogGenerator.showConfirm(
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
