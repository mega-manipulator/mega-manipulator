package com.github.jensim.megamanipulator.ui

interface UiProtector {

    fun <T> uiProtectedOperation(title: String, action: suspend () -> T): T?

    fun <T, U> mapConcurrentWithProgress(
        title: String,
        extraText1: String? = null,
        extraText2: (T) -> String? = { null },
        concurrent: Int = 1,
        data: Collection<T>,
        mappingFunction: suspend (T) -> U
    ): List<Pair<T, U?>>
}
