package com.github.jensim.megamanipulator.toolswindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.slf4j.LoggerFactory

class MyToolWindowFactory : ToolWindowFactory {

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        logger.info("CreateToolWindowContent")
    }
}
