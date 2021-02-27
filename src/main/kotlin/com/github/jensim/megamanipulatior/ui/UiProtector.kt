package com.github.jensim.megamanipulatior.ui

import com.github.jensim.megamanipulatior.actions.NotificationsOperator
import com.github.jensim.megamanipulatior.settings.ProjectOperator.project
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

fun <T> uiProtectedOperation(
    title: String,
    action: () -> T
): T? {
    val task = object : Task.WithResult<T, Exception>(project, title, false) {
        override fun compute(indicator: ProgressIndicator): T? = try {
            action()
        } catch (e: Exception) {
            NotificationsOperator.show(
                title = "Failed executing task $title",
                body = "Exception caught executing task: ${e.message}\n${e.stackTrace.joinToString("\n")}",
                type = NotificationType.ERROR
            )
            null
        }
    }
    return ProgressManager.getInstance().run(task)
}

fun uiOperation(
    title: String,
    action: () -> Unit
) {
    val task = object : Task.ConditionalModal(project, title, false, PerformInBackgroundOption { false }) {
        override fun run(indicator: ProgressIndicator) {
            try {
                action()
            } catch (e: Exception) {
                NotificationsOperator.show(
                    title = "Failed executing task $title",
                    body = "Exception caught executing task: ${e.message}\n${e.stackTrace.joinToString("\n")}",
                    type = NotificationType.ERROR
                )
            }
        }
    }
    return ProgressManager.getInstance().run(task)
}

fun <T, U> Collection<T>.mapConcurrentWithProgress(
    title: String,
    cancelable: Boolean = true,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    mappingFunction: (T) -> U
): List<Pair<T, U?>> {
    val all: Collection<T> = this
    val task = object : Task.WithResult<List<Pair<T, U?>>, Exception>(project, title, cancelable) {
        private val isCanceled = AtomicBoolean(false)

        /*
        override fun compute(indicator: ProgressIndicator): List<Pair<T,U?>> {
            return runBlocking(context = coroutineContext) {
                val futures: List<Deferred<Pair<T,U?>>?> = all.mapIndexed { index, t ->
                    if (isCanceled.get()) {
                        GlobalScope.async {Pair(t, null) }
                    }else{
                        GlobalScope.async {
                            t to try {
                                withTimeout(timeoutMillis){
                                    mappingFunction(t)
                                }
                            }catch (e:Exception){
                                null
                            }.also {
                                indicator.fraction = index.toDouble() / size
                            }
                        }
                    }
                }
                futures.filterNotNull().awaitAll()
            }
        }
        */
        override fun compute(indicator: ProgressIndicator): List<Pair<T, U?>> {
            return all.mapIndexed { index, t ->
                t to try {
                    mappingFunction(t)
                } catch (e: Exception) {
                    null
                }.also {
                    indicator.fraction = index.toDouble() / size
                }
            }
        }

        override fun onCancel() {
            super.onCancel()
            isCanceled.set(true)
            cancelText = "Canceled"
            NotificationsOperator.show(
                title = "Cancelled $title",
                body = "Cancelling operation might lead to undefined behaviour..\nYou might need to start over..",
                type = NotificationType.WARNING
            )
        }

    }
    return ProgressManager.getInstance().run(task)
}

fun <T> Collection<T>.asyncWithProgress(
    title: String,
    cancelable: Boolean = true,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    mappingFunction: suspend (T) -> Unit
) {
    val all: Collection<T> = this
    val task = object : Task.Backgroundable(project, title, cancelable) {
        private val isCanceled = AtomicBoolean(false)
        override fun run(indicator: ProgressIndicator) {
            runBlocking(context = coroutineContext) {
                val futures: List<Deferred<Unit>?> = all.mapIndexed { index, t ->
                    if (isCanceled.get()) {
                        return@mapIndexed null
                    }
                    GlobalScope.async {
                        mappingFunction(t).also {
                            indicator.fraction = index.toDouble() / size
                        }
                    }
                }
                futures.filterNotNull().awaitAll()
            }
        }

        override fun onCancel() {
            super.onCancel()
            isCanceled.set(true)
        }
    }
    BackgroundableProcessIndicator(task)
}
