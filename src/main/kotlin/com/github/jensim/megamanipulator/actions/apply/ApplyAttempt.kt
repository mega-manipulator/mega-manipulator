package com.github.jensim.megamanipulator.actions.apply

import java.time.LocalDateTime

data class ApplyAttempt(
    val result: List<ApplyOutput>,
    val time: LocalDateTime = LocalDateTime.now(),
)
