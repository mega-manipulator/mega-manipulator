package com.github.jensim.megamanipulatior.ui

import com.github.jensim.megamanipulatior.actions.vcs.PullRequest
import com.github.jensim.megamanipulatior.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel

class EditPullRequestDialog(pullRequests: List<PullRequest>) : CreatePullRequestDialog() {
    override val title: String = "Edit pull request"
    private val preExistingSelect: ComboBox<PullRequest?> = ComboBox(pullRequests.toTypedArray()).also { select ->
        select.addActionListener { _ ->
            prTitle = select.item?.title()
            prDescription = select.item?.body()
        }
        select.addCellRenderer { "${it.project().fixedLength(5)}/${it.repo().fixedLength(5)} ${it.title().fixedLength(12)} ${it.body().fixedLength(12)}" }
    }
    override val panel: DialogPanel = panel {
        row(label = "Template from") { component(preExistingSelect) }
        row(label = "PR Title") { component(titleField) }
        row(label = "PR Description") { component(descriptionScrollArea) }
    }
}
