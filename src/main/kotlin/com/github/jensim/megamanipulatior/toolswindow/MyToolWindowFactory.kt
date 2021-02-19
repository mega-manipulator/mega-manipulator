package com.github.jensim.megamanipulatior.toolswindow

import com.github.jensim.megamanipulatior.MyBundle
import com.github.jensim.megamanipulatior.actions.apply.ApplyWindow
import com.github.jensim.megamanipulatior.actions.search.SearchWindow
import com.github.jensim.megamanipulatior.module.MegaManipulatorModuleType.Companion.MODULE_TYPE_ID
import com.github.jensim.megamanipulatior.settings.ProjectOperator
import com.github.jensim.megamanipulatior.settings.SettingsWindow
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.noteComponent
import com.intellij.ui.content.ContentFactory

object MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content1 = contentFactory.createContent(SettingsWindow.content, MyBundle.message("tabTitleSettings"), false)
        toolWindow.contentManager.addContent(content1)
        val content2 = contentFactory.createContent(SearchWindow.content, MyBundle.message("tabTitleSearch"), false)
        toolWindow.contentManager.addContent(content2)
        //val content3 = contentFactory.createContent(noteComponent("Clone"), MyBundle.message("tabTitleClone"), false)
        //toolWindow.contentManager.addContent(content3)
        val content4 = contentFactory.createContent(ApplyWindow.content, MyBundle.message("tabTitleApply"), false)
        toolWindow.contentManager.addContent(content4)
        val content5 = contentFactory.createContent(noteComponent("Commit"), MyBundle.message("tabTitleCommit"), false)
        toolWindow.contentManager.addContent(content5)
        val content6 =
            contentFactory.createContent(noteComponent("Create PRs"), MyBundle.message("tabTitlePRsCreate"), false)
        toolWindow.contentManager.addContent(content6)
        val content7 = contentFactory.createContent(noteComponent("Manage PRs"), MyBundle.message("tabTitlePRsManage"), false)
        toolWindow.contentManager.addContent(content7)
    }

    override fun isApplicable(project: Project): Boolean {
        val applicable = ModuleManager.getInstance(project).modules.any {
            it.moduleTypeName == MODULE_TYPE_ID
        } && super.isApplicable(project)
        if (applicable) {
            ProjectOperator.project = project
        }
        return applicable
    }
}
