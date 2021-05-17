package com.github.jensim.megamanipulator.settings.passwords

interface PasswordsOperator {

    fun isPasswordSet(username: String, baseUrl: String): Boolean
    fun getPassword(username: String, baseUrl: String): String?
    fun promptForPassword(username: String?, baseUrl: String): String
}
