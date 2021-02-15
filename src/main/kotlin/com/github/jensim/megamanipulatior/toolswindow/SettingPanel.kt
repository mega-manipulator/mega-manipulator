package com.github.jensim.megamanipulatior.toolswindow

import com.intellij.ui.components.JBTabbedPane

object SettingPanel {

    enum class Tab {
        SOURCEGRAPH,
        CODE_HOSTS,
        CODE_HOST_SETTINGS,
    }

    val content = JBTabbedPane().apply {
        add("SourceGraph", SourcegraphSettingsPanel.content)
        add("Code hosts", CodeHostsSettings.conent)
        add("Code host settings", CodeHostSettings.conent)
    }

    fun setSelectedTab(tab: Tab) {
        when (tab) {
            Tab.SOURCEGRAPH -> content.selectedIndex = 0
            Tab.CODE_HOSTS -> content.selectedIndex = 1
            Tab.CODE_HOST_SETTINGS -> content.selectedIndex = 2
        }
    }
}
