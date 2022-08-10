package com.github.jensim.megamanipulator.project

enum class PrefillString(val fallback: PrefillString? = null, val default: String? = null, val maxHistory: Int) {

    COMMENT(
        maxHistory = 10,
    ),
    BRANCH(
        default = "feature/bulk_change",
        maxHistory = 10,
    ),
    COMMIT_MESSAGE(
        default = "Bulk changes",
        maxHistory = 10,
    ),
    PR_TITLE(
        fallback = COMMIT_MESSAGE,
        default = "Bulk changes",
        maxHistory = 10,
    ),
    PR_BODY(
        fallback = COMMIT_MESSAGE,
        default = "Bulk changes",
        maxHistory = 10,
    ),
    SEARCH(
        maxHistory = 25,
    ),
    PR_SEARCH_PROJECT(
        maxHistory = 10,
    ),
    PR_SEARCH_REPO(
        maxHistory = 10,
    ),
}
