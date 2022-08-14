package com.github.jensim.megamanipulator.settings.types.codehost

import com.github.jensim.megamanipulator.graphql.generated.gitlab.enums.MergeRequestState
import com.github.jensim.megamanipulator.graphql.generated.gitlab.enums.MergeRequestState.OPENED
import com.github.jensim.megamanipulator.graphql.generated.gitlab.enums.MergeRequestState.__UNKNOWN_VALUE

enum class CodeHostSettingsType(
    val prRoleAuthor: String,
    val prRoleAssignee: String,
    val prRoles: Set<String?>,
    val prStateOpen: String,
    val prStates: Set<String?>,
) {
    BITBUCKET_SERVER(
        prRoleAuthor = "AUTHOR",
        prRoleAssignee = "REVIEWER",
        prRoles = setOf("AUTHOR", "REVIEWER", "PARTICIPANT", null),
        prStateOpen = "OPEN",
        prStates = setOf("OPEN", "DECLINED", "MERGED", null),
    ),
    GITHUB(
        prRoleAuthor = "author",
        prRoleAssignee = "assignee",
        prRoles = setOf("assignee", "author", "commenter", null),
        prStateOpen = "open",
        prStates = setOf("open", "closed", null),
    ),
    GITLAB(
        prRoleAuthor = "author",
        prRoleAssignee = "assignee",
        prRoles = setOf("assignee", "author", null),
        prStateOpen = OPENED.name,
        prStates = (MergeRequestState.values().toSet() - __UNKNOWN_VALUE).map { it.name }.toSet(),
    );
}
