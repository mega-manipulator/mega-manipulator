package com.github.jensim.megamanipulator.settings.types

import com.github.ricky12awesome.jss.JsonSchema
import kotlinx.serialization.Serializable

@Serializable
enum class CloneType {

    @JsonSchema.Description(["If you have a passphrase in your ssh key the it must be added via ssh-agent and ssh-add prior to clone/fetch/push."])
    SSH,

    @JsonSchema.Description(["It's not recommended to use for your daily work, as the password/token will be stored in the git settings in plain text"])
    HTTPS,
}
