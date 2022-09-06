package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.settings.MegaManipulatorSettingsState
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class PrefillStringSuggestionOperator(project: Project) {

    private val megaManipulatorSettingsState: MegaManipulatorSettingsState by lazy { project.service() }

    /**
     * Returns a list of previously added prefills.
     * The first entry in the list is the most recent addition.
     *
     * First it will try with the given PrefillString, then the fallback, and last the default.
     * The three are mutually exclusive, and will not be concatenated into one list.
     */
    fun getPrefills(prefillString: PrefillString): List<String> = (
        megaManipulatorSettingsState.let { state ->
            state.prefillStrings[prefillString] ?: prefillString.fallback?.let { prefillFallback ->
                state.prefillStrings[prefillFallback]
            }
        } ?: listOf(prefillString.default)
        ).filterNotNull().reversed()

    /**
     * Returns the most recently added prefill.
     * First it will try with the given PrefillString, then the fallback, and last the default
     */
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

    fun resetPrefill() {
        megaManipulatorSettingsState.prefillStrings.clear()
    }

    fun removePrefill(prefillString: PrefillString, v: String) {
        megaManipulatorSettingsState.prefillStrings[prefillString]?.let { l ->
            l.removeIf { it == v }
        }
    }
}
