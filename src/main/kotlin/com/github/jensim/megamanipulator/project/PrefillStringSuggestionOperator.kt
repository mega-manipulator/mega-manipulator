package com.github.jensim.megamanipulator.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class PrefillStringSuggestionOperator(project: Project) {

    private val megaManipulatorSettingsState: MegaManipulatorSettingsState by lazy { project.service() }

    fun getPrefills(prefillString: PrefillString): List<String> = (
        megaManipulatorSettingsState.let { state ->
            state.prefillStrings[prefillString] ?: prefillString.fallback?.let {
                state.prefillStrings[it]
            }
        } ?: listOf(prefillString.default)
        ).filterNotNull()

    fun getPrefill(prefillString: PrefillString): String? {
        val prefill = megaManipulatorSettingsState.let { state ->
            state.prefillStrings[prefillString]?.lastOrNull() ?: prefillString.fallback
                ?.let { state.prefillStrings[it]?.lastOrNull() }
        } ?: prefillString.default
        return prefill
    }

    fun addPrefill(prefillString: PrefillString, v: String) {
        val list: MutableList<String> = megaManipulatorSettingsState.prefillStrings.computeIfAbsent(prefillString) { mutableListOf() }
        list.removeIf { it == v }
        list.add(v)
        (list.size - prefillString.maxHistory).let { excess ->
            if (excess > 0) {
                list.drop(excess)
            }
        }
    }

    fun removePrefill(prefillString: PrefillString, v: String) {
        megaManipulatorSettingsState.prefillStrings[prefillString]?.let { l ->
            l.removeIf { it == v }
        }
    }
}
