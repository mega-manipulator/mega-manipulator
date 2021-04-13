package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.actions.vcs.PullRequestWrapper
import com.github.jensim.megamanipulator.ui.GeneralListCellRenderer.addCellRenderer
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.panel

class EditPullRequestDialog(pullRequests: List<PullRequestWrapper>) : CreatePullRequestDialog() {
    override val title: String = "Edit pull request"
    private val preExistingSelect: ComboBox<PullRequestWrapper?> = ComboBox(pullRequests.toTypedArray()).also { select ->
        select.addActionListener { _ ->
            prTitle = select.item?.title()
            prDescription = select.item?.body()
        }
        select.addCellRenderer { "${it.project().fixedLength(5)}/${it.baseRepo().fixedLength(5)} ${it.title().fixedLength(12)} ${it.body().fixedLength(12)}" }
    }
    override val panel: DialogPanel = panel {
        row(label = "Template from") { component(preExistingSelect) }
        row(label = "PR Title") { component(titleField) }
        row(label = "PR Description") { component(descriptionScrollArea) }
    }
    private fun String.fixedLength(len: Int) = take(len).padEnd(len, ' ')
}
