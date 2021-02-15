package com.github.jensim.megamanipulatior.toolswindow

import com.intellij.ui.layout.panel
import com.jetbrains.rd.swing.textProperty
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JLabel
import javax.swing.JPasswordField

object SourcegraphSettingsPanel {

    private var sg_username: String = ""
    private const val width: Int = 30
    private val passwordField = JPasswordField("", width).apply {
        addActionListener { println("Password: ${textProperty()}") }
        addKeyListener(object : KeyListener {
            override fun keyTyped(e: KeyEvent) = println("Typed: Not yet implemented")
            override fun keyPressed(e: KeyEvent) = println("Pressed: Not yet implemented")
            override fun keyReleased(e: KeyEvent) = println("Released: Not yet implemented")
        })
        addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent) = println("Focus gained")
            override fun focusLost(e: FocusEvent) = println("Focus lost")
        })
    }
    private val loadingLabel = JLabel("Loading...").apply { isVisible = false }

    val content = panel {
        row {
            label("Sourcegraph base uri")
            textField({ "https://sourcegraph.example.com/" }, {}, width)
        }
        row {
            label("Sourcegraph username")
            textField({ sg_username }, { sg_username = it }, width)
        }
        row {
            label("Sourcegraph password")
            component(passwordField)
        }
        row {
            button("Test") {
                println("Im clicked!")
                loadingLabel.isVisible = true
            }
            component(loadingLabel)
        }
    }
}
