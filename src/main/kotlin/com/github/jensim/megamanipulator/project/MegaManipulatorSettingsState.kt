package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.jensim.megamanipulator.project.MegaManipulatorSettingsState",
    storages = [Storage("MegaManipulator.xml")]
)
class MegaManipulatorSettingsState : PersistentStateComponent<MegaManipulatorSettingsState> {

    val seenOnboarding: MutableMap<OnboardingId, Boolean> = mutableMapOf()
    var lastSearch: String = "repo:github.com/mega-manipulator/mega-manipulator"
    val prefillString: MutableMap<PrefillString, String> = mutableMapOf()

    companion object {
        fun getInstance(): MegaManipulatorSettingsState? = try {
            ApplicationManager.getApplication().getService(MegaManipulatorSettingsState::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        fun seenOnBoarding(id: OnboardingId): Boolean = getInstance()?.seenOnboarding?.put(id, true) ?: false
        fun resetOnBoarding() = getInstance()?.seenOnboarding?.clear()
    }

    override fun getState(): MegaManipulatorSettingsState {
        return this
    }

    override fun loadState(state: MegaManipulatorSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
