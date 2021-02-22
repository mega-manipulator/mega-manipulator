package com.github.jensim.megamanipulatior.actions

import com.github.jensim.megamanipulatior.settings.ProjectOperator
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

object NotificationsOperator {
    private val NOTIFICATION_GROUP = NotificationGroup("MegaManipulatorGroup", NotificationDisplayType.BALLOON, true)

    fun show(title: String, body: String, type: NotificationType = NotificationType.INFORMATION) {
        NOTIFICATION_GROUP.createNotification(
            title = title,
            content = body,
            type = type,
        ).notify(ProjectOperator.project)
    }
}
