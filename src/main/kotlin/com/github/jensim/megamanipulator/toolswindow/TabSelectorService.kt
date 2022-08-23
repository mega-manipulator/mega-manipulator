package com.github.jensim.megamanipulator.toolswindow

import org.slf4j.LoggerFactory

class TabSelectorService {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val listeners: MutableList<TabServiceListener> = mutableListOf()

    fun connectTabListener(listener: TabServiceListener) {
        listeners.add(listener)
    }

    fun selectTab(tabKey: TabKey) {
        listeners.forEach {
            try {
                it.tabSelectionRequested(tabKey)
            } catch (e: Exception) {
                logger.error("Failed calling tabServiceListener ${it.javaClass.canonicalName}", e)
            }
        }
    }
}
