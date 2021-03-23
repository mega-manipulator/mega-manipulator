package com.github.jensim.megamanipulatior.files

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.File
import java.net.JarURLConnection
import java.net.URL
import java.util.jar.JarEntry

object FilesOperator {

    class VirtFile(
        val nameWithPath: String,
        val content: ByteArray,
    )

    fun makeUpBaseFiles() {
        try {
            FileDocumentManager.getInstance().saveAllDocuments()
        } catch (e: Exception) {
            System.err.println("Failed saving docs, due to: ${e.message}")
        }
        try {
            findBaseFiles().forEach { baseFile: VirtFile ->
                makeUpBaseFile(baseFile)
            }
        } catch (e: Exception) {
            NotificationsOperator.show(
                title = "Failed reading base files",
                body = e.stackTrace.joinToString("\n"),
                type = NotificationType.WARNING
            )
            e.printStackTrace()
        }
    }

    private fun makeUpBaseFile(baseFile: VirtFile) {
        val file = File(ProjectOperator.project.basePath, baseFile.nameWithPath)
        try {
            if (!file.exists()) {
                file.parentFile.mkdirs()
                file.createNewFile()
                file.writeBytes(baseFile.content)
                if (baseFile.nameWithPath.endsWith(".bash")) {
                    file.setExecutable(true)
                }
            } else {
                println("file already exists ${file.path}")
            }
        } catch (e: Exception) {
            NotificationsOperator.show(
                title = "Failed creating file",
                body = "${file.path}\n${e.stackTrace.joinToString("\n")}",
                type = NotificationType.WARNING
            )
            e.printStackTrace()
        }
    }

    private fun findBaseFiles(): List<VirtFile> = findAllClasspathFiles("base-files")
        .plus(VirtFile(".gitignore", "clones\n".toByteArray()))

    private fun findAllClasspathFiles(dir: String): List<VirtFile> {
        val resource: URL? = FilesOperator::class.java.getResource("/$dir/")
        return if (resource?.protocol == "jar") {
            (resource.openConnection() as JarURLConnection).jarFile.entries().toList()
                .filter { it.name.startsWith("$dir/") }
                .mapNotNull { file: JarEntry ->
                    FilesOperator::class.java.classLoader.getResourceAsStream(file.name)?.readAllBytes()?.let { content ->
                        if (content.isNotEmpty()) {
                            val name = file.name.removePrefix("$dir/")
                            VirtFile(name, content)
                        } else {
                            null
                        }
                    }
                }
        } else {
            System.exit(1)
            emptyList()
        }
    }
}
