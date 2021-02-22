package com.github.jensim.megamanipulatior.toolswindow

import javax.swing.JComponent

interface ToolWindowTab {

    fun refresh()
    val index: Int
    val content: JComponent
}
