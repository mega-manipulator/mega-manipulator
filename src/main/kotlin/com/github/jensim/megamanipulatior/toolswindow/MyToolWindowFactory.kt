package com.github.jensim.megamanipulatior.toolswindow

import com.github.jensim.megamanipulatior.MyBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.noteComponent
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.layout.panel

class MyToolWindowFactory : ToolWindowFactory {

    private val panel: DialogPanel = panel {
        noteRow("Foo bar")
        row {
            button("Click me!") {
                println("I was clicked")
                JBPopupFactory.getInstance()
                    .createComponentPopupBuilder(noteComponent("Foo!"), null)
                    .setCancelButton(IconButton("Close", AllIcons.Windows.CloseSmall))
                    .createPopup()
                    .showInFocusCenter()
            }
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, MyBundle.message("tabTitleSettings"), false)
        toolWindow.contentManager.addContent(content)
        val content2 = contentFactory.createContent(noteComponent("Search"), MyBundle.message("tabTitleSearch"), false)
        toolWindow.contentManager.addContent(content2)
        val content3 = contentFactory.createContent(noteComponent("Clone"), MyBundle.message("tabTitleClone"), false)
        toolWindow.contentManager.addContent(content3)
        val content4 = contentFactory.createContent(noteComponent("Apply"), MyBundle.message("tabTitleApply"), false)
        toolWindow.contentManager.addContent(content4)
        val content5 = contentFactory.createContent(noteComponent("Commit"), MyBundle.message("tabTitleCommit"), false)
        toolWindow.contentManager.addContent(content5)
        val content6 =
            contentFactory.createContent(noteComponent("Create PRs"), MyBundle.message("tabTitlePRsCreate"), false)
        toolWindow.contentManager.addContent(content6)
        val content7 =
            contentFactory.createContent(noteComponent("Manage PRs"), MyBundle.message("tabTitlePRsManage"), false)
        toolWindow.contentManager.addContent(content7)
    }

    override fun isApplicable(project: Project): Boolean {
        return ModuleManager.getInstance(project).modules.any {
            it.moduleTypeName == "mega_manipulator"
        } && super.isApplicable(project)
    }
}