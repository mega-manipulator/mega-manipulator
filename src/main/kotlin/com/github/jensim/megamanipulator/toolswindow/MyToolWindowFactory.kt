package com.github.jensim.megamanipulator.toolswindow

import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.apply.ApplyWindow
import com.github.jensim.megamanipulator.actions.forks.ForksWindow
import com.github.jensim.megamanipulator.actions.git.GitWindow
import com.github.jensim.megamanipulator.actions.search.SearchWindow
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWindow
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
import com.github.jensim.megamanipulator.settings.SettingsWindow
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import java.io.File

class MyToolWindowFactory : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory: ContentFactory = ContentFactory.SERVICE.getInstance()

        val tabs = listOf<Pair<String, ToolWindowTab>>(
            "tabTitleSettings" to SettingsWindow(project),
            "tabTitleSearch" to SearchWindow(project),
            "tabTitleApply" to ApplyWindow(project),
            "tabTitleClones" to GitWindow(project),
            "tabTitlePRsManage" to PullRequestWindow(project),
            "tabTitleForks" to ForksWindow(project),
        )
        tabs.sortedBy { it.second.index }.forEachIndexed { index, (headerKey, tab) ->
            if (index == 0) {
                tab.refresh()
            }
            val content1: Content = contentFactory.createContent(tab.content, MyBundle.message(headerKey), false)
            toolWindow.contentManager.addContent(content1)
        }
        val filesOperator = project.getService(FilesOperator::class.java)
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                super.selectionChanged(event)
                filesOperator.makeUpBaseFiles()
                filesOperator.refreshConf()
                tabs.find { it.second.index == event.index }?.second?.refresh()
            }
        })
        filesOperator.makeUpBaseFiles()
        try {
            VirtualFileManager.getInstance().let {
                it.findFileByNioPath(File("${project.basePath}/config/mega-manipulator.md").toPath())
                    ?.let { file: VirtualFile ->
                        FileEditorManager.getInstance(project).openFile(file, true)
                    }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }

    override fun isApplicable(project: Project): Boolean {
        return super.isApplicable(project) && isMM(project)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return super.shouldBeAvailable(project) && isMM(project)
    }
}
