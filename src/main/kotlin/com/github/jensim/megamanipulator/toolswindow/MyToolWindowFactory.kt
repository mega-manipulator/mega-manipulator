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
import org.slf4j.LoggerFactory
import java.io.File

class MyToolWindowFactory : ToolWindowFactory, TabServiceListener {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val refreshMap = mutableMapOf<String, ToolWindowTab>()
    private val selectMap = mutableMapOf<TabKey, Content>()
    private lateinit var toolWindow: ToolWindow

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        logger.debug("CreateToolWindowContent")
        val contentFactory: ContentFactory = ContentFactory.SERVICE.getInstance()
        this.toolWindow = toolWindow
        val tabs = listOf<Pair<TabKey, ToolWindowTab>>(
            TabKey.tabTitleSettings to SettingsWindow(project),
            TabKey.tabTitleSearch to SearchWindow(project),
            TabKey.tabTitleApply to ApplyWindow(project),
            TabKey.tabTitleClones to GitWindow(project),
            TabKey.tabTitlePRsManage to PullRequestWindow(project),
            TabKey.tabTitleForks to ForksWindow(project),
        )
        tabs.forEach { (headerKey, tab) ->
            val header = MyBundle.message(headerKey.name)
            val content: Content = contentFactory.createContent(tab.content, header, false)
            refreshMap[header] = tab
            selectMap[headerKey] = content
            toolWindow.contentManager.addContent(content)
        }
        val filesOperator = project.getService(FilesOperator::class.java)
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                super.selectionChanged(event)
                filesOperator.makeUpBaseFiles()
                filesOperator.refreshConf()
                toolWindow.contentManager.selectedContent?.displayName?.let { selectedTabName ->
                    refreshMap[selectedTabName]?.let {
                        it.refresh()
                    }
                }
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
            logger.error("Was unable to open the mega-manipulator.md welcome screen", e)
        }
        try {
            project.getService(TabSelectorService::class.java)
                ?.connectTabListener(this)
        } catch (e: Exception) {
            logger.error("Was unable to open the Mega Manipulator tab", e)
        }
    }

    override fun isApplicable(project: Project): Boolean {
        return super.isApplicable(project) && isMM(project)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        return super.shouldBeAvailable(project) && isMM(project)
    }

    override fun tabSelectionRequested(tabKey: TabKey) {
        selectMap[tabKey]?.let {
            toolWindow.contentManager.setSelectedContent(it)
        }
    }
}
