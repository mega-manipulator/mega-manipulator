package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.settings.PasswordsOperator

typealias Password = String
typealias Username = String
typealias BaseUrl = String
typealias Login = Pair<Username, BaseUrl>

class TestPasswordOperator(private val passwordsMap: Map<Login, Password>): PasswordsOperator{

    override fun isPasswordSet(username: String, baseUrl: String): Boolean = passwordsMap.containsKey(username to baseUrl)
    override fun getPassword(username: String, baseUrl: String): String? = passwordsMap[username to baseUrl]
    override fun promptForPassword(username: String?, baseUrl: String): String = TODO("not implemented")
}
