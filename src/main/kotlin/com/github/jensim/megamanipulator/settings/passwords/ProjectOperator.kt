package com.github.jensim.megamanipulator.settings.passwords

import com.github.jensim.megamanipulator.project.MegaManipulatorModuleType.Companion.MODULE_TYPE_ID
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

class ProjectOperator(val project: Project) {

    private val moduleRootManager: ModuleRootManager?
        get() = ModuleManager.getInstance(project).modules.filter { ModuleType.get(it).id == MODULE_TYPE_ID }.let { modules ->
            if (modules.size == 1) {
                ModuleRootManager.getInstance(modules.first())
            } else {
                null
            }
        }

    fun toggleExcludeClones() {
        moduleRootManager?.let { manager ->
            val model: ModifiableRootModel = manager.modifiableModel
            try {
                val rootFile = LocalFileSystem.getInstance().findFileByIoFile(File(project.basePath!!))!!
                val clonesFile = LocalFileSystem.getInstance().findFileByIoFile(File(project.basePath!!, "clones"))!!
                val rootEntry = model.contentEntries.first { it.file == rootFile }
                val excludeFolders = rootEntry.excludeFolders.filter { it.file == clonesFile }
                if (excludeFolders.isNotEmpty()) {
                    excludeFolders.forEach {
                        rootEntry.removeExcludeFolder(it)
                    }
                    rootEntry.addSourceFolder(clonesFile, true)
                } else {
                    rootEntry.addExcludeFolder(clonesFile)
                    rootEntry.sourceFolders.filter { it.file == clonesFile }.forEach {
                        rootEntry.removeSourceFolder(it)
                    }
                }
                ApplicationManager.getApplication().runWriteAction {
                    model.commit()
                }
            } catch (e: Throwable) {
                ApplicationManager.getApplication().runWriteAction {
                    model.dispose()
                }
            }
        }
    }
}
