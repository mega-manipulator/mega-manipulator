package com.github.jensim.megamanipulatior.toolswindow

import com.github.jensim.megamanipulatior.toolswindow.SettingPanel.Tab.CODE_HOST_SETTINGS
import com.github.jensim.megamanipulatior.toolswindow.SettingPanel.setSelectedTab
import com.intellij.ui.layout.panel

object CodeHostsSettings {

    val conent = panel {
        noteRow("Code hosts")
        row {
            button("Create code host") {
                setSelectedTab(CODE_HOST_SETTINGS)
                println("Create code host")
            }
        }
    }
}