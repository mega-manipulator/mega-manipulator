package com.github.jensim.megamanipulator.project

import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project

object MegaManipulatorUtil {

    fun isMM(project: Project) = ModuleManager.getInstance(project).modules.any {
        ModuleType.get(it).id == MegaManipulatorModuleType.MODULE_TYPE_ID
    }
}
