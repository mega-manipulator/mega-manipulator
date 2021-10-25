package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.ui.CodeHostSelector.CodeHostSelect
import com.intellij.openapi.ui.ComboBox
import java.awt.Dimension

class CodeHostSelector(
    private val settingsFileOperator: SettingsFileOperator,
    private val codeHostLoader: () -> List<CodeHostSelect> = {
        settingsFileOperator.readSettings()?.searchHostSettings?.flatMap { (searchName, searchWrapper) ->
            searchWrapper.codeHostSettings.keys.map { codeHost ->
                CodeHostSelect(searchName, codeHost)
            }
        }.orEmpty()
    },
) : ComboBox<CodeHostSelect>() {

    init {
        minimumSize = Dimension(200, 10)
    }

    data class CodeHostSelect(
        val searchHostName: String,
        val codeHostName: String,
    ) {
        override fun toString(): String = "$searchHostName / $codeHostName"
    }

    override fun getSelectedItem(): CodeHostSelect? = super.getSelectedItem() as CodeHostSelect?

    fun load() {
        super.removeAllItems()
        codeHostLoader().forEach {
            super.addItem(it)
        }
    }

    @Deprecated(message = "Supply a loader function and use the public load function instead", replaceWith = ReplaceWith(expression = "load"))
    override fun addItem(item: CodeHostSelect?) {
        super.addItem(item)
    }

    @Deprecated(message = "Supply a loader function and use the public load function instead", replaceWith = ReplaceWith(expression = "load"))
    override fun removeAllItems() {
        super.removeAllItems()
    }

    @Deprecated(message = "Supply a loader function and use the public load function instead", replaceWith = ReplaceWith(expression = "load"))
    override fun removeItem(anObject: Any?) {
        super.removeItem(anObject)
    }
}
