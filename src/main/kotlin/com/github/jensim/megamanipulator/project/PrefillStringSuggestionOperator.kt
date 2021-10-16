package com.github.jensim.megamanipulator.project

object PrefillStringSuggestionOperator {

    fun getPrefill(prefillString: PrefillString): String? {
        return MegaManipulatorSettingsState.getInstance()?.let { state ->
            state.state.prefillString[prefillString] ?: prefillString.fallback?.let { fallback -> state.state.prefillString[fallback] }
        } ?: prefillString.default
    }

    fun setPrefill(prefillString: PrefillString, v: String) {
        MegaManipulatorSettingsState.getInstance()?.state?.prefillString?.put(prefillString, v)
    }
}
