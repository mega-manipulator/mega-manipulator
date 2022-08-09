package com.github.jensim.megamanipulator.ui

import com.intellij.ui.table.JBTable
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.util.function.Predicate
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import kotlin.reflect.KClass

typealias ColumnHeader = String

class GeneralKtDataTable<T : Any>(
    type: KClass<T>,
    columns: List<Pair<ColumnHeader, (T) -> String>>,
    autoRowSorter: Boolean = true,
    minSize: Dimension = Dimension(300, 100),
    /**
     * @see ListSelectionModel
     */
    selectionMode: Int? = null,
    private val colorizer: Predicate<T>? = null,
) : JBTable() {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val myModel = GeneralTableModel<T>(type, columns)
    private val listeners = mutableListOf<() -> Unit>()
    private val myRowSorter = TableRowSorter<GeneralTableModel<T>>(myModel)

    val items: List<T> get() = myModel.items

    init {
        this.model = myModel
        if (autoRowSorter) {
            this.rowSorter = myRowSorter
        }
        this.minimumSize = minSize
        selectionMode?.let {
            setSelectionMode(it)
        }
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
                } catch (e: Throwable) {
                    logger.error("An exception escaped from a list selection listener :-O !!! ", e)
                }
            }
        }
    }

    private val selectedProblemColor = Color.RED
    private val notSelectedProblemColor = selectedProblemColor.darker()
    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
        val c = super.prepareRenderer(renderer, row, column)
        if (colorizer != null) {
            val rowItem = myModel.items[row]
            if (colorizer.test(rowItem)) {
                if (isRowSelected(row)) {
                    selectionBackground
                    c.background = selectedProblemColor
                } else {
                    c.background = notSelectedProblemColor
                }
            } else {
                if (isRowSelected(row)) {
                    c.background = selectionBackground
                } else {
                    c.background = background
                }
            }
        }
        return c
    }

    fun setListData(items: List<T>) {
        myModel.items = items
        resizeAndRepaint()
    }

    fun selectFirst() {
        try {
            selectionModel.setSelectionInterval(0, 0)
        } catch (e: Exception) {
            logger.warn("Unable to select first")
        }
    }

    fun selectLast() {
        try {
            val indexLast = myModel.items.size - 1
            if (indexLast < 0) return
            selectionModel.setSelectionInterval(indexLast, indexLast)
        } catch (e: Exception) {
            logger.warn("Unable to select last")
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

        private val logger = LoggerFactory.getLogger(javaClass)
        // val columnAccessors: List<KProperty1<T, *>> by lazy { type.memberProperties.toList() }

        var items = emptyList<T>()

        override fun getRowCount(): Int = items.size
        override fun getColumnCount(): Int = columns.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? = try {
            columns[columnIndex].second(items[rowIndex])
        } catch (e: Exception) {
            logger.warn("Unable to get value at row:$rowIndex, column:$columnIndex, for type:${type.simpleName}, column:${columns[columnIndex]}, number of items:${items.size}")
            null
        }
    }
}
