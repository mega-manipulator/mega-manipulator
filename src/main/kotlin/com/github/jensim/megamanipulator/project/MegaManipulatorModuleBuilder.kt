package com.github.jensim.megamanipulator.project

import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.intellij.ide.util.projectWizard.ModuleBuilder
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl
import com.intellij.openapi.module.ModuleType
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

@SuppressWarnings("TooManyFunctions")
class MegaManipulatorModuleBuilder : ModuleBuilder() {

    companion object {
        val MODULE_TYPE: ModuleType<MegaManipulatorModuleBuilder> by lazy { MegaManipulatorModuleType() }
    }

    override fun getModuleType(): ModuleType<MegaManipulatorModuleBuilder> = MODULE_TYPE

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        ProjectOperator.project = modifiableRootModel.project
        val contentEntryFile = createAndGetContentEntry()
        val contentEntry = modifiableRootModel.addContentEntry(contentEntryFile)

        FilesOperator.makeUpBaseFiles()

        val confDir = createDirIfNotExists(modifiableRootModel.project, "config")
        contentEntry.addSourceFolder(confDir, false)

        val clonesDir = createDirIfNotExists(modifiableRootModel.project, "clones")
        contentEntry.addExcludeFolder(clonesDir)

        super.setupRootModel(modifiableRootModel)
    }

    private fun createDirIfNotExists(project: Project, dir: String): VirtualFile {
        val root = File(project.basePath!!)
        val refreshAndFindFileByIoFile: VirtualFile? = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(project.basePath, dir))
        return if (refreshAndFindFileByIoFile?.exists() != true) {
            val virtRoot = LocalFileSystem.getInstance().findFileByIoFile(root)!!
            LocalFileSystem.getInstance().createChildDirectory(null, virtRoot, dir)
            LocalFileSystem.getInstance().refreshAndFindFileByIoFile(File(project.basePath, dir))!!
        } else {
            refreshAndFindFileByIoFile
        }
    }

    override fun isOpenProjectSettingsAfter(): Boolean = false
    override fun canCreateModule(): Boolean = true
    override fun getPresentableName(): String = "Mega Manipulator"
    override fun getGroupName(): String = presentableName
    override fun isTemplateBased(): Boolean = true

    override fun getDescription(): String = """Search and replace:<br>
        |Replace make large scale changes to your source code across multiple repos<br>
        |&nbsp;* Search - using SourceGraph OSS<br>
        |&nbsp;* Replace - using scrips or doing changes manually<br>
        |&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;My personal favorite is comby.dev, try it out and fall in love!""".trimMargin()

    private fun createAndGetContentEntry(): VirtualFile {
        val path = FileUtil.toSystemIndependentName(this.contentEntryPath!!)
        File(path).mkdirs()
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path)!!
    }

    override fun createProject(name: String?, path: String?): Project? {
        return ExternalProjectsManagerImpl.setupCreatedProject(super.createProject(name, path))
    }
}
