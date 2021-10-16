package com.github.jensim.megamanipulator.toolswindow

class TabSelectorService {

    private val listeners: MutableList<TabServiceListener> = mutableListOf()

    fun connectTabListener(listener:TabServiceListener){
        listeners.add(listener)
    }

    fun selectTab(tabKey: TabKey){
        listeners.forEach {
            try {
                it.tabSelectionRequested(tabKey)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
