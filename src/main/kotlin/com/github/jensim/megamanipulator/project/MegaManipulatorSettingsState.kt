package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
    name = "com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState",
    storages = [Storage("MegaManipulator.xml", roamingType = RoamingType.DISABLED)]
)
class MegaManipulatorSettingsState : PersistentStateComponent<MegaManipulatorSettingsState.State> {

    private var settingsState: State = State()

    data class State(
        val seenOnboarding: MutableMap<OnboardingId, Boolean> = mutableMapOf(),
        var lastSearch: String = "repo:github.com/mega-manipulator/mega-manipulator",
        val prefillString: MutableMap<PrefillString, String> = mutableMapOf(),
    )

    fun seenOnBoarding(id: OnboardingId): Boolean = state.seenOnboarding[id] ?: false
    fun setOnBoardingSeen(id: OnboardingId) {
        state.seenOnboarding[id] = true
    }
    fun resetOnBoarding() = state.seenOnboarding.clear()
    fun resetOnBoarding(id: OnboardingId) = state.seenOnboarding.remove(id)

    override fun getState(): State {
        return settingsState
    }

    override fun loadState(state: State) {
        this.settingsState = state
    }
}
