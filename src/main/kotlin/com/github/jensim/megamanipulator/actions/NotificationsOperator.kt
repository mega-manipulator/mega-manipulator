package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType

object NotificationsOperator {

    private val NOTIFICATION_GROUP = NotificationGroup.balloonGroup("MegaManipulatorGroup")

    fun show(title: String, body: String, type: NotificationType = NotificationType.INFORMATION) {
        NOTIFICATION_GROUP.createNotification(
            title = title,
            content = body,
            type = type,
        ).notify(ProjectOperator.project)
    }
}
