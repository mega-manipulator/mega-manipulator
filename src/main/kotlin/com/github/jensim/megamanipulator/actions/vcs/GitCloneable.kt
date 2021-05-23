package com.github.jensim.megamanipulator.actions.vcs

import com.github.jensim.megamanipulator.settings.types.CloneType

interface GitCloneable {

    fun project(): String
    fun baseRepo(): String
    fun cloneUrlFrom(cloneType: CloneType): String?
    fun cloneUrlTo(cloneType: CloneType): String?
    fun isFork(): Boolean
}
