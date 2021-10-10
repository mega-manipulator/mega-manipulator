package com.github.jensim.megamanipulator.onboarding

enum class OnboardingId(val title: String? = null, val text: String, val next: OnboardingId? = null) {
    CONFIG_FILE(
        title = "Config #️⃣",
        text = "This config is where you define your code and search hosts",
    ),
    SETTINGS_TAB_BUTTON_VALIDATE_TOKENS(
        text = """
            Validate saved tokens. 
            Click the tokens in the list under the button to set 
            or reset tokens in the secrets store
        """.trimIndent(),
        next = null,
    ),
    SETTINGS_TAB_BUTTON_RESET_ONBOARDING(
        text = """
            Reset the onboarding flow. 
            Allow previously seen popups to show again.
        """.trimIndent(),
        next = SETTINGS_TAB_BUTTON_VALIDATE_TOKENS,
    ),
    SETTINGS_TAB_BUTTON_DOCS(
        text = "Open browser and navigate to our online documentation",
        next = SETTINGS_TAB_BUTTON_RESET_ONBOARDING,
    ),
    SETTINGS_TAB_BUTTON_TOGGLE_CLONES(
        text = "Toggle project indexing for the $/clones folder",
        next = SETTINGS_TAB_BUTTON_DOCS,
    ),
    SETTINGS_TAB_BTN_VALIDATE_CONF(
        text = """
            Validate the Mega Manipulator configuration file 
            $/config/mega-manipulator.json. 
            That file is at the heart of how this plugin works.
            Containing your setup for connecting to your code hosts and search hosts.
        """.trimIndent(),
        next = SETTINGS_TAB_BUTTON_TOGGLE_CLONES,
    ),
    SETTINGS_TAB(
        title = "Settings",
        text = """
            Here you are able to validate the settings files,
            set and validate your login credentials to the code & search hosts
        """.trimIndent(),
        next = SETTINGS_TAB_BTN_VALIDATE_CONF,
    ),

    WELCOME(
        title = "Thanks for downloading mega-manipulator!",
        text = """
            This is a powerful tool.
            And I take zero responsibility for what you do with it.
            Be it by mistake or intentional.
            ----
            With that out of the way. ;-)
            I hope you'll come to love mm and all work it removes from your "desk".
        """.trimIndent(),
        next = SETTINGS_TAB,
    ),

    MM_PROJECT_INSTRUCTION(
        title = "Thanks for downloading mega-manipulator!",
        text = """To use the mega-manipulator plugin, you must create a intelliJ project of type "Mega Manipulator""""
    )
}
