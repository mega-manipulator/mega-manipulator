package com.github.jensim.megamanipulator.project

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleType
import javax.swing.Icon

class MegaManipulatorModuleType : ModuleType<MegaManipulatorModuleBuilder>("mega-manipulator") {

    companion object {
        const val MODULE_TYPE_ID: String = "mega-manipulator"
    }

    override fun createModuleBuilder(): MegaManipulatorModuleBuilder = MegaManipulatorModuleBuilder()
    override fun getName(): String = MODULE_TYPE_ID
    override fun getDescription(): String = "Configuration for the Mega Manipulator project structure"
    override fun getNodeIcon(isOpened: Boolean): Icon = AllIcons.Ide.IncomingChangesOn
}
