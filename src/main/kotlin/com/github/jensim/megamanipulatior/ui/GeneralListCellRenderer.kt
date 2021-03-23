package com.github.jensim.megamanipulatior.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import java.awt.Color
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

object GeneralListCellRenderer {

    inline fun <reified T> ComboBox<T>.addCellRenderer(crossinline textMut: (T) -> String) {
        this.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = if (value is T) {
                    textMut(value)
                } else {
                    "error endering"
                }
                return this
            }
        }
    }

    inline fun <reified T> ComboBox<T>.addCellRenderer(crossinline textMut: (T) -> String, crossinline toolTipMut: (T) -> String) {
        this.renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is T) {
                    text = textMut(value)
                    toolTipText = toolTipMut(value)
                } else {
                    text = "error rendering"
                }
                return this
            }
        }
    }

    inline fun <reified T> JBList<T>.addCellRenderer(
        crossinline colorResolver: (T) -> Color?,
        crossinline textMut: (T) -> String
    ) {
        val renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = if (value is T) {
                    colorResolver(value)?.let {
                        background = it
                    }
                    textMut(value)
                } else {
                    background = Color.RED
                    "error endering"
                }

                return this
            }
        }
        this.cellRenderer = renderer
    }

    inline fun <reified T> JBList<T>.addCellRenderer(crossinline textMut: (T) -> String) {
        this.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                if (value is T) {
                    text = textMut(value)
                } else {
                    text = "error rendering"
                }
                return this
            }
        }
    }
}
