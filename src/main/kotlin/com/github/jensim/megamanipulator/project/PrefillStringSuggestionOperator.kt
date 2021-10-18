package com.github.jensim.megamanipulator.project

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

class PrefillStringSuggestionOperator(project: Project) {

    private val megaManipulatorSettingsState: MegaManipulatorSettingsState by lazy { project.service() }

    fun getPrefill(prefillString: PrefillString): String? {
        val prefill = megaManipulatorSettingsState.let { state ->
            state.prefillString[prefillString] ?: prefillString.fallback?.let { fallback -> state.prefillString[fallback] }
        } ?: prefillString.default
        return prefill
    }

    fun setPrefill(prefillString: PrefillString, v: String) {
        megaManipulatorSettingsState.prefillString[prefillString] = v
    }
}
