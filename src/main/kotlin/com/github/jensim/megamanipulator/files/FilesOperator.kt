package com.github.jensim.megamanipulator.files

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.serviceContainer.NonInjectable
import com.intellij.util.io.inputStream
import com.intellij.util.io.isDirectory
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FilesOperator @NonInjectable constructor(
    private val project: Project,
    notificationsOperator: NotificationsOperator?,
) {
    constructor(project: Project) : this(project, null)

    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)

    class VirtFile(
        val nameWithPath: String,
        val content: ByteArray,
    )

    fun refreshConf() {
        refresh("config")
    }

    fun refreshClones() {
        refresh("clones")
    }

    private fun refresh(dir: String) {
        val projectRoot = File(project.basePath!!)
        val root = File(projectRoot, dir)
        val tree = root.walkTopDown().onEnter { it.isDirectory }.iterator().asSequence().toList()
        LocalFileSystem.getInstance().refreshIoFiles(tree + root + projectRoot)
    }

    fun makeUpBaseFiles() {
        try {
            FileDocumentManager.getInstance().saveAllDocuments()
        } catch (e: Exception) {
            System.err.println("Failed saving docs, due to: ${e.message}")
        }
        try {
            findBaseFiles("soft")
                .plus(VirtFile(".gitignore", "clones\n.idea\n".toByteArray()))
                .forEach { baseFile: VirtFile ->
                    makeUpBaseFile(baseFile, false)
                }
            findBaseFiles("hard")
                .forEach { baseFile: VirtFile ->
                    makeUpBaseFile(baseFile, true)
                }
        } catch (e: Exception) {
            notificationsOperator.show(
                title = "Failed reading base files",
                body = e.stackTrace.joinToString("<br>"),
                type = NotificationType.WARNING
            )
            e.printStackTrace()
        }
    }

    private fun makeUpBaseFile(baseFile: VirtFile, hard: Boolean) {
        val file = File(project.basePath!!, baseFile.nameWithPath)

        try {
            if (hard || !file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeBytes(baseFile.content)
                if (baseFile.nameWithPath.endsWith(".bash")) {
                    file.setExecutable(true)
                }
            } // else { println("file already exists ${file.path}") }
        } catch (e: Exception) {
            notificationsOperator.show(
                title = "Failed creating file",
                body = "${file.path}\n${e.stackTrace.joinToString("<br>")}",
                type = NotificationType.WARNING
            )
            e.printStackTrace()
        }
    }

    private fun findBaseFiles(category: String): List<VirtFile> = findAllClasspathFiles("base-files/$category")

    private fun findAllClasspathFiles(dir: String): List<VirtFile> {
        val uri: URI = FilesOperator::class.java.classLoader.getResource(dir)?.toURI()!!
        return if (uri.scheme.equals("jar")) {
            FileSystems.newFileSystem(uri, emptyMap<String, Any>()).use { fileSystem: FileSystem ->
                fileSystem.getPath("/$dir").toVirtConfFiles()
            }
        } else {
            Paths.get(uri).toVirtConfFiles()
        }
    }

    private fun Path.toVirtConfFiles(): List<VirtFile> = Files.walk(this)
        .iterator().asSequence()
        .filter { it.isDirectory().not() }
        .mapNotNull {
            val content = BufferedInputStream(it.inputStream()).use { it.readAllBytes() }
            if (content.isNotEmpty()) {
                VirtFile("config/${it.fileName}", content)
            } else {
                null
            }
        }
        .toList()
}
