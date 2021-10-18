package com.github.jensim.megamanipulator.onboarding

import com.github.jensim.megamanipulator.toolswindow.TabKey

enum class OnboardingId(
    val title: String? = null,
    val text: String,
    val autoMultiLineConvertion: Boolean = true,
    val tab: TabKey? = null,
    val next: OnboardingId? = null
) {

    FORK_LIST_AREA(
        text = "Here you are able to select only the fork repos that you want to affect.",
        next = null,
        tab = TabKey.tabTitleForks,
    ),
    FORK_DELETE_STALE_FORK_BUTTON(
        text = """
            Delete selected stale repos.
            LOOK CAREFULLY BEFORE YOU DO SOMETHING YOU'LL REGRET HERE.
        """.trimIndent(),
        next = FORK_LIST_AREA,
        tab = TabKey.tabTitleForks,
    ),
    FORK_LOAD_STALE_FORK_BUTTON(
        text = """
            Load fork repos that look stale.
            Have no active pull requests.
        """.trimIndent(),
        next = FORK_DELETE_STALE_FORK_BUTTON,
        tab = TabKey.tabTitleForks,
    ),
    FORK_TAB(
        title = "Forks",
        text = """
            Manage your private forks.
            That often become quite many when you are not granted rights
            to create branches directly on the origin repo.
        """.trimIndent(),
        next = FORK_LOAD_STALE_FORK_BUTTON,
        tab = TabKey.tabTitleForks,
    ),

    PR_ACTIONS_RESULT_AREA(
        text = """
            Pull request list and raw-format area.
            Select to limit the actions scope to these.
            Be ware that filtering does not implicitly deselect for you.
        """.trimIndent(), // TODO
        next = FORK_TAB,
        tab = TabKey.tabTitlePRsManage,
    ),
    PR_ACTIONS_BUTTON(
        text = """<html>
            <h3>Apply actions to selected Pull Requests</h3>
            <ul>
                <li>Decline PRs</li>
                <li>Reword PRs</li>
                <li>Add default reviewers</li>
                <li>Clone PRs</li>
                <li>Open in browser</li>
                <li>Add comment</li>
                <li>Mark Approved</li>
                <li>Mark Needs work</li>
                <li>Merge</li>
            </ul>
            </html>
        """.trimIndent(),
        autoMultiLineConvertion = false,
        next = PR_ACTIONS_RESULT_AREA,
        tab = TabKey.tabTitlePRsManage,
    ),
    PR_LIST_FILTER_FIELD(
        text = "Filter the fetched PRs",
        next = PR_ACTIONS_BUTTON,
        tab = TabKey.tabTitlePRsManage,
    ),
    PR_FETCH_ASSIGNEE_PR_BUTTON(
        text = "Fetch PRs you're assigned to",
        next = PR_LIST_FILTER_FIELD,
        tab = TabKey.tabTitlePRsManage,
    ),
    PR_FETCH_AUTHOR_PR_BUTTON(
        text = "Fetch PRs you've authored",
        next = PR_FETCH_ASSIGNEE_PR_BUTTON,
        tab = TabKey.tabTitlePRsManage,
    ),
    PR_CODE_HOST_SELECT(
        text = "Select which code host config to fetch PRs from",
        next = PR_FETCH_AUTHOR_PR_BUTTON,
        tab = TabKey.tabTitlePRsManage,
    ),
    PR_TAB(
        title = "Pull requests",
        text = """
            Manage your pull requests
            Assigned, or authored.
        """.trimIndent(),
        next = PR_CODE_HOST_SELECT,
        tab = TabKey.tabTitlePRsManage,
    ),

    CLONES_STEP_OUTPUT(
        text = "Outcome/output of selected step",
        next = PR_TAB,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_LIST_STEPS(
        text = "List of steps taken in last action for the selected repo",
        next = CLONES_STEP_OUTPUT,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_LIST_REPOS(
        text = "List of local clones",
        next = CLONES_LIST_STEPS,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_CLEAN_REPOS(
        text = """
            Delete local clones.
            If you have PRs, you are able to clone from PRs in the PR-tab
        """.trimIndent(),
        next = CLONES_LIST_REPOS,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_PR_BUTTON(
        text = "Draft new PRs for clones",
        next = CLONES_CLEAN_REPOS,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_PUSH_BUTTON(
        text = "Just push",
        next = CLONES_PR_BUTTON,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_COMMIT_PUSH_BUTTON(
        text = "Commit and push (optional)",
        next = CLONES_PUSH_BUTTON,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_LIST_BRANCH(
        text = """
            Refresh the list of local clones.
            List the git log, branches and remotes.
        """.trimIndent(),
        next = CLONES_COMMIT_PUSH_BUTTON,
        tab = TabKey.tabTitleClones,
    ),
    CLONES_TAB(
        title = "The Clones tab",
        autoMultiLineConvertion = false,
        text = """<html>
            Manage the clones you currently have
            <ul>
            <li>Switch branches</li>
            <li>Commit</li>
            <li>Push</li>
            <li>Create PRs</li>
            <li>Clean away clones</li>
            </ul>
            </html>
        """.trimIndent(),
        next = CLONES_LIST_BRANCH,
        tab = TabKey.tabTitleClones,
    ),

    APPLY_RESULT_AREA(
        text = """
            See how the scripted changes went.
            In case your script terminated in a non-zero status code,
            the repository line will be highlighted.
        """.trimIndent(),
        next = CLONES_TAB,
        tab = TabKey.tabTitleApply,
    ),
    APPLY_SCRIPT_OPEN_BUTTON(
        text = """
            Convenience button to find and open the script file,
            otherwise locates here:
            ./config/mega-manipulator.bash
        """.trimIndent(),
        next = APPLY_RESULT_AREA,
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
            Select your search host here if you have several.
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
        autoMultiLineConvertion = false,
        text = """
            <html>
            <a href="https://mega-manipulator.github.io/docs/intro"><h1>Mega Manipulator</h1></a>
            <p>
            This is a powerful tool.<br>
            And I take zero responsibility for what you do with it.<br>
            Be it by mistake or intentional.
            </p>
            <hr>
            <p>
            With that out of the way. ;-)<br>
            I hope you'll come to love mm and all work it removes from your "desk".
            </p>
            </html>
        """.trimIndent(),
        next = SETTINGS_TAB,
    ),

    MM_PROJECT_INSTRUCTION(
        title = "Thanks for downloading mega-manipulator!",
        autoMultiLineConvertion = false,
        text = """
            <html>
            <a href="https://mega-manipulator.github.io/docs/intro"><h1>Mega Manipulator</h1></a>
            To use the mega-manipulator plugin,<br> 
            you must create a intelliJ project of type<br> 
            "Mega Manipulator"
            </html>
        """.trimIndent()
    )
}
