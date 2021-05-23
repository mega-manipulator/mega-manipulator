package com.github.jensim.megamanipulator.actions.vcs.gitlab

import com.github.jensim.megamanipulator.actions.vcs.GitCloneable
import com.github.jensim.megamanipulator.actions.vcs.GitLabRepoWrapping
import com.github.jensim.megamanipulator.settings.types.CloneType

data class GitLabGitCloneableRepoWrapper(
    val source: GitLabRepoWrapping,
    val target: GitLabRepoWrapping
) : GitCloneable {
    override fun project(): String = target.getProject()
    override fun baseRepo(): String = target.getRepo()
    override fun cloneUrlFrom(cloneType: CloneType): String? = source.getCloneUrl(cloneType)
    override fun cloneUrlTo(cloneType: CloneType): String? = target.getCloneUrl(cloneType)
    override fun isFork(): Boolean = target.repo.fullPath != source.repo.fullPath
}
