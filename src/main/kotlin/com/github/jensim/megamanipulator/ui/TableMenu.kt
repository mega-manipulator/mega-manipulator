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

    fun show(mouseEvent: MouseEvent, t: T) {
        show(Point(mouseEvent.x, mouseEvent.y), t)
    }

    fun show(point: Point, t: T) {
        val menu = JBPopupMenu()
        menus.forEach { menu.add(it.toMenu(t)) }
        menu.autoscrolls = true
        menu.isVisible = true
        menu.show(component, point.x, point.y)
    }

    class MenuItem<T> private constructor(
        private val header: String?,
        private val headerer: ((T) -> String)?,
        private val filter: Predicate<T> = Predicate { true },
        private val onClick: (T) -> Unit,
    ) {

        constructor(
            header: String,
            filter: Predicate<T> = Predicate { true },
            onClick: (T) -> Unit,
        ) : this(header, null, filter, onClick)

        constructor(
            header: (T) -> String,
            filter: Predicate<T> = Predicate { true },
            onClick: (T) -> Unit,
        ) : this(null, header, filter, onClick)

        companion object {
            private val logger = LoggerFactory.getLogger(MenuItem::class.java)
        }

        fun toMenu(t: T): JBMenuItem {
            val header = header ?: headerer!!(t)
            val menu = JBMenuItem(header)
            if (!filter.test(t)) {
                menu.isEnabled = false
            } else {
                menu.addActionListener {
                    try {
                        onClick(t)
                    } catch (e: Exception) {
                        logger.error("Something went wrong in menu action execution", e)
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
