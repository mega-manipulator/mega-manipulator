package com.github.jensim.megamanipulator

import com.expediagroup.graphql.client.serialization.GraphQLClientKotlinxSerializer
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyWindow
import com.github.jensim.megamanipulator.actions.forks.ForksWindow
import com.github.jensim.megamanipulator.actions.git.GitUrlHelper
import com.github.jensim.megamanipulator.actions.git.GitWindow
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.search.SearchWindow
import com.github.jensim.megamanipulator.actions.search.hound.HoundClient
import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchClient
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestActionsMenu
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWindow
import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClient
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComClient
import com.github.jensim.megamanipulator.actions.vcs.gitlab.GitLabClient
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.http.HttpClientProvider
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.SettingsWindow
import com.github.jensim.megamanipulator.settings.passwords.IntelliJPasswordsOperator
import com.github.jensim.megamanipulator.settings.passwords.PasswordsOperator
import com.github.jensim.megamanipulator.settings.passwords.ProjectOperator
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.github.jensim.megamanipulator.ui.UiProtectorImpl
import com.intellij.notification.NotificationGroupManager
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json

data class ApplicationWiring(
    val projectOperator: ProjectOperator,
    private val filesOperatorOverride: FilesOperator? = null,
    private val tabSettingsOverride: SettingsWindow? = null,
    private val tabSearchOverride: SearchWindow? = null,
    private val tabApplyOverride: ApplyWindow? = null,
    private val tabClonesOverride: GitWindow? = null,
    private val tabPRsManageOverride: PullRequestWindow? = null,
    private val tabForksOverride: ForksWindow? = null,
    private val myBundleOverride: MyBundle? = null,
    private val notificationGroupManagerOverride: NotificationGroupManager? = null,
    private val notificationsOperatorOverride: NotificationsOperator? = null,

    private val passwordsOperatorOverride: PasswordsOperator? = null,
    private val settingsFileOperatorOverride: SettingsFileOperator? = null,
    private val searchOperatorOverride: SearchOperator? = null,
    private val cloneOperatorOverride: CloneOperator? = null,
    private val uiProtectorOverride: UiProtector? = null,
    private val applyOperatorOverride: ApplyOperator? = null,
    private val localRepoOperatorOverride: LocalRepoOperator? = null,
    private val processOperatorOverride: ProcessOperator? = null,
    private val commitOperatorOverride: CommitOperator? = null,
    private val dialogGeneratorOverride: DialogGenerator? = null,
    private val prRouterOverride: PrRouter? = null,
    private val serializationHolderOverride: SerializationHolder? = null,
    private val pullRequestActionsMenuOverride: PullRequestActionsMenu? = null,
    private val sourcegraphSearchClientOverride: SourcegraphSearchClient? = null,
    private val graphQLClientKotlinxSerializerOverride: GraphQLClientKotlinxSerializer? = null,
    private val houndClientOverride: HoundClient? = null,
    private val bitbucketServerClientOverride: BitbucketServerClient? = null,
    private val githubComClientOverride: GithubComClient? = null,
    private val gitLabClientOverride: GitLabClient? = null,
    private val httpClientProviderOverride: HttpClientProvider? = null,
    private val jsonOverride: Json? = null,
) {

    constructor(project: Project) : this(projectOperator = ProjectOperator(project))

    val serializationHolder: SerializationHolder by lazy {
        serializationHolderOverride ?: SerializationHolder()
    }
    val pullRequestActionsMenu: PullRequestActionsMenu by lazy {
        pullRequestActionsMenuOverride ?: PullRequestActionsMenu(
            prRouter = this.prRouter,
            notificationsOperator = this.notificationsOperator,
            dialogGenerator = this.dialogGenerator,
            cloneOperator = this.cloneOperator,
            uiProtector = this.uiProtector,
        )
    }
    val passwordsOperator: PasswordsOperator by lazy {
        passwordsOperatorOverride ?: IntelliJPasswordsOperator(
            notificationsOperator = this.notificationsOperator,
            serializationHolder = this.serializationHolder,
        )
    }
    val settingsFileOperator: SettingsFileOperator by lazy {
        settingsFileOperatorOverride ?: SettingsFileOperator(
            projectOperator = this.projectOperator,
            notificationsOperator = this.notificationsOperator,
        )
    }
    val httpClientProvider: HttpClientProvider by lazy {
        httpClientProviderOverride ?: HttpClientProvider(
            settingsFileOperator = this.settingsFileOperator,
            passwordsOperator = this.passwordsOperator,
            notificationsOperator = this.notificationsOperator,
        )
    }
    val json: Json by lazy {
        jsonOverride ?: serializationHolder.readableJson
    }
    val graphQLClientKotlinxSerializer: GraphQLClientKotlinxSerializer by lazy {
        graphQLClientKotlinxSerializerOverride ?: GraphQLClientKotlinxSerializer()
    }
    val sourcegraphSearchClient: SourcegraphSearchClient by lazy {
        sourcegraphSearchClientOverride ?: SourcegraphSearchClient(
            httpClientProvider = this.httpClientProvider,
            notificationsOperator = this.notificationsOperator,
            graphQLClientKotlinxSerializer = this.graphQLClientKotlinxSerializer,
        )
    }
    val houndClient: HoundClient by lazy {
        houndClientOverride ?: HoundClient(
            httpClientProvider = this.httpClientProvider,
            json = this.json,
        )
    }
    val bitbucketServerClient: BitbucketServerClient by lazy {
        bitbucketServerClientOverride ?: BitbucketServerClient(
            httpClientProvider = this.httpClientProvider,
            localRepoOperator = this.localRepoOperator,
            notificationsOperator = this.notificationsOperator,
            json = this.json,
        )
    }
    val githubComClient: GithubComClient by lazy {
        githubComClientOverride ?: GithubComClient(
            httpClientProvider = this.httpClientProvider,
            localRepoOperator = this.localRepoOperator,
            json = this.json,
        )
    }
    val gitLabClient: GitLabClient by lazy {
        gitLabClientOverride ?: GitLabClient(
            httpClientProvider = this.httpClientProvider,
            json = this.json,
            graphQLClientKotlinxSerializer = this.graphQLClientKotlinxSerializer,
            localRepoOperator = this.localRepoOperator,
        )
    }
    val searchOperator: SearchOperator by lazy {
        searchOperatorOverride ?: SearchOperator(
            settingsFileOperator = this.settingsFileOperator,
            sourcegraphSearchClient = this.sourcegraphSearchClient,
            houndClient = this.houndClient,
        )
    }
    val gitUrlHelper:GitUrlHelper by lazy {
        GitUrlHelper(
            passwordsOperator = this.passwordsOperator,
        )
    }
    val cloneOperator: CloneOperator by lazy {
        cloneOperatorOverride ?: CloneOperator(
            filesOperator = this.filesOperator,
            projectOperator = this.projectOperator,
            prRouter = this.prRouter,
            localRepoOperator = this.localRepoOperator,
            processOperator = this.processOperator,
            notificationsOperator = this.notificationsOperator,
            uiProtector = this.uiProtector,
            settingsFileOperator = this.settingsFileOperator,
            gitUrlHelper = this.gitUrlHelper,
        )
    }
    val uiProtector: UiProtector by lazy {
        uiProtectorOverride ?: UiProtectorImpl(
            projectOperator = this.projectOperator,
            notificationsOperator = this.notificationsOperator,
        )
    }
    val applyOperator: ApplyOperator by lazy {
        applyOperatorOverride ?: ApplyOperator(
            settingsFileOperator = this.settingsFileOperator,
            filesOperator = this.filesOperator,
            processOperator = this.processOperator,
            localRepoOperator = this.localRepoOperator,
            uiProtector = this.uiProtector,
        )
    }
    val localRepoOperator: LocalRepoOperator by lazy {
        localRepoOperatorOverride ?: LocalRepoOperator(
            projectOperator = this.projectOperator,
            processOperator = this.processOperator,
            uiProtector = this.uiProtector,
        )
    }
    val processOperator: ProcessOperator by lazy {
        processOperatorOverride ?: ProcessOperator(
            projectOperator = this.projectOperator,
        )
    }
    val commitOperator: CommitOperator by lazy {
        commitOperatorOverride ?: CommitOperator(
            dialogGenerator = this.dialogGenerator,
            settingsFileOperator = this.settingsFileOperator,
            localRepoOperator = this.localRepoOperator,
            processOperator = this.processOperator,
            prRouter = this.prRouter,
            uiProtector = this.uiProtector,
            gitUrlHelper = this.gitUrlHelper,
        )
    }
    val dialogGenerator: DialogGenerator by lazy {
        dialogGeneratorOverride ?: DialogGenerator()
    }
    val prRouter: PrRouter by lazy {
        prRouterOverride ?: PrRouter(
            settingsFileOperator = this.settingsFileOperator,
            bitbucketServerClient = this.bitbucketServerClient,
            githubComClient = this.githubComClient,
            gitLabClient = this.gitLabClient,
            notificationsOperator = this.notificationsOperator,
        )
    }
    val notificationGroupManager: NotificationGroupManager by lazy {
        notificationGroupManagerOverride ?: NotificationGroupManager.getInstance()
    }
    val notificationsOperator: NotificationsOperator by lazy {
        notificationsOperatorOverride ?: NotificationsOperator(projectOperator, notificationGroupManager.getNotificationGroup("Mega Manipulator"))
    }
    val filesOperator: FilesOperator by lazy {
        filesOperatorOverride ?: FilesOperator(
            notificationsOperator = this.notificationsOperator,
            projectOperator = this.projectOperator,
        )
    }
    val tabSettings: SettingsWindow by lazy {
        tabSettingsOverride ?: SettingsWindow(
            passwordsOperator = this.passwordsOperator,
            projectOperator = this.projectOperator,
            filesOperator = this.filesOperator,
            settingsFileOperator = this.settingsFileOperator,
            uiProtector = this.uiProtector,
            prRouter = this.prRouter,
            searchOperator = this.searchOperator,
        )
    }
    val tabSearch: SearchWindow by lazy {
        tabSearchOverride ?: SearchWindow(
            searchOperator = this.searchOperator,
            settingsFileOperator = this.settingsFileOperator,
            cloneOperator = this.cloneOperator,
            uiProtector = this.uiProtector,
        )
    }
    val tabApply: ApplyWindow by lazy {
        tabApplyOverride ?: ApplyWindow(
            applyOperator = this.applyOperator,
            projectOperator = this.projectOperator,
        )
    }
    val tabClones: GitWindow by lazy {
        tabClonesOverride ?: GitWindow(
            localRepoOperator = this.localRepoOperator,
            processOperator = this.processOperator,
            commitOperator = this.commitOperator,
            dialogGenerator = this.dialogGenerator,
            filesOperator = this.filesOperator,
            projectOperator = this.projectOperator,
            prRouter = this.prRouter,
            uiProtector = this.uiProtector,
        )
    }
    val tabPRsManage: PullRequestWindow by lazy {
        tabPRsManageOverride ?: PullRequestWindow(
            prRouter = this.prRouter,
            serializationHolder = this.serializationHolder,
            uiProtector = this.uiProtector,
            pullRequestActionsMenu = this.pullRequestActionsMenu,
            settingsFileOperator = this.settingsFileOperator,
        )
    }
    val tabForks: ForksWindow by lazy {
        tabForksOverride ?: ForksWindow(
            prRouter = this.prRouter,
            notificationsOperator = this.notificationsOperator,
            uiProtector = this.uiProtector,
            settingsFileOperator = this.settingsFileOperator,
        )
    }
    val myBundle: MyBundle by lazy { myBundleOverride ?: MyBundle() }
}
