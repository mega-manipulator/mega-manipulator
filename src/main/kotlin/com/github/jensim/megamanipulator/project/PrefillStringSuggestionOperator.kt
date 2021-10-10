package com.github.jensim.megamanipulator.project

object PrefillStringSuggestionOperator {

    fun getPrefill(prefillString: PrefillString): String? {
        return MegaManipulatorSettingsState.getInstance()?.let { state ->
            state.prefillString[prefillString] ?: prefillString.fallback?.let { fallback -> state.prefillString[fallback] }
        } ?: prefillString.default
    }

    fun setPrefill(prefillString: PrefillString, v: String) {
        MegaManipulatorSettingsState.getInstance()?.prefillString?.put(prefillString, v)
    }
}
