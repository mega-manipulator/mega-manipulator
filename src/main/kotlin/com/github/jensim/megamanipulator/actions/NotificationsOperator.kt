package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

class NotificationsOperator(
    private val projectOperator: ProjectOperator
) {

    companion object {
        val instance: NotificationsOperator by lazy {
            NotificationsOperator(
                projectOperator = ProjectOperator.instance
            )
        }
    }

    private val notificationGroup = NotificationGroup.balloonGroup("MegaManipulatorGroup")

    fun show(title: String, body: String, type: NotificationType = NotificationType.INFORMATION) {
        notificationGroup.createNotification(
            title = title,
            content = body,
            type = type,
        ).notify(projectOperator.project)
    }
}
