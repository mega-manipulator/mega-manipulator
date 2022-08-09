package com.github.jensim.megamanipulator.http

import kotlinx.coroutines.Deferred

interface HttpAccessValidator {

    /**
     * Validate the access of all search/code hosts
     * The result is a Map, keyed with a pair of searchHostName to codeHostName, and the value is a async validation result.
     * A null value as the validation result is OK, and any other string value is a remark.
     * @return a map containing async validation results, where null is OK, and anything else is a remark, the keys consist of a pair of searchHostName to codeHostName
     */
    suspend fun validateTokens(): Map<Pair<String, String?>, Deferred<String?>>
}
