package com.github.jensim.megamanipulator.settings.types

import java.util.Base64

private val base64encoder = Base64.getEncoder()

fun encodeToBase64String(value: String) = base64encoder.encodeToString(value.toByteArray())
