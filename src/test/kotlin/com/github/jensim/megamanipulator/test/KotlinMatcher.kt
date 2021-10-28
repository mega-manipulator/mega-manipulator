package com.github.jensim.megamanipulator.test

import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher

object KotlinMatcher {

    fun <T : Any?> kMatch(match: (item: T) -> Boolean) = object : TypeSafeMatcher<T>() {
        override fun describeTo(p0: Description?) = Unit
        override fun matchesSafely(p0: T): Boolean = match(p0)
    }
}
