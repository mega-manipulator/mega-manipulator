package com.github.jensim.megamanipulatior.files

import java.net.JarURLConnection
import java.net.URL
import java.util.jar.JarEntry

object FilesOperator {

    class VirtFile(
        val nameWithPath: String,
        val content: ByteArray,
    )


    fun findBaseFiles(): List<VirtFile> = findAllClasspathFiles("base-files")
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
