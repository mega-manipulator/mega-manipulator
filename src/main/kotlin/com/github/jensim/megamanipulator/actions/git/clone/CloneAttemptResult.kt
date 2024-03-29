package com.github.jensim.megamanipulator.actions.git.clone

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput
import com.github.jensim.megamanipulator.actions.git.Action
import com.github.jensim.megamanipulator.actions.search.SearchResult

data class CloneAttemptResult(
    val repo: SearchResult,
    val branch: String,
    val actions: List<Action>,
    val success: Boolean,
) {

    constructor(repo: SearchResult, branch: String, actions: List<Action>) : this(
        repo = repo,
        branch = branch,
        actions = actions,
        success = actions.isNotEmpty() && actions.last().how.exitCode == 0
    )

    companion object Factory {
        fun fail(repo: SearchResult, branch: String, what: String, how: String) = CloneAttemptResult(
            repo = repo,
            branch = branch,
            actions = listOf(Action(what, ApplyOutput(dir = repo.asPathString(), std = how, exitCode = 1))),
            success = false
        )
    }
}
