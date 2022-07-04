package com.github.jensim.megamanipulator.settings

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.project.lazyService
import com.github.jensim.megamanipulator.settings.types.MegaManipulatorSettings
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.text.Charsets.UTF_8

class SettingsFileOperator @NonInjectable constructor(
    private val project: Project,
    private val settingsFileName: String = "config/mega-manipulator.json",
    private val scriptFileName: String = "config/mega-manipulator.bash",
    notificationsOperator: NotificationsOperator?,
) {

    constructor(project: Project) : this(project = project, notificationsOperator = null)

    private val notificationsOperator: NotificationsOperator by lazyService(project, notificationsOperator)

    private val lastPeek = AtomicLong(0L)
    private val lastUpdated: AtomicLong = AtomicLong(0L)
    private val bufferedSettings: AtomicReference<MegaManipulatorSettings?> = AtomicReference(null)
    private val settingsFile: File
        get() = File(project.basePath!!, settingsFileName)
    val scriptFile: File
        get() = File(project.basePath!!, scriptFileName)

    private val okValidationText = "Settings are valid"
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
                // e.printStackTrace()
            }
            if (lastUpdated.get() == settingsFile.lastModified()) {
                lastPeek.set(System.currentTimeMillis())
                bufferedSettings.get()
            } else {
                val json = String(settingsFile.readBytes(), UTF_8)
                val readValue: MegaManipulatorSettings? = SerializationHolder.objectMapper.readValue(json, MegaManipulatorSettings::class.java)
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
            notificationsOperator.show(
                title = "Failed settings validation",
                body = "Failed settings validation: ${e.message}",
                type = WARNING
            )
        }
        return bufferedSettings.get()
    }
}
