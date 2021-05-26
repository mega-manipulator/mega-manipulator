package com.github.jensim.megamanipulator.settings.passwords

interface PasswordsOperator {

    fun isPasswordSet(username: String, baseUrl: String): Boolean
    fun getPassword(username: String, baseUrl: String): String?
    fun promptForPassword(username: String?, baseUrl: String): String

    @Suppress("ComplexMethod")
    fun aggressivePercentEncoding(word: String): String = word.map {
        when (it) {
            ' ' -> "%20"
            '!' -> "%21"
            '#' -> "%23"
            '$' -> "%24"
            '&' -> "%26"
            '\'' -> "%27"
            '(' -> "%28"
            ')' -> "%29"
            '*' -> "%2A"
            '+' -> "%2B"
            ',' -> "%2C"
            '/' -> "%2F"
            ':' -> "%3A"
            ';' -> "%3B"
            '=' -> "%3D"
            '?' -> "%3F"
            '@' -> "%4D"
            '[' -> "%5B"
            ']' -> "%5D"
            else -> "$it"
        }
    }.joinToString(separator = "")
}
