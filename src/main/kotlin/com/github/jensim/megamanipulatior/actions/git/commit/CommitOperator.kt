package com.github.jensim.megamanipulatior.actions.git.commit

import com.github.jensim.megamanipulatior.ui.DialogGenerator
import java.io.File

object CommitOperator {

    fun commit(): Map<String, String> {
        val commitMessageKey = "Commit message"
        DialogGenerator.askForInput(
            title = "Create commits",
            message = "Create commits for all changes in all checked out repositories",
            values = listOf(commitMessageKey),
            onOk = {
                //TODO
                var push: Boolean = false
                DialogGenerator.showConfirm("TODO", "Also push? ${it[commitMessageKey]}") {
                    push = true
                }
                println("psuh:$push")
            },
            onCancel = {
                DialogGenerator.showConfirm("Info", "No commit performed!") {}
            }
        )
        //push()
        return mapOf("Test" to "commited and pushed!")
    }

    private fun push(repo: File): String {
        // TODO
        return "TODO"
    }

    fun push(): Map<String, String> {
        DialogGenerator.showConfirm("Push", "Push local commits to origin") {
            //TODO
            DialogGenerator.showConfirm("TODO", "No push performed!\nOperation not implemented") {}
        }
        return mapOf("Test" to "pushed it!")
    }
}
