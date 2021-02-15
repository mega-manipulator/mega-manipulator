package com.github.jensim.megamanipulatior.toolswindow

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.remoteServer.util.CloudConfigurationUtil.createCredentialAttributes
import com.intellij.ui.layout.panel
import com.jetbrains.rd.swing.textProperty
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JLabel
import javax.swing.JPasswordField

object SourcegraphSettingsPanel {

    private val attr: CredentialAttributes = createCredentialAttributes("sourcegrapgh-token", "token")!!
    private const val width: Int = 30
    private val tokenField = JPasswordField("", width).apply {
        addActionListener { println("Password: ${textProperty()}") }
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) = save()
            override fun keyPressed(e: KeyEvent) = Unit
            override fun keyReleased(e: KeyEvent) = Unit
        })
    }
    private val loadingLabel = JLabel("Loading...").apply { isVisible = false }

    val content = panel {
        row {
            label("Sourcegraph base uri")
            textField({ "https://sourcegraph.example.com/" }, {}, width)
        }
        row {
            label("Sourcegraph token")
            component(tokenField)
        }
        row {
            button("Test") {
                println("Im clicked!")
                loadingLabel.isVisible = true
            }
            component(loadingLabel)
        }
    }

    private fun save() {
        val credentials = Credentials("token", tokenField.password)
        PasswordSafe.instance[attr] = credentials
    }

    private fun load() {
        PasswordSafe.instance[attr]?.password?.let {
            tokenField.text = it.toString()
        }
    }

    init {
        load()
    }
}
