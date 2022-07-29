package com.github.jensim.megamanipulator.actions.apply

import com.github.jensim.megamanipulator.actions.search.SearchResult
import java.time.Instant

data class ApplyAttempt(
    val result: Map<SearchResult, ApplyOutput>,
    val time: Instant = Instant.now(),
)
