package com.github.jensim.megamanipulator.project

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object CoroutinesHolder {

    val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
}
