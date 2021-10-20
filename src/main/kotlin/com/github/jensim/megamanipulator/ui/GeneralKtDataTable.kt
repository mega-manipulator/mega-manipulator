package com.github.jensim.megamanipulator.ui

import com.intellij.ui.table.JBTable
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.util.function.Predicate
import javax.swing.JTable
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import kotlin.reflect.KClass

typealias ColumnHeader = String

class GeneralKtDataTable<T : Any>(
    type: KClass<T>,
    columns: List<Pair<ColumnHeader, (T) -> String>>,
    minSize: Dimension = Dimension(300, 100),
    colorizer: Predicate<T>? = null,
) : JBTable() {

    private val myModel = GeneralTableModel(type, columns)
    private val listeners = mutableListOf<() -> Unit>()

    init {
        model = myModel
        minimumSize = minSize
        with(columnModel) {
            columnSelectionAllowed = false
            columns.forEachIndexed { idx, (header, _) ->
                with(getColumn(idx)) {
                    minWidth = 25
                    headerValue = header
                }
            }
        }
        getSelectionModel().addListSelectionListener {
            listeners.forEach {
                try {
                    it()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        colorizer?.let { colorizer ->
            val selectedProblemColor = Color.RED
            val notSelectedProblemColor = selectedProblemColor.darker()
            setDefaultRenderer(
                Any::class.java,
                object : DefaultTableCellRenderer() {
                    override fun getTableCellRendererComponent(table: JTable, value: Any?, isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
                        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                        try {
                            val rowItem = myModel.items[row]
                            if (colorizer.test(rowItem)) {
                                background = if (isSelected) selectedProblemColor else notSelectedProblemColor
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        return this
                    }
                }
            )
        }
    }

    fun setListData(items: List<T>) {
        myModel.items = items
        resizeAndRepaint()
    }

    fun selectFirst() {
        try {
            if (myModel.items.isNotEmpty()) {
                selectionModel.setSelectionInterval(0, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addListSelectionListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    val selectedValuesList: List<T>
        get() {
            return selectedRows.map {
                try {
                    myModel.items[it]
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()
        }

    private class GeneralTableModel<T : Any>(
        private val type: KClass<T>,
        private val columns: List<Pair<ColumnHeader, (T) -> String>>,
    ) : AbstractTableModel() {
        // val columnAccessors: List<KProperty1<T, *>> by lazy { type.memberProperties.toList() }

        var items = emptyList<T>()

        override fun getRowCount(): Int = items.size
        override fun getColumnCount(): Int = columns.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? = try {
            columns[columnIndex].second(items[rowIndex])
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
