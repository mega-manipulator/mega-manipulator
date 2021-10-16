package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState",
    storages = [Storage("MegaManipulator.xml")]
)
class MegaManipulatorSettingsState : PersistentStateComponent<MegaManipulatorSettingsState.State> {

    private var settingsState: State = State()

    data class State(
        val seenOnboarding: MutableMap<OnboardingId, Boolean> = mutableMapOf(),
        var lastSearch: String = "repo:github.com/mega-manipulator/mega-manipulator",
        val prefillString: MutableMap<PrefillString, String> = mutableMapOf(),
    )

    companion object {
        fun getInstance(): MegaManipulatorSettingsState? = try {
            ApplicationManager.getApplication().getService(MegaManipulatorSettingsState::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        fun seenOnBoarding(id: OnboardingId): Boolean = getInstance()?.state?.seenOnboarding?.get(id) ?: false
        fun setOnBoardingSeen(id: OnboardingId) { getInstance()?.state?.seenOnboarding?.set(id, true) }
        fun resetOnBoarding() = getInstance()?.state?.seenOnboarding?.clear()
    }

    override fun getState(): State {
        return settingsState
    }

    override fun loadState(state: State) {
        this.settingsState = state
    }
}
