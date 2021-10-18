package com.github.jensim.megamanipulator.project

import com.intellij.ide.util.PropertiesComponent

object ApplicationLevelPropertiesOperator {

    private val propertiesComponent by lazy { PropertiesComponent.getInstance() }

    fun get(flag: ApplicationLevelFlag): Boolean {
        return propertiesComponent.getValue(flag.name) == flag.ver
    }

    fun set(flag: ApplicationLevelFlag) {
        propertiesComponent.setValue(flag.name, flag.ver)
    }

    fun unset(flag: ApplicationLevelFlag) {
        propertiesComponent.setValue(flag.name, null)
    }

    enum class ApplicationLevelFlag(val ver: String) {
        GLOBAL_ONBOARDING("1")
    }
}
