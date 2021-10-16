package com.github.jensim.megamanipulator.toolswindow

import javax.swing.JComponent

interface ToolWindowTab {

    fun refresh()
    val content: JComponent
}
