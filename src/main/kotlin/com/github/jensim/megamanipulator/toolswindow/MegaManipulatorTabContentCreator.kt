package com.github.jensim.megamanipulator.toolswindow

import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.apply.ApplyWindow
import com.github.jensim.megamanipulator.actions.forks.ForksWindow
import com.github.jensim.megamanipulator.actions.git.CloneHistoryWindow
import com.github.jensim.megamanipulator.actions.git.GitWindow
import com.github.jensim.megamanipulator.actions.search.SearchWindow
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWindow
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.SettingsWindow
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener
import com.intellij.util.ui.components.BorderLayoutPanel
import org.slf4j.LoggerFactory
import java.io.File
import javax.swing.JButton

class MegaManipulatorTabContentCreator(
    private val project: Project,
) : TabServiceListener {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val refreshMap = mutableMapOf<String, ToolWindowTab>()
    private val selectMap = mutableMapOf<TabKey, Content>()
    private val toolWindow: ToolWindow? get() = ToolWindowManager.getInstance(project).getToolWindow("Mega Manipulator")

    fun createHelloContent() {
        ToolWindowManager.getInstance(project).getToolWindow("Mega Manipulator")?.let { toolWindow ->
            toolWindow.hide()
            val contentFactory: ContentFactory = ContentFactory.SERVICE.getInstance()
            // 2022.2 val contentFactory: ContentFactory = ContentFactory.getInstance()

            // Language=html
            val textContent = """<html>
                <h1>Mega Manipulator</h1>
                <p>This is not a Mega Manipulator project</p>
                <p>Please create a Mega Manipulator project from the New Project menu.</p>
                <p>If you REALLY want to run this plugin in an existing project, click the button above.</p>
                </html>
            """.trimIndent()
            val label = JBLabel(textContent)
            val overrideButton = JButton("Open Mega Manipulator content, even though this is not actually a Mega Manipulator project")
            val panel = BorderLayoutPanel()
            panel.addToTop(overrideButton)
            panel.addToCenter(label)
            val content = contentFactory.createContent(panel, "Hello", false)
            toolWindow.contentManager.addContent(content)
            overrideButton.addActionListener {
                val dialogGenerator: DialogGenerator = project.service()
                dialogGenerator.showConfirm(
                    title = "Open Mega Manipulator content?",
                    message = """
                        Open Mega Manipulator content,
                        even though this is not actually a Mega Manipulator project?..
                        
                        I take no responsibility for anything bad that might come from this,
                        or any other action you take with this plugin.
                    """.trimIndent(),
                    focusComponent = overrideButton
                ) {
                    toolWindow.contentManager.removeContent(content, false)
                    createContentMegaManipulator()
                }
            }
        }
    }

    fun createContentMegaManipulator() {
        val toolWindow = toolWindow ?: return
        val contentFactory: ContentFactory = ContentFactory.SERVICE.getInstance()
        // 2022.2 val contentFactory: ContentFactory = ContentFactory.getInstance()

        val tabs = listOf<Pair<TabKey, ToolWindowTab>>(
            TabKey.tabTitleSettings to SettingsWindow(project),
            TabKey.tabTitleSearch to SearchWindow(project),
            TabKey.tabTitleCloneHistory to CloneHistoryWindow(project),
            TabKey.tabTitleClones to GitWindow(project),
            TabKey.tabTitleApply to ApplyWindow(project),
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
                    refreshMap[selectedTabName]?.refresh()
                }
            }
            override fun contentRemoveQuery(event: ContentManagerEvent) {
                logger.warn("User tried to remove a tab")
                event.consume()
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
        try {
            tabs.find { it.first == TabKey.tabTitleSettings }?.second?.refresh()
        } catch (e: Exception) {
            logger.warn("Was unable to refresh the settings page")
        }
    }

    override fun tabSelectionRequested(tabKey: TabKey) {
        selectMap[tabKey]?.let {
            toolWindow?.contentManager?.setSelectedContent(it)
        }
    }
}
