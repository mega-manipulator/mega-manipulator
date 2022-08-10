package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.project.PrefillString
import com.github.jensim.megamanipulator.project.PrefillStringSuggestionOperator
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettingsType
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import org.slf4j.LoggerFactory
import javax.swing.InputVerifier
import javax.swing.JButton
import javax.swing.JComponent

class PullRequestLoaderDialogGenerator(
    private val project: Project,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val projectInput = JBTextField(30)
    private val repoInput = JBTextField(30)
    private val stateSelector = ComboBox<String>()
    private val roleSelector = ComboBox<String>()

    private val limitField = JBTextField("100")
    private val btnYes = JButton("Load")
    private val btnNo = JButton("Cancel")
    private val magicNull = "*"

    private val prefillStringSuggestionOperator: PrefillStringSuggestionOperator by lazy { project.service() }

    init {
        limitField.inputVerifier = object : InputVerifier() {
            override fun verify(input: JComponent?): Boolean = try {
                limitField.text.toInt()
                btnYes.isEnabled = true
                true
            } catch (e: NumberFormatException) {
                btnYes.isEnabled = false
                false
            }
        }
    }

    fun generateDialog(
        focus: JComponent,
        type: CodeHostSettingsType,
        onYes: (state: String?, role: String?, limit: Int, project: String?, repo: String?) -> Unit
    ) {
        try {
            type.prStates.forEach { stateSelector.addItem(it ?: magicNull) }
            type.prRoles.forEach { roleSelector.addItem(it ?: magicNull) }

            val content = com.intellij.ui.dsl.builder.panel {
                addProjectRepoInput(type)
                row {
                    groupPanel("Pull request state") {
                        cell(stateSelector)
                    }
                }
                row {
                    groupPanel("Pull request role") {
                        cell(roleSelector)
                    }
                }
                row {
                    groupPanel("Limit") {
                        cell(limitField)
                    }
                }
                row {
                    cell(btnYes); cell(btnNo)
                }
            }
            val popupFactory: JBPopupFactory = JBPopupFactory.getInstance()
            val popup = popupFactory.createDialogBalloonBuilder(content, "Load pull requests")
                .createBalloon()
            btnYes.addActionListener {
                val text = limitField.text
                try {
                    val limit = text.toInt()
                    onYes(
                        stateSelector.item.toNullable(),
                        roleSelector.item.toNullable(),
                        limit,
                        projectInput.text.ifBlank { null }?.trim()?.also {
                            prefillStringSuggestionOperator.addPrefill(PrefillString.PR_SEARCH_PROJECT, it)
                        },
                        repoInput.text.ifBlank { null }?.trim()?.also {
                            prefillStringSuggestionOperator.addPrefill(PrefillString.PR_SEARCH_REPO, it)
                        },
                    )
                } catch (e: NumberFormatException) {
                    logger.warn("Not a valid number '$text'")
                }
                popup.hide()
            }
            btnNo.addActionListener {
                popup.hide()
            }
            val location: RelativePoint = popupFactory.guessBestPopupLocation(focus)

            popup.show(location, Balloon.Position.above)
        } catch (e: Exception) {
            logger.error("Failed setting up the Pull Request loader dialog", e)
        }
    }

    private fun Panel.addProjectRepoInput(type: CodeHostSettingsType) {
        if (type == CodeHostSettingsType.GITHUB) {
            projectInput.text = prefillStringSuggestionOperator.getPrefill(PrefillString.PR_SEARCH_PROJECT)
            repoInput.text = prefillStringSuggestionOperator.getPrefill(PrefillString.PR_SEARCH_REPO)
            addProjectRepoInput("User / Org", "Repo")
        } else if (type == CodeHostSettingsType.GITLAB) {
            projectInput.text = prefillStringSuggestionOperator.getPrefill(PrefillString.PR_SEARCH_PROJECT)
            addProjectRepoInput("Group", null)
        }
    }

    private fun Panel.addProjectRepoInput(projectNaming: String, repoNaming: String?) {
        row {
            groupPanel(projectNaming) {
                cell(projectInput)
                cell(PrefillHistoryButton(project, PrefillString.PR_SEARCH_PROJECT, projectInput) { projectInput.text = it })
            }
        }
        if (repoNaming != null) {
            row {
                groupPanel(repoNaming) {
                    cell(repoInput)
                    cell(PrefillHistoryButton(project, PrefillString.PR_SEARCH_REPO, repoInput) { repoInput.text = it })
                }
            }
        }
    }

    private fun String.toNullable(): String? = when (this) {
        magicNull -> null
        else -> this
    }
}
