package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel

class EditPullRequestDialog(
    pullRequests: List<PullRequestWrapper>,
    project: Project,
) : CreatePullRequestDialog(
    yesText = "Edit PRs",
    title = "Edit pull request",
    project = project,
) {
    private val preExistingSelect: ComboBox<PullRequestWrapper?> = ComboBox(pullRequests.toTypedArray()).also { select ->
        select.addActionListener {
            prTitle = select.item?.title()
            prDescription = select.item?.body()
        }
        select.addCellRenderer { "${it.project().fixedLength(5)}/${it.baseRepo().fixedLength(5)} ${it.title().fixedLength(12)} ${it.body().fixedLength(12)}" }
        if (pullRequests.isNotEmpty()) {
            select.selectedItem = pullRequests.first()
        }
    }
    override val panel: DialogPanel = panel {
        row {
            component(panel(title = "Template from") {
                row {
                    component(preExistingSelect)

                }
            })
        }
        row { component(titlePanel) }
        row { component(descriptionPanel) }
        row { component(buttonPanel) }
    }

    private fun String.fixedLength(len: Int) = take(len).padEnd(len, ' ')
}
