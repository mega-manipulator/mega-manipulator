package com.github.jensim.megamanipulatior.ui

import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asCoroutineDispatcher

object SingleThreadContext : CoroutineContext by Executors.newSingleThreadExecutor().asCoroutineDispatcher()
