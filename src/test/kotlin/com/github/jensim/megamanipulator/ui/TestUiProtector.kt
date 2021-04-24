package com.github.jensim.megamanipulator.ui

import kotlinx.coroutines.runBlocking

internal class TestUiProtector : UiProtector {

    override fun <T> uiProtectedOperation(title: String, action: suspend () -> T): T? {
        return runBlocking { action() }
    }

    override fun <T, U> mapConcurrentWithProgress(
        title: String,
        extraText1: String?,
        extraText2: (T) -> String?,
        concurrent: Int,
        data: Collection<T>,
        mappingFunction: suspend (T) -> U
    ): List<Pair<T, U?>> {
        return runBlocking { data.map { it to mappingFunction(it) } }
    }
}
