package com.github.jensim.megamanipulator.onboarding

import com.github.jensim.megamanipulator.toolswindow.TabKey

enum class OnboardingId(
    val title: String? = null,
    val text: String,
    val tab: TabKey? = null,
    val next: OnboardingId? = null
) {

    APPLY_SCRIPT_OPEN_BUTTON(
        text = """
            Convenience button to find and open the script file,
            otherwise locates here:
            ./config/mega-manipulator.bash
            """.trimIndent(),
        tab = TabKey.tabTitleApply,
    ),
    APPLY_BUTTON(
        text = """
            Apply scripted changes defined in the
            ./config/mega-manipulator.bash file
            """.trimIndent(),
        next = APPLY_SCRIPT_OPEN_BUTTON,
        tab = TabKey.tabTitleApply,
    ),
    APPLY_TAB(
        title = "Apply scripted changes",
        text = "Apply scripted changes to all the currently cloned repos",
        next = APPLY_BUTTON,
        tab = TabKey.tabTitleApply,
    ),
    CONFIG_FILE(
        title = "Config #️⃣",
        text = "This config is where you define your code and search hosts",
    ),
    SEARCH_CLONE_BUTTON(
        text = "Clone <b><u>selected</u></b> repos",
        tab = TabKey.tabTitleSearch,
        next = APPLY_TAB,
    ),
    SEARCH_BUTTON(
        text = "Execute your search here",
        tab = TabKey.tabTitleSearch,
        next = SEARCH_CLONE_BUTTON,
    ),
    SEARCH_INPUT(
        text = "Search string, same format (copy-pasteable) as your search host.",
        tab = TabKey.tabTitleSearch,
        next = SEARCH_BUTTON,
    ),
    SEARCH_HOST_SELECT(
        text = """
            Select your code host here if you have several.
            Reorder the hosts in the config file for them to appear in that order here.
            Top one is preselected.
            """.trimIndent(),
        tab = TabKey.tabTitleSearch,
        next = SEARCH_INPUT,
    ),
    SEARCH_DOC_BUTTON(
        text = "Direct link to mm-docs on the selected search host",
        tab = TabKey.tabTitleSearch,
        next = SEARCH_HOST_SELECT,
    ),
    SEARCH_TAB(
        title = "The Search tab",
        text = """
            This is where each search and replace journey begins.
            Different search hosts behave a bit differently,
            so be aware of that.
            """.trimIndent(),
        tab = TabKey.tabTitleSearch,
        next = SEARCH_DOC_BUTTON,
    ),
    SETTINGS_TAB_BUTTON_VALIDATE_TOKENS(
        text = """
        Validate saved tokens. 
        Click the tokens in the list under the button to set 
        or reset tokens in the secrets store
        """.trimIndent(),
        next = SEARCH_TAB,
        tab = TabKey.tabTitleSettings,
    ),
    SETTINGS_TAB_BUTTON_RESET_ONBOARDING(
        text = """
            Reset the onboarding flow. 
            Allow previously seen popups to show again.
        """.trimIndent(),
        next = SETTINGS_TAB_BUTTON_VALIDATE_TOKENS,
        tab = TabKey.tabTitleSettings,
    ),
    SETTINGS_TAB_BUTTON_DOCS(
        text = "Open browser and navigate to our online documentation",
        next = SETTINGS_TAB_BUTTON_RESET_ONBOARDING,
        tab = TabKey.tabTitleSettings,
    ),
    SETTINGS_TAB_BUTTON_TOGGLE_CLONES(
        text = "Toggle project indexing for the $/clones folder",
        next = SETTINGS_TAB_BUTTON_DOCS,
        tab = TabKey.tabTitleSettings,
    ),
    SETTINGS_TAB_BTN_VALIDATE_CONF(
        text = """
            Validate the Mega Manipulator configuration file 
            $/config/mega-manipulator.json. 
            That file is at the heart of how this plugin works.
            Containing your setup for connecting to your code hosts and search hosts.
        """.trimIndent(),
        next = SETTINGS_TAB_BUTTON_TOGGLE_CLONES,
        tab = TabKey.tabTitleSettings,
    ),
    SETTINGS_TAB(
        title = "Settings",
        text = """
            Here you are able to validate the settings files,
            set and validate your login credentials to the code & search hosts
        """.trimIndent(),
        next = SETTINGS_TAB_BTN_VALIDATE_CONF,
        tab = TabKey.tabTitleSettings,
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
        text = """
            To use the mega-manipulator plugin, 
            you must create a intelliJ project of type 
            "Mega Manipulator"
            """.trimIndent()
    )
}
