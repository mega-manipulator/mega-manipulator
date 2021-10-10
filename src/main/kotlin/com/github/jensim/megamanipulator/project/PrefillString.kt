package com.github.jensim.megamanipulator.project

enum class PrefillString(val fallback: PrefillString? = null, val default: String? = null) {

    BRANCH(default = "feature/bulk_change"),
    COMMIT_MESSAGE(default = "Bulk changes"),
    PR_TITLE(fallback = COMMIT_MESSAGE, default = "Bulk changes"),
    PR_BODY(fallback = COMMIT_MESSAGE, default = "Bulk changes"),
}
