package com.github.jensim.megamanipulatior.toolswindow

import com.intellij.ui.components.JBTabbedPane

object SettingPanel {

    val content = JBTabbedPane().apply {
        add("SourceGraph", SourcegraphSettingsPanel.content)
    }
}