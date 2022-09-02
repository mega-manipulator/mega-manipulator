package com.github.jensim.megamanipulator.ui

import com.intellij.ui.table.JBTable
import org.slf4j.LoggerFactory
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.function.Predicate
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel
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
    private val selectListeners = mutableListOf<() -> Unit>()
    private val clickListeners = mutableListOf<(MouseEvent, T?) -> Unit>()
    private val myRowSorter = TableRowSorter<GeneralTableModel<T>>(myModel)

    override fun getModel(): GeneralTableModel<T> {
        return myModel
    }

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
            if (!it.valueIsAdjusting) {
                selectListeners.forEach {
                    try {
                        it()
                    } catch (e: Throwable) {
                        logger.error("An exception escaped from a list selection listener :-O !!! ", e)
                    }
                }
            }
        }
        addMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent?) {
                if (clickListeners.isEmpty()) return
                e?.let { mouseEvent ->
                    mouseEvent.toItem().let { item ->
                        clickListeners.forEach { listener -> listener(mouseEvent, item) }
                    }
                }
            }
            override fun mousePressed(e: MouseEvent?) = Unit
            override fun mouseReleased(e: MouseEvent?) = Unit
            override fun mouseEntered(e: MouseEvent?) = Unit
            override fun mouseExited(e: MouseEvent?) = Unit
        })
    }

    private fun MouseEvent?.toItem(): T? {
        if (this == null) return null
        val row = rowAtPoint(point)
        if (row < 0) return null
        val col = columnAtPoint(point)
        if (col < 0) return null
        return if (items.size > row) {
            val index = rowSorter.convertRowIndexToModel(row)
            items.getOrNull(index)
        } else {
            null
        }
    }

    private val selectedProblemColor = Color.RED
    private val notSelectedProblemColor = selectedProblemColor.darker()
    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
        val index = rowSorter.convertRowIndexToModel(row)
        val c = super.prepareRenderer(renderer, row, column)
        if (colorizer != null) {
            val rowItem = items.getOrNull(index)
            if (rowItem != null && colorizer.test(rowItem)) {
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

    fun addClickListener(listener: (MouseEvent, T?) -> Unit) {
        clickListeners.add(listener)
    }

    fun setListData(items: List<T>) {
        myModel.items = items
        myModel.fireTableDataChanged()
        resizeAndRepaint()
    }

    fun selectFirst() {
        try {
            val zero = rowSorter.convertRowIndexToModel(0)
            selectionModel.setSelectionInterval(zero, zero)
        } catch (e: Exception) {
            logger.warn("Unable to select first")
        }
    }

    fun selectLast() {
        try {
            val indexLast = rowSorter.convertRowIndexToModel(items.size - 1)
            if (indexLast < 0) return
            selectionModel.setSelectionInterval(indexLast, indexLast)
        } catch (e: Exception) {
            logger.warn("Unable to select last")
        }
    }

    fun addListSelectionListener(listener: () -> Unit) {
        selectListeners.add(listener)
    }

    val selectedValuesList: List<T>
        get() {
            return selectedRows.map {
                try {
                    items.getOrNull(rowSorter.convertRowIndexToModel(it))
                } catch (e: Exception) {
                    null
                }
            }.filterNotNull()
        }

    class GeneralTableModel<T : Any>(
        private val type: KClass<T>,
        private val columns: List<Pair<ColumnHeader, (T) -> String>>,
    ) : DefaultTableModel() {

        private val logger = LoggerFactory.getLogger(javaClass)
        // val columnAccessors: List<KProperty1<T, *>> by lazy { type.memberProperties.toList() }

        var items: List<T> = emptyList()

        override fun isCellEditable(row: Int, column: Int): Boolean {
            return false
        }

        override fun getRowCount(): Int = items?.size ?: 0 // Throws NullPointers during initiation 🤔
        override fun getColumnCount(): Int = columns.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): String? = try {
            items.getOrNull(rowIndex)?.let {
                columns.getOrNull(columnIndex)?.second?.invoke(it)
            }
        } catch (e: Exception) {
            logger.warn("Unable to get value at row:$rowIndex, column:$columnIndex, for type:${type.simpleName}, column:${columns.getOrNull(columnIndex)?.first}, number of items:${items.size}")
            null
        }
    }

    data class TableMenu<T>(
        val header: String,
        val action: (T) -> Unit,
    )
}
