package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.github.jensim.megamanipulator.toolswindow.ToolWindowTab
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

class CloneHistoryWindow(val project: Project) : ToolWindowTab {

    private val state: MegaManipulatorSettingsState by lazy { project.service() }

    override val content: JComponent = panel {
        row {
            text("Hello world, my name is Clone History Window")
        }
    }

    override fun refresh() {
        TODO("Not yet implemented")
    }
}
