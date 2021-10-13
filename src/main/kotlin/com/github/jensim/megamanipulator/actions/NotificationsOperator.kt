package com.github.jensim.megamanipulator.actions

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.serviceContainer.NonInjectable

class NotificationsOperator @NonInjectable constructor(
    private val project: Project,
    notificationGroup: NotificationGroup? = null,
) {

    constructor(project: Project) : this(project, null)

    private val notificationGroup: NotificationGroup by lazy {
        notificationGroup ?: NotificationGroupManager.getInstance().getNotificationGroup("Mega Manipulator")
    }

    fun show(title: String, body: String, type: NotificationType = NotificationType.INFORMATION) {
        notificationGroup.createNotification(
            title = title,
            content = body,
            type = type,
        ).notify(project)
    }
}
