package com.github.jensim.megamanipulatior.toolswindow

import com.github.jensim.megamanipulatior.MyBundle
import com.github.jensim.megamanipulatior.actions.apply.ApplyWindow
import com.github.jensim.megamanipulatior.actions.search.SearchWindow
import com.github.jensim.megamanipulatior.settings.SettingsWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManagerEvent
import com.intellij.ui.content.ContentManagerListener

object MyToolWindowFactory : ToolWindowFactory {

    private val tabs = listOf<Pair<String, ToolWindowTab>>(
        "tabTitleSettings" to SettingsWindow,
        "tabTitleSearch" to SearchWindow,
        "tabTitleApply" to ApplyWindow,
        //"tabTitlePRsCreate" to noteComponent("Create PRs"),
        //"tabTitlePRsManage" to noteComponent("Manage PRs"),
    )

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        tabs.sortedBy { it.second.index }.forEachIndexed { index, (headerKey, tab) ->
            if (index == 0) {
                tab.refresh()
            }
            val content1 = contentFactory.createContent(tab.content, MyBundle.message(headerKey), false)
            toolWindow.contentManager.addContent(content1)
        }
        toolWindow.addContentManagerListener(object : ContentManagerListener {
            override fun selectionChanged(event: ContentManagerEvent) {
                super.selectionChanged(event)
                tabs.find { it.second.index == event.index }?.second?.refresh()
            }
        })

    }
}
