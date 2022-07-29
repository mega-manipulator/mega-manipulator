package com.github.jensim.megamanipulator.actions.git

import com.github.jensim.megamanipulator.actions.apply.ApplyOutput

data class Action(
    val what: String,
    val how: ApplyOutput,
)
