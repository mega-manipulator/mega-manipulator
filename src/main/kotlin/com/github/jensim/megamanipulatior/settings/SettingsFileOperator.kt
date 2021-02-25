package com.github.jensim.megamanipulatior.settings

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jensim.megamanipulatior.files.FilesOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.github.jensim.megamanipulatior.settings.SerializationHolder.yamlObjectMapper
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.Charsets.UTF_8

object SettingsFileOperator {

    private val NOTIFICATION_GROUP = NotificationGroup("SettingsFileOperator", NotificationDisplayType.BALLOON, true)
    private const val settingsFileName = "mega-manipulator.yml"
    private const val scriptFileName = "mega-manipulator.bash"

    private val lastUpdated: AtomicLong = AtomicLong()
    private val bufferedSettings: AtomicReference<MegaManipulatorSettings> = AtomicReference(dummy())
    private val settingsFile: File
        get() = File("${project.basePath}", settingsFileName)
    val scriptFile: File
        get() = File("${project.basePath}", scriptFileName)

    private const val okValidationText = "Settings are valid"
    private val privateValidationText = AtomicReference("Settings are not yet validated")
    val validationText: String
        get() = privateValidationText.acquire
    private val dummyYaml: String by lazy {
        """
# Please edit this file to set up the plugin
# Removing the file will reset the file to the example state

${yamlObjectMapper.writeValueAsString(dummy())}
"""
    }

    internal fun readSettings(): MegaManipulatorSettings? = try {
        try {
            FileDocumentManager.getInstance().saveAllDocuments()
        } catch (e: Exception) {
        }
        try {
            FilesOperator.findBaseFiles().forEach { baseFile ->
                val file = File(project.basePath, baseFile.nameWithPath)
                if (!file.exists()) {
                    file.parentFile.mkdirs()
                    file.createNewFile()
                    file.writeBytes(baseFile.content)
                    if (baseFile.nameWithPath.endsWith(".bash")) {
                        file.setExecutable(true)
                    }
                } else {
                    println("File ${baseFile.nameWithPath} already exists..")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (!settingsFile.exists()) {
            println("Creating settings file")
            writeSettings(dummyYaml)
        }
        if (lastUpdated.get() == settingsFile.lastModified()) {
            bufferedSettings.get()
        } else {
            val yaml = String(settingsFile.readBytes(), UTF_8)
            val readValue: MegaManipulatorSettings? = yamlObjectMapper.readValue(yaml)
            readValue?.let {
                lastUpdated.set(settingsFile.lastModified())
                bufferedSettings.set(it)
            }
            privateValidationText.set(okValidationText)
            readValue
        }
    } catch (e: Exception) {
        e.printStackTrace()
        privateValidationText.set(e.message)
        NOTIFICATION_GROUP.createNotification("Failed settings validation: ${e.message}", NotificationType.WARNING)
            .notify(project)
        null
    }

    private fun dummy() = MegaManipulatorSettings(
        defaultHttpsOverride = HttpsOverride.ALLOW_ANYTHING,
        searchHostSettings = mapOf(
            "example-sourcegraph" to SearchHostSettingsWrapper(
                type = SearchHostType.SOURCEGRAPH,
                settings = SourceGraphSettings(
                    baseUrl = "https://sourcegraph.example.com",
                    httpsOverride = null,
                    authMethod = AuthMethod.TOKEN,
                    username = null,
                ),
                codeHostSettings = mapOf(
                    "bitbucket.example.com" to CodeHostSettingsWrapper(
                        type = CodeHostType.BITBUCKET_SERVER,
                        BitBucketSettings(
                            httpsOverride = null,
                            baseUrl = "https://bitbucket.example.com",
                            clonePattern = "ssh://git@bitbucket.example.com/{project}/{repo}.git",
                            authMethod = AuthMethod.TOKEN,
                            username = null,
                        ),
                    ),
                    "github.com" to CodeHostSettingsWrapper(
                        type = CodeHostType.GITHUB,
                        GitHubSettings(
                            httpsOverride = null,
                            baseUrl = "https://github.com",
                            clonePattern = "ssh://git@bitbucket.example.com/{project}/{repo}.git",
                            authMethod = AuthMethod.TOKEN,
                            username = null,
                        ),
                    )
                ),
            ),
        ),
    )

    private fun writeSettings(yaml: String) {
        if (!settingsFile.exists()) {
            settingsFile.createNewFile()
        }
        settingsFile.writeBytes(yaml.toByteArray(charset = UTF_8))
    }
}
