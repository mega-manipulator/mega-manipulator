package com.github.jensim.megamanipulator.settings

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator.project
import com.github.jensim.megamanipulator.settings.SerializationHolder.yamlObjectMapper
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.fileEditor.FileDocumentManager
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.Charsets.UTF_8

object SettingsFileOperator {

    private const val settingsFileName = "config/mega-manipulator.yml"
    private const val scriptFileName = "config/mega-manipulator.bash"

    private val lastPeek = AtomicLong(0L)
    private val lastUpdated: AtomicLong = AtomicLong(0L)
    private val bufferedSettings: AtomicReference<MegaManipulatorSettings?> = AtomicReference(null)
    private val settingsFile: File
        get() = File(project?.basePath!!, settingsFileName)
    val scriptFile: File
        get() = File(project?.basePath!!, scriptFileName)

    private const val okValidationText = "Settings are valid"
    private val privateValidationText = AtomicReference("Settings are not yet validated")
    val validationText: String
        get() = privateValidationText.acquire

    internal fun readSettings(): MegaManipulatorSettings? {
        if (System.currentTimeMillis() - lastPeek.get() < 250) {
            return bufferedSettings.get()
        }
        try {
            try {
                FileDocumentManager.getInstance().saveAllDocuments()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (!settingsFile.exists()) {
                println("Creating settings file")
                FilesOperator.makeUpBaseFiles()
            }
            if (lastUpdated.get() == settingsFile.lastModified()) {
                lastPeek.set(System.currentTimeMillis())
                bufferedSettings.get()
            } else {
                val yaml = String(settingsFile.readBytes(), UTF_8)
                val readValue: MegaManipulatorSettings? = yamlObjectMapper.readValue(yaml)
                readValue?.let {
                    lastUpdated.set(settingsFile.lastModified())
                    bufferedSettings.set(it)
                    lastPeek.set(System.currentTimeMillis())
                }
                privateValidationText.set(okValidationText)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            privateValidationText.set(e.message)
            NotificationsOperator.show(
                title = "Failed settings validation",
                body = "Failed settings validation: ${e.message}",
                type = WARNING
            )
        }
        return bufferedSettings.get()
    }
}
