package com.github.jensim.megamanipulatior.toolswindow

import com.github.jensim.megamanipulatior.toolswindow.SettingPanel.Tab.CODE_HOSTS
import com.github.jensim.megamanipulatior.toolswindow.SettingPanel.setSelectedTab
import com.intellij.ui.layout.panel

object CodeHostSettings {

    val conent = panel {
        noteRow("Code host")
        row {
            button("Create or update") {
                setSelectedTab(CODE_HOSTS)
                println("Create code host")
            }
        }
    }
}