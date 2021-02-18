package com.github.jensim.megamanipulatior.module

import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.module.ModuleType

class MegaManipulatorModuleBuilder : ModuleBuilder() {

    override fun getModuleType(): ModuleType<MegaManipulatorModuleBuilder> {
        return MegaManipulatorModuleType()
    }
}
