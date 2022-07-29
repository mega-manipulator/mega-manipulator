package com.github.jensim.megamanipulator.actions.git.clone

import java.time.Instant

data class CloneAttempt(
    val results: List<CloneAttemptResult>,
    val time: Instant = Instant.now(),
)
