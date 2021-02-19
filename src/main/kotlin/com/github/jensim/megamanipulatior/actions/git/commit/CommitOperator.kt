package com.github.jensim.megamanipulatior.actions.git.commit

import kotlinx.coroutines.delay

object CommitOperator {

    suspend fun commit() {
        delay(1)

        TODO(
            """
            commit 
            push?
        """.trimIndent()
        )
    }

    private suspend fun push() {
        delay(1)
        TODO()
    }
}