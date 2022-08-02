package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.actions.apply.ApplyAttempt
import com.github.jensim.megamanipulator.actions.apply.ApplyAttemptConverter
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttempt
import com.github.jensim.megamanipulator.actions.git.clone.CloneAttemptConverter
import com.github.jensim.megamanipulator.onboarding.OnboardingId
import com.github.jensim.megamanipulator.project.PrefillString
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType.DEFAULT
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.OptionTag

@State(
    name = "MegaManipulatorSettingsState",
    storages = [Storage("MegaManipulator.xml", roamingType = DEFAULT)],
)
class MegaManipulatorSettingsState : PersistentStateComponent<MegaManipulatorSettingsState> {

    var seenOnboarding: MutableMap<OnboardingId, Boolean> = mutableMapOf()
    var prefillStrings: MutableMap<PrefillString, MutableList<String>> = mutableMapOf()
    var seenGlobalOnboarding: Boolean = false
    @OptionTag(converter = CloneAttemptConverter::class)
    var cloneHistory: MutableList<CloneAttempt>? = ArrayList()
    @OptionTag(converter = ApplyAttemptConverter::class)
    var applyHistory: MutableList<ApplyAttempt>? = ArrayList()

    fun seenOnBoarding(id: OnboardingId): Boolean = seenOnboarding[id] ?: false
    fun setOnBoardingSeen(id: OnboardingId) {
        seenOnboarding[id] = true
    }

    fun addCloneAttempt(cloneAttempt: CloneAttempt) {
        cloneHistory = ArrayList(cloneHistory.orEmpty().takeLast(9) + cloneAttempt)
    }

    fun addApplyAttempt(applyAttempt: ApplyAttempt) {
        applyHistory = ArrayList(applyHistory.orEmpty().takeLast(9) + applyAttempt)
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
