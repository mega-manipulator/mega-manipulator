package com.github.jensim.megamanipulator.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "MegaManipulatorApplicationSettings", storages = [Storage("MegaManipulatorApplication.xml")])
class MegaManipulatorApplicationSettings : PersistentStateComponent<MegaManipulatorApplicationSettings> {

    var seenGlobalOnboarding: Boolean = false

    override fun getState(): MegaManipulatorApplicationSettings = this

    override fun loadState(state: MegaManipulatorApplicationSettings) = XmlSerializerUtil.copyBean(state, this)
}
