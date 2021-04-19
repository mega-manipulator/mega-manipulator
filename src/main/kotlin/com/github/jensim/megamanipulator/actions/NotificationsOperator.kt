package com.github.jensim.megamanipulator.actions

import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType

class NotificationsOperator(
    private val projectOperator: ProjectOperator,
    private val notificationGroup: NotificationGroup,
) {

    companion object {
        val instance: NotificationsOperator by lazy {
            NotificationsOperator(
                projectOperator = ProjectOperator.instance,
                notificationGroup = NotificationGroupManager.getInstance()
                    .getNotificationGroup("Mega Manipulator"),
            )
        }
    }

    fun show(title: String, body: String, type: NotificationType = NotificationType.INFORMATION) {
        notificationGroup.createNotification(
            title = title,
            content = body,
            type = type,
        ).notify(projectOperator.project)
    }
}
