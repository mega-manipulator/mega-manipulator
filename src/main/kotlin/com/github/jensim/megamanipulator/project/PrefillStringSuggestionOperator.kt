package com.github.jensim.megamanipulator.project

import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable

class PrefillStringSuggestionOperator @NonInjectable constructor(
    project: Project,
    megaManipulatorSettingsState: MegaManipulatorSettingsState?,
) {

    private val megaManipulatorSettingsState: MegaManipulatorSettingsState by lazyService(project, megaManipulatorSettingsState)

    constructor(project: Project):this(project, null)

    fun getPrefill(prefillString: PrefillString): String? {
        return megaManipulatorSettingsState.let { state ->
            state.state.prefillString[prefillString] ?: prefillString.fallback?.let { fallback -> state.state.prefillString[fallback] }
        } ?: prefillString.default
    }

    fun setPrefill(prefillString: PrefillString, v: String) {
        megaManipulatorSettingsState.state?.prefillString?.put(prefillString, v)
    }
}
