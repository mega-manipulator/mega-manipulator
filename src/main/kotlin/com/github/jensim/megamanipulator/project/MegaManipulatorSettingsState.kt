package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "MegaManipulatorSettingsState",
    storages = [Storage("MegaManipulator.xml")]
)
class MegaManipulatorSettingsState : PersistentStateComponent<MegaManipulatorSettingsState> {

    var seenOnboarding: MutableMap<OnboardingId, Boolean> = mutableMapOf()
    var prefillStrings: MutableMap<PrefillString, MutableList<String>> = mutableMapOf()

    fun seenOnBoarding(id: OnboardingId): Boolean = seenOnboarding[id] ?: false
    fun setOnBoardingSeen(id: OnboardingId) {
        seenOnboarding[id] = true
    }

    fun resetPrefill(): Unit = prefillStrings.clear()
    fun resetOnBoarding(): Unit = seenOnboarding.clear()
    fun resetOnBoarding(id: OnboardingId) = seenOnboarding.remove(id)

    override fun getState(): MegaManipulatorSettingsState = this
    override fun loadState(state: MegaManipulatorSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
