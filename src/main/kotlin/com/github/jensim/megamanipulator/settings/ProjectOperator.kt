package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.project.MegaManipulatorModuleType.Companion.MODULE_TYPE_ID
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.content.ContentFactory
import java.io.File

class ProjectOperator {

    companion object {
        val instance by lazy { ProjectOperator() }
    }

    private var projectPrivate: Project? = null
    var project: Project?
        get() = projectPrivate ?: projectBackup()
        set(value) {
            if (value != null) {
                projectPrivate = value
            }
        }

    private fun projectBackup(): Project? = try {
        ProjectManager.getInstance().openProjects.firstOrNull()
    } catch (e: NullPointerException) {
        null
    }

    val moduleRootManager: ModuleRootManager?
        get() = ModuleManager.getInstance(project!!).modules.filter { it.moduleTypeName == MODULE_TYPE_ID }.let { modules ->
            if (modules.size == 1) {
                ModuleRootManager.getInstance(modules.first())
            } else {
                null
            }
        }

    val contentFactory: ContentFactory
        get() = ContentFactory.SERVICE.getInstance()

    fun toggleExcludeClones() {
        moduleRootManager?.let { manager ->
            val model: ModifiableRootModel = manager.modifiableModel
            try {
                val rootFile = LocalFileSystem.getInstance().findFileByIoFile(File(project?.basePath!!))!!
                val clonesFile = LocalFileSystem.getInstance().findFileByIoFile(File(project?.basePath!!, "clones"))!!
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
