package com.github.jensim.megamanipulator.ui

import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory

internal class TestUiProtector : UiProtector {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun <T> uiProtectedOperation(title: String, action: suspend () -> T): T? {
        return runBlocking {
            try {
                action()
            } catch (e: Throwable) {
                log.warn("Exception caught", e)
                null
            }
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
        return runBlocking {
            data.map {
                it to try {
                    mappingFunction(it)
                } catch (e: Throwable) {
                    log.warn("Exception caught", e)
                    null
                }
            }
        }
    }
}
