package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType.DEFAULT
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MegaManipulatorSettingsState",
    storages = [Storage("MegaManipulator.xml", roamingType = DEFAULT)],
)
class MegaManipulatorSettingsState : PersistentStateComponent<MegaManipulatorSettingsState> {

    var seenOnboarding: MutableMap<OnboardingId, Boolean> = mutableMapOf()
    var prefillStrings: MutableMap<PrefillString, MutableList<String>> = mutableMapOf()
    var seenGlobalOnboarding: Boolean = false

    fun seenOnBoarding(id: OnboardingId): Boolean = seenOnboarding[id] ?: false
    fun setOnBoardingSeen(id: OnboardingId) {
        seenOnboarding[id] = true
    }

    fun resetPrefill() {
        prefillStrings.clear()
    }
    fun resetOnBoarding() {
        seenOnboarding.clear()
        seenGlobalOnboarding = false
    }
    fun resetOnBoarding(id: OnboardingId) {
        seenOnboarding.remove(id)
        seenGlobalOnboarding = false
    }

    override fun getState(): MegaManipulatorSettingsState = this
    override fun loadState(state: MegaManipulatorSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
