package com.github.jensim.megamanipulatior.settings

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.Charsets.UTF_8

object SettingsFileOperator {

    private val NOTIFICATION_GROUP = NotificationGroup("SettingsFileOperator", NotificationDisplayType.BALLOON, true)
    private const val settingsFileName = "mega-manipulator.yml"
    private const val scriptFileName = "mega-manipulator.bash"
    private const val howtoFileName = "mega-manipulator.md"
    val objectMapper: ObjectMapper by lazy {
        ObjectMapper(YAMLFactory())
            .registerKotlinModule()
    }
    private val settingsFile: File
        get() = File("${project.basePath}", settingsFileName)
    val scriptFile: File
        get() = File("${project.basePath}", scriptFileName)
    private val howToFile: File
        get() = File("${project.basePath}", howtoFileName)

    private const val okValidationText = "Settings are valid"
    private val privateValidationText = AtomicReference("Settings are not yet validated")
    val validationText: String
        get() = privateValidationText.acquire
    private val dummyYaml: String by lazy {
        """
# Please edit this file to set up the plugin
# Removing the file will reset the file to the example state

${objectMapper.writeValueAsString(dummy())}
"""
    }

    internal fun readSettings(): MegaManipulatorSettings? = try {
        try {
            FileDocumentManager.getInstance().saveAllDocuments()
        } catch (e: Exception) {
        }
        if (!settingsFile.exists()) {
            println("Creating settings file")
            writeSettings(dummyYaml)
        }
        if (!scriptFile.exists()) {
            scriptFile.createNewFile()
            scriptFile.writeBytes(
                """
                #!/bin/bash
                echo 'Hello world'
                
                """.trimIndent().toByteArray(charset = UTF_8)
            )
            scriptFile.setExecutable(true)
        }
        if (!howToFile.exists()) {
            howToFile.createNewFile()
            howToFile.writeBytes(
                """
                # Mega Manipulator
                
                One stop shop for making changes, big or small, in a lot of places at once
                
                ## How to
                
                #### 1. Validate config
                
                * [x] Validate configuration, and give user feedback
                
                #### 2. Search
                
                * [x] Search for repos using Sourcegraph
                * [ ] Search for repos using etsy/hound
                * [x] Clone repos
                * [ ] Select a branchName for all cloned repos and switch to that branch
                
                #### 3. Apply changes
                
                * [x] Write a scripted changeset in ${scriptFileName} and have it applied to all cloned repos
                
                #### 4. Commit & Push
                
                * [ ] Commit 
                
                #### 5. Create PRs
                
                * [ ] Define a title and body for all the branches, and lean back as we 
                
                #### 6. Manage PRs
                
                * [ ] Reword a bunch of PRs at the same time
                * [ ] Add reviewers
                * [ ] Remove PRs
                """.trimIndent().toByteArray(charset = UTF_8)
            )
        }
        val yaml = String(settingsFile.readBytes(), UTF_8)
        val readValue: MegaManipulatorSettings? = objectMapper.readValue(yaml)
        privateValidationText.set(okValidationText)
        readValue
    } catch (e: Exception) {
        e.printStackTrace()
        privateValidationText.set(e.message)
        NOTIFICATION_GROUP.createNotification("Failed settings validation: ${e.message}", NotificationType.WARNING)
            .notify(project)
        null
    }

    private fun dummy() = MegaManipulatorSettings(
        sourceGraphSettings = SourceGraphSettings(baseUrl = "https://sourcegraph.example.com"),
        codeHostSettings = listOf(
            CodeHostSettingsWrapper(
                type = CodeHostType.BITBUCKET_SERVER,
                BitBucketSettings(
                    baseUrl = "https://bitbucket.example.com",
                    sourceGraphName = "bitbucket",
                    clonePattern = "ssh://git@bitbucket.example.com/{project}/{repo}.git",
                )
            ),
        )
    )

    private fun writeSettings(yaml: String) {
        if (!settingsFile.exists()) {
            settingsFile.createNewFile()
        }
        settingsFile.writeBytes(yaml.toByteArray(charset = UTF_8))
    }
}
