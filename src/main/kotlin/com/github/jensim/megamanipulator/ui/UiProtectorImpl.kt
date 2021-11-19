package com.github.jensim.megamanipulator.ui

import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class UiProtectorImpl(
    private val project: Project,
) : UiProtector {

    private val notificationsOperator: NotificationsOperator by lazy { project.service() }
    private val processOperator: ProcessOperator by lazy { project.service() }
    private val coroutineCntx: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override fun <T> uiProtectedOperation(
        title: String,
        action: suspend () -> T
    ): T? {
        val task = object : Task.WithResult<T, Exception>(project, title, true) {
            override fun compute(indicator: ProgressIndicator): T? = runBlocking {
                processOperator.clearPids()
                indicator.isIndeterminate = true
                try {
                    val deferred = async(context = coroutineCntx) {
                        try {
                            action()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            notificationsOperator.show(
                                title = "Failed executing task $title",
                                body = "Exception caught executing task:<br>${e.message}<br>${
                                e.stackTrace.joinToString(
                                    "<br>"
                                )
                                }",
                                type = NotificationType.ERROR
                            )
                            null
                        }
                    }
                    while (indicator.isRunning && !indicator.isCanceled) {
                        try {
                            return@runBlocking withTimeout(250) {
                                deferred.await()
                            }
                        } catch (e: TimeoutCancellationException) {
                            // Just not done yet
                        }
                    }
                    try {
                        if (indicator.isCanceled) {
                            processOperator.stopAll()
                        }
                        withTimeout(250) {
                            deferred.await()
                        }
                    } catch (e: Exception) {
                        try {
                            deferred.cancel(cause = CancellationException("Action cancelled"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        throw e
                    }
                } catch (e: Exception) {
                    notificationsOperator.show(
                        title = "Failed executing task $title",
                        body = "Exception caught executing task: ${e.message}<br>${e.stackTrace.joinToString("<br>")}",
                        type = NotificationType.ERROR
                    )
                    null
                }
            }
        }
        return try {
            ProgressManager.getInstance().run(task)
        } catch (e: Exception) {
            e.printStackTrace()
            notificationsOperator.show(
                title = "Exception running task $title",
                body = "${e.message}\n${e.stackTrace.joinToString("<br>")}"
            )
            null
        }
    }

    override fun <T, U> mapConcurrentWithProgress(
        title: String,
        extraText1: String?,
        extraText2: (T) -> String?,
        concurrent: Int,
        data: Collection<T>,
        mappingFunction: suspend (T) -> U
    ): List<Pair<T, U?>> {

        val task = object : Task.WithResult<List<Pair<T, U?>>, Exception>(project, title, true) {
            @Suppress("LongMethod")
            override fun compute(indicator: ProgressIndicator): List<Pair<T, U?>> {
                processOperator.clearPids()
                indicator.isIndeterminate = false
                extraText1?.let { text ->
                    indicator.text = text
                }

                indicator.fraction = 0.0
                return runBlocking {
                    val semaphore = Semaphore(permits = concurrent)
                    val futures: List<Pair<T, Deferred<U?>?>> = data.mapIndexed { index, t ->
                        t to async(context = coroutineCntx) {
                            try {
                                if (!indicator.isCanceled) {
                                    semaphore.acquire()
                                    extraText2(t)?.let { text ->
                                        indicator.text2 = text
                                    }
                                    mappingFunction(t)
                                } else {
                                    null
                                }
                            } catch (e: Exception) {
                                notificationsOperator.show(
                                    title = "Failed with [$index]",
                                    body = "${e.message}\n${e.stackTrace.joinToString("<br>")}",
                                    type = NotificationType.ERROR
                                )
                                null
                            } finally {
                                semaphore.release()
                                indicator.fraction = (index + 1.0) / data.size
                            }
                        }
                    }
                    // Try gracefully
                    while (indicator.isRunning && !indicator.isCanceled) {
                        try {
                            return@runBlocking withTimeout(250) {
                                futures.map { it.first to it.second?.await() }
                            }
                        } catch (e: TimeoutCancellationException) {
                            // Just not done yet
                        }
                    }
                    // Try last chance
                    try {
                        if (indicator.isCanceled) {
                            processOperator.stopAll()
                        }
                        val first = AtomicBoolean(true)
                        futures.map {
                            it.first to try {
                                if (first.getAndSet(false)) {
                                    withTimeout(250) {
                                        it.second?.await()
                                    }
                                } else {
                                    it.second?.getCompleted()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                try {
                                    it.second?.cancel(cause = CancellationException("Job not completed in time or cancelled"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                null
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        data.map { it to null }
                    }
                }
            }
        }
        return try {
            ProgressManager.getInstance().run(task)
        } catch (e: Exception) {
            e.printStackTrace()
            notificationsOperator.show(
                title = "Exception running task $title",
                body = "${e.message}\n${e.stackTrace.joinToString("<br>")}"
            )
            data.map { it to null }
        }
    }
}
