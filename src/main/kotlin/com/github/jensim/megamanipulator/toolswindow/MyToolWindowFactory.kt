package com.github.jensim.megamanipulator.toolswindow

import com.github.jensim.megamanipulator.ApplicationWiring
import com.github.jensim.megamanipulator.project.MegaManipulatorUtil.isMM
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
import java.util.function.Supplier

class MyToolWindowFactory(
    private val applicationWiring: ApplicationWiring?,
    private val contentFactorySupplier: Supplier<ContentFactory>,
) : ToolWindowFactory {

    constructor() : this(
        contentFactorySupplier = { ContentFactory.SERVICE.getInstance() },
        applicationWiring = null
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val wiring = applicationWiring ?: ApplicationWiring(project)
        val contentFactory: ContentFactory = contentFactorySupplier.get()
        val tabs = listOf<Pair<String, ToolWindowTab>>(
            "tabTitleSettings" to wiring.tabSettings,
            "tabTitleSearch" to wiring.tabSearch,
            "tabTitleApply" to wiring.tabApply,
            "tabTitleClones" to wiring.tabClones,
            "tabTitlePRsManage" to wiring.tabPRsManage,
            "tabTitleForks" to wiring.tabForks,
        )
        tabs.sortedBy { it.second.index }.forEachIndexed { index, (headerKey, tab) ->
            if (index == 0) {
                tab.refresh()
            }
            val content1: Content = contentFactory.createContent(tab.content, wiring.myBundle.message(headerKey), false)
            toolWindow.contentManager.addContent(content1)
        }
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                super.selectionChanged(event)
                wiring.filesOperator.makeUpBaseFiles()
                wiring.filesOperator.refreshConf()
                tabs.find { it.second.index == event.index }?.second?.refresh()
            }
        })
        wiring.filesOperator.makeUpBaseFiles()
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
