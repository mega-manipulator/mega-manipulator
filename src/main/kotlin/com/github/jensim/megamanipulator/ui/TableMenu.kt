package com.github.jensim.megamanipulator.ui

import com.intellij.openapi.ui.JBMenuItem
import com.intellij.openapi.ui.JBPopupMenu
import org.slf4j.LoggerFactory
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.function.Predicate
import javax.swing.JComponent

class TableMenu<T>(
    private val component: JComponent,
    private val menus: List<MenuItem<T>>
) {

    fun show(mouseEvent: MouseEvent, items: List<T>) {
        show(Point(mouseEvent.x, mouseEvent.y), items)
    }

    fun show(point: Point, items: List<T>) {
        val menu = JBPopupMenu()
        menus.forEach { menu.add(it.toMenu(items)) }
        menu.autoscrolls = true
        menu.isVisible = true
        menu.show(component, point.x, point.y)
    }

    class MenuItem<T>(
        private val headerer: (List<T>) -> String,
        private val appendFilteredItemCountSuffix: Boolean = false,
        private val filter: Predicate<T> = Predicate { true },
        private val onClick: (T) -> Unit,
    ) {

        companion object {
            private val logger = LoggerFactory.getLogger(MenuItem::class.java)
        }

        fun toMenu(items: List<T>): JBMenuItem {
            val filtered = items.filter { filter.test(it) }
            val baseHeader = headerer(items)
            val header = if (appendFilteredItemCountSuffix) appendCountSuffix(filtered.size, items.size, baseHeader) else baseHeader
            val menu = JBMenuItem(header)
            if (filtered.isEmpty()) {
                menu.isEnabled = false
            } else {
                menu.addActionListener {
                    filtered.forEach {
                        try {
                            onClick(it)
                        } catch (e: Exception) {
                            logger.error("Something went wrong in menu action execution", e)
                        }
                    }
                }
            }
            return menu
        }

        private fun appendCountSuffix(filtered: Int, total: Int, baseHeader: String): String {
            return when (filtered) {
                total -> "$baseHeader ($total)"
                else -> "$baseHeader ($filtered/$total)"
            }
        }
    }
}
