package com.github.jensim.megamanipulator.ui

import com.intellij.ui.table.JBTable
import javax.swing.table.AbstractTableModel
import kotlin.reflect.KClass

typealias ColumnHeader = String

class GeneralKtDataTable<T : Any>(
    type: KClass<T>,
    columns: List<Pair<ColumnHeader, (T) -> String>>
) : JBTable() {

    private val myModel = GeneralTableModel(type, columns)
    private val listeners = mutableListOf<() -> Unit>()

    init {
        model = myModel
        with(columnModel) {
            columnSelectionAllowed = false
            columns.forEachIndexed { idx, (header, _) ->
                with(getColumn(idx)) {
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
