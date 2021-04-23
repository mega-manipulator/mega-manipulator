package com.github.jensim.megamanipulator.test

import com.github.jensim.megamanipulator.MyBundle
import com.github.jensim.megamanipulator.actions.NotificationsOperator
import com.github.jensim.megamanipulator.actions.ProcessOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyOperator
import com.github.jensim.megamanipulator.actions.apply.ApplyWindow
import com.github.jensim.megamanipulator.actions.forks.ForksWindow
import com.github.jensim.megamanipulator.actions.git.GitWindow
import com.github.jensim.megamanipulator.actions.git.clone.CloneOperator
import com.github.jensim.megamanipulator.actions.git.commit.CommitOperator
import com.github.jensim.megamanipulator.actions.localrepo.LocalRepoOperator
import com.github.jensim.megamanipulator.actions.search.SearchOperator
import com.github.jensim.megamanipulator.actions.search.SearchWindow
import com.github.jensim.megamanipulator.actions.search.sourcegraph.SourcegraphSearchClient
import com.github.jensim.megamanipulator.actions.vcs.PrRouter
import com.github.jensim.megamanipulator.actions.vcs.PullRequestActionsMenu
import com.github.jensim.megamanipulator.actions.vcs.PullRequestWindow
import com.github.jensim.megamanipulator.actions.vcs.bitbucketserver.BitbucketServerClient
import com.github.jensim.megamanipulator.actions.vcs.githubcom.GithubComClient
import com.github.jensim.megamanipulator.files.FilesOperator
import com.github.jensim.megamanipulator.settings.MegaManipulatorSettings
import com.github.jensim.megamanipulator.settings.PasswordsOperator
import com.github.jensim.megamanipulator.settings.ProjectOperator
import com.github.jensim.megamanipulator.settings.SerializationHolder
import com.github.jensim.megamanipulator.settings.SettingsFileOperator
import com.github.jensim.megamanipulator.settings.SettingsWindow
import com.github.jensim.megamanipulator.toolswindow.MyToolWindowFactory
import com.github.jensim.megamanipulator.ui.DialogGenerator
import com.github.jensim.megamanipulator.ui.UiProtector
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.nio.file.Path
import java.util.UUID
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory

@ExperimentalPathApi
class TestWiring(
    project: Project? = null,
    projectOperator: ProjectOperator? = null,
    processOperator: ProcessOperator? = null,
    localRepoOperator: LocalRepoOperator? = null,
    settings: MegaManipulatorSettings? = null,
    settingsFileOperator: SettingsFileOperator? = null,
    myToolWindowFactory: MyToolWindowFactory? = null,
    filesOperator: FilesOperator? = null,
    tabSettings: SettingsWindow? = null,
    tabSearch: SearchWindow? = null,
    tabApply: ApplyWindow? = null,
    tabClones: GitWindow? = null,
    tabPRsManage: PullRequestWindow? = null,
    tabForks: ForksWindow? = null,
    notificationsOperator: NotificationsOperator? = null,
    passwordsOperator: PasswordsOperator? = null,
    searchOperator: SearchOperator? = null,
    cloneOperator: CloneOperator? = null,
    uiProtector: UiProtector? = null,
    prRouter: PrRouter? = null,
    serializationHolder: SerializationHolder? = null,
    applyOperator: ApplyOperator? = null,
    commitOperator: CommitOperator? = null,
    dialogGenerator: DialogGenerator? = null,
    sourcegraphSearchClient: SourcegraphSearchClient? = null,
    bitbucketServerClient: BitbucketServerClient? = null,
    githubComClient: GithubComClient? = null,
    pullRequestActionsMenu: PullRequestActionsMenu? = null,
    myBundle:MyBundle? = null,
) {

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())
    val project: Project by lazy {
        project ?: mock {
            on { basePath } doReturn tempDir.absolutePath
        }
    }
    val projectOperator: ProjectOperator by lazy {
        val contentFactory:ContentFactory = mock<ContentFactory>().apply {
            whenever(this.createContent(any(), anyString(), anyBoolean())).thenReturn(mock())
        }
        projectOperator ?: mock<ProjectOperator>().apply {
            whenever(this.project).thenReturn(project)
            whenever(this.contentFactory).thenReturn(contentFactory)
        }
    }
    val processOperator: ProcessOperator by lazy {
        processOperator ?: ProcessOperator(
            projectOperator = this.projectOperator
        )
    }
    val localRepoOperator: LocalRepoOperator by lazy {
        localRepoOperator ?: LocalRepoOperator(
            projectOperator = this.projectOperator,
            processOperator = this.processOperator
        )
    }
    val passwordsOperator: PasswordsOperator by lazy {
        passwordsOperator ?: TestPasswordOperator(emptyMap())
    }
    val settingsFileOperator: SettingsFileOperator by lazy {
        settingsFileOperator ?: mock {
            on { readSettings() } doReturn settings
        }
    }
    val notificationsOperator: NotificationsOperator by lazy {
        notificationsOperator ?: mock()
    }
    val sourcegraphSearchClient: SourcegraphSearchClient by lazy {
        sourcegraphSearchClient ?: mock()
    }
    val searchOperator: SearchOperator by lazy {
        searchOperator ?: SearchOperator(
            settingsFileOperator = this.settingsFileOperator,
            sourcegraphSearchClient = this.sourcegraphSearchClient,
        )
    }
    val cloneOperator: CloneOperator by lazy {
        cloneOperator ?: CloneOperator(
            filesOperator = this.filesOperator,
            projectOperator = this.projectOperator,
            prRouter = this.prRouter,
            localRepoOperator = this.localRepoOperator,
            processOperator = this.processOperator,
            notificationsOperator = this.notificationsOperator,
            uiProtector = this.uiProtector,
        )
    }
    val bitbucketServerClient: BitbucketServerClient by lazy {
        bitbucketServerClient ?: mock()
    }
    val githubComClient: GithubComClient by lazy {
        githubComClient ?: mock()
    }
    val uiProtector: UiProtector by lazy {
        uiProtector ?: UiProtector(
            projectOperator = this.projectOperator,
            notificationsOperator = this.notificationsOperator,
        )
    }
    val prRouter: PrRouter by lazy {
        prRouter ?: PrRouter(
            settingsFileOperator = this.settingsFileOperator,
            bitbucketServerClient = this.bitbucketServerClient,
            githubComClient = this.githubComClient,
            notificationsOperator = this.notificationsOperator
        )
    }
    val serializationHolder: SerializationHolder by lazy {
        serializationHolder ?: SerializationHolder()
    }
    val applyOperator: ApplyOperator by lazy {
        applyOperator ?: ApplyOperator(
            settingsFileOperator = this.settingsFileOperator,
            filesOperator = this.filesOperator,
            processOperator = this.processOperator,
            localRepoOperator = this.localRepoOperator,
            uiProtector = this.uiProtector,
        )
    }
    val commitOperator: CommitOperator by lazy {
        commitOperator ?: CommitOperator(
            dialogGenerator = this.dialogGenerator,
            settingsFileOperator = this.settingsFileOperator,
            localRepoOperator = this.localRepoOperator,
            processOperator = this.processOperator,
            prRouter = this.prRouter,
            uiProtector = this.uiProtector,
        )
    }
    val dialogGenerator: DialogGenerator by lazy {
        dialogGenerator ?: DialogGenerator()
    }
    val filesOperator: FilesOperator by lazy {
        filesOperator ?: FilesOperator(
            notificationsOperator = this.notificationsOperator,
            projectOperator = this.projectOperator,
        )
    }
    val tabSettings: SettingsWindow by lazy {
        tabSettings ?: SettingsWindow(
            passwordsOperator = this.passwordsOperator,
            projectOperator = this.projectOperator,
            filesOperator = this.filesOperator,
            settingsFileOperator = this.settingsFileOperator,
        )
    }
    val tabSearch: SearchWindow by lazy {
        tabSearch ?: SearchWindow(
            searchOperator = this.searchOperator,
            settingsFileOperator = this.settingsFileOperator,
            cloneOperator = this.cloneOperator,
            uiProtector = this.uiProtector,
        )
    }
    val tabApply: ApplyWindow by lazy {
        tabApply ?: ApplyWindow(
            applyOperator = this.applyOperator,
            projectOperator = this.projectOperator,
        )
    }
    val tabClones: GitWindow by lazy {
        tabClones ?: GitWindow(
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
    val pullRequestActionsMenu: PullRequestActionsMenu by lazy {
        pullRequestActionsMenu ?: PullRequestActionsMenu(
            prRouter = this.prRouter,
            notificationsOperator = this.notificationsOperator,
            dialogGenerator = this.dialogGenerator,
            cloneOperator = this.cloneOperator,
            uiProtector = this.uiProtector,
        )
    }
    val tabPRsManage: PullRequestWindow by lazy {
        tabPRsManage ?: PullRequestWindow(
            prRouter = this.prRouter,
            serializationHolder = this.serializationHolder,
            uiProtector = this.uiProtector,
            pullRequestActionsMenu = this.pullRequestActionsMenu,
        )
    }
    val tabForks: ForksWindow by lazy {
        tabForks ?: ForksWindow(
            prRouter = this.prRouter,
            notificationsOperator = this.notificationsOperator,
            uiProtector = this.uiProtector,
        )
    }
    val myBundle:MyBundle by lazy {
        myBundle ?: mock { on { message(anyString(), anyVararg()) } doReturn UUID.randomUUID().toString() }
    }
    val myToolWindowFactory: MyToolWindowFactory by lazy {
        myToolWindowFactory ?: MyToolWindowFactory(
            filesOperator = this.filesOperator,
            projectOperator = this.projectOperator,
            tabSettings = this.tabSettings,
            tabSearch = this.tabSearch,
            tabApply = this.tabApply,
            tabClones = this.tabClones,
            tabPRsManage = this.tabPRsManage,
            tabForks = this.tabForks,
            myBundle = this.myBundle,
        )
    }

    internal class TestWiringTest {

        @Test
        internal fun `everything lazy`() {
            val testWiring = TestWiring(filesOperator = mock())
            val target = testWiring.myToolWindowFactory
            val toolWindow = mock<ToolWindow> {
                on { contentManager } doReturn mock()
            }

            target.createToolWindowContent(testWiring.project, toolWindow)
        }
    }
}
