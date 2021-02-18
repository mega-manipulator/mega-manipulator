package com.github.jensim.megamanipulatior.module

import com.github.jensim.megamanipulatior.MyBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleType
import javax.swing.Icon
import org.jetbrains.annotations.NotNull

class MegaManipulatorModuleType : ModuleType<MegaManipulatorModuleBuilder>(MODULE_TYPE_ID) {

    companion object {

        @JvmStatic
        val MODULE_TYPE_ID = "mega_manipulator"
    }

    override fun createModuleBuilder(): MegaManipulatorModuleBuilder {
        return MegaManipulatorModuleBuilder()
    }

    override fun getName(): String = MyBundle.message("name")
    override fun getDescription(): String = MyBundle.message("description")
    override fun getNodeIcon(isOpened: Boolean): @NotNull Icon = if (isOpened) {
        AllIcons.Actions.IntentionBulb
    } else {
        AllIcons.Actions.IntentionBulbGrey
    }
}
