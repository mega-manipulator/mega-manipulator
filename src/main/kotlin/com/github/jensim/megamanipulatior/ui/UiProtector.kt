package com.github.jensim.megamanipulatior.ui

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import java.util.concurrent.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout

inline fun <T> uiProtectedOperation(
    title: String,
    crossinline action: suspend () -> T
): T? {
    val task = object : Task.WithResult<T, Exception>(project, title, true) {
        override fun compute(indicator: ProgressIndicator): T? = runBlocking {
            indicator.isIndeterminate = true
            try {
                val deferred = async {
                    try {
                        action()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                while (indicator.isRunning && !indicator.isCanceled) {
                    try {
                        return@runBlocking withTimeout(250) {
                            deferred.await()
                        }
                    } catch (e: Exception) {
                        // Just not done yet
                    }
                }
                try {
                    deferred.getCompleted()
                } catch (e: Exception) {
                    try {
                        deferred.cancel(cause = CancellationException("Action cancelled"))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    throw e
                }
            } catch (e: Exception) {
                NotificationsOperator.show(
                    title = "Failed executing task $title",
                    body = "Exception caught executing task: ${e.message}\n${e.stackTrace.joinToString("\n")}",
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
        NotificationsOperator.show(
            title = "Exception running task $title",
            body = "${e.message}\n${e.stackTrace.joinToString("\n")}"
        )
        null
    }
}

inline fun <T, U> Collection<T>.mapConcurrentWithProgress(
    title: String,
    extraText1: String? = null,
    crossinline extraText2: (T) -> String? = { null },
    concurrent: Int = 5,
    crossinline mappingFunction: suspend (T) -> U
): List<Pair<T, U?>> {
    val all: Collection<T> = this
    val task = object : Task.WithResult<List<Pair<T, U?>>, Exception>(project, title, true) {
        override fun compute(indicator: ProgressIndicator): List<Pair<T, U?>> {
            indicator.isIndeterminate = false
            extraText1?.let { text ->
                indicator.text = text
            }

            indicator.fraction = 0.0
            return runBlocking {
                val semaphore = Semaphore(permits = concurrent)
                val futures: List<Pair<T, Deferred<U?>?>> = all.mapIndexed { index, t ->
                    t to GlobalScope.async {
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
                            null
                        } finally {
                            semaphore.release()
                            indicator.fraction = (index + 1.0) / size
                        }
                    }
                }
                // Try gracefully
                while (indicator.isRunning && !indicator.isCanceled) {
                    try {
                        return@runBlocking withTimeout(250) {
                            futures.map { it.first to it.second?.await() }
                        }
                    } catch (e: Exception) {
                        // Just not done yet
                    }
                }
                // Try last chance
                try {
                    futures.map {
                        it.first to try {
                            it.second?.getCompleted()
                        } catch (e: Exception) {
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
                    all.map { it to null }
                }
            }
        }
    }
    return try {
        ProgressManager.getInstance().run(task)
    } catch (e: Exception) {
        e.printStackTrace()
        NotificationsOperator.show(
            title = "Exception running task $title",
            body = "${e.message}\n${e.stackTrace.joinToString("\n")}"
        )
        all.map { it to null }
    }
}

private fun <T> Collection<T>.asyncWithProgress(
    title: String,
    concurrent: Int = 5,
    mappingFunction: suspend (T) -> Unit
) {
    TODO("Need to implement better wait func")
    val all: Collection<T> = this
    val task = object : Task.Backgroundable(project, title, true) {
        override fun run(indicator: ProgressIndicator) {
            indicator.isIndeterminate = false
            indicator.fraction = 0.0
            runBlocking {
                val semaphore = Semaphore(permits = concurrent)
                val futures: List<Deferred<Unit>> = all.mapIndexed { index, t ->
                    GlobalScope.async {
                        try {
                            if (!indicator?.isCanceled) {
                                semaphore.acquire()
                                indicator.fraction = (index + 1.0) / size
                                mappingFunction(t)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            semaphore.release()
                        }
                    }
                }
                // TODO watchForCancellation(futures, indicator)
                try {
                    futures.awaitAll()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    val indicator = BackgroundableProcessIndicator(task)
    ProgressManager.getInstance().runProcessWithProgressAsynchronously(task, indicator)
}

fun String.fixedLength(len: Int) = take(len).padEnd(len, ' ')
