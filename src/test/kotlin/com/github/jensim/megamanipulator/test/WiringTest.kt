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
import com.github.jensim.megamanipulator.http.HttpClientProvider
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
class WiringTest {

    private val tempDirPath: Path = createTempDirectory(prefix = null, attributes = emptyArray())
    private val tempDir: File = File(tempDirPath.toUri())
    private val mockProject: Project = mock {
        on { basePath } doReturn tempDir.absolutePath
    }
    private val projectOperator: ProjectOperator
        get() {
            val contentFactory: ContentFactory = mock<ContentFactory>().apply {
                whenever(this.createContent(any(), anyString(), anyBoolean())).thenReturn(mock())
            }
            return mock<ProjectOperator>().apply {
                whenever(this.project).thenReturn(mockProject)
                whenever(this.contentFactory).thenReturn(contentFactory)
            }
        }
    private val processOperator: ProcessOperator = ProcessOperator(
        projectOperator = this.projectOperator
    )
    private val localRepoOperator: LocalRepoOperator = LocalRepoOperator(
        projectOperator = this.projectOperator,
        processOperator = this.processOperator
    )
    private val passwordsOperator: PasswordsOperator = TestPasswordOperator(emptyMap())
    private val settingsFileOperator: SettingsFileOperator = mock()
    private val notificationsOperator: NotificationsOperator = mock()
    private val sourcegraphSearchClient: SourcegraphSearchClient = mock()
    private val searchOperator: SearchOperator = SearchOperator(
        settingsFileOperator = this.settingsFileOperator,
        sourcegraphSearchClient = this.sourcegraphSearchClient,
    )
    private val filesOperator: FilesOperator = mock()
    private val httpClientProvider = HttpClientProvider(
        settingsFileOperator = this.settingsFileOperator,
        passwordsOperator = this.passwordsOperator,
        notificationsOperator = this.notificationsOperator,
    )
    private val serializationHolder: SerializationHolder = SerializationHolder()
    private val json = this.serializationHolder.readableJson
    private val bitbucketServerClient = BitbucketServerClient(
        httpClientProvider = this.httpClientProvider,
        localRepoOperator = this.localRepoOperator,
        notificationsOperator = this.notificationsOperator,
        json = this.json,
    )
    private val githubComClient = GithubComClient(
        httpClientProvider = this.httpClientProvider,
        localRepoOperator = this.localRepoOperator,
        json = this.json,
    )
    private val prRouter: PrRouter = PrRouter(
        settingsFileOperator = this.settingsFileOperator,
        bitbucketServerClient = this.bitbucketServerClient,
        githubComClient = this.githubComClient,
        notificationsOperator = this.notificationsOperator
    )
    private val uiProtector: UiProtector = UiProtector(
        projectOperator = this.projectOperator,
        notificationsOperator = this.notificationsOperator,
    )
    private val cloneOperator: CloneOperator = CloneOperator(
        filesOperator = this.filesOperator,
        projectOperator = this.projectOperator,
        prRouter = this.prRouter,
        localRepoOperator = this.localRepoOperator,
        processOperator = this.processOperator,
        notificationsOperator = this.notificationsOperator,
        uiProtector = this.uiProtector,
    )
    private val applyOperator: ApplyOperator = ApplyOperator(
        settingsFileOperator = this.settingsFileOperator,
        filesOperator = this.filesOperator,
        processOperator = this.processOperator,
        localRepoOperator = this.localRepoOperator,
        uiProtector = this.uiProtector,
    )
    private val dialogGenerator: DialogGenerator = DialogGenerator()
    private val commitOperator: CommitOperator = CommitOperator(
        dialogGenerator = this.dialogGenerator,
        settingsFileOperator = this.settingsFileOperator,
        localRepoOperator = this.localRepoOperator,
        processOperator = this.processOperator,
        prRouter = this.prRouter,
        uiProtector = this.uiProtector,
    )
    private val tabSettings: SettingsWindow = SettingsWindow(
        passwordsOperator = this.passwordsOperator,
        projectOperator = this.projectOperator,
        filesOperator = this.filesOperator,
        settingsFileOperator = this.settingsFileOperator,
    )
    private val tabSearch: SearchWindow = SearchWindow(
        searchOperator = this.searchOperator,
        settingsFileOperator = this.settingsFileOperator,
        cloneOperator = this.cloneOperator,
        uiProtector = this.uiProtector,
    )
    private val tabApply: ApplyWindow = ApplyWindow(
        applyOperator = this.applyOperator,
        projectOperator = this.projectOperator,
    )
    private val tabClones: GitWindow = GitWindow(
        localRepoOperator = this.localRepoOperator,
        processOperator = this.processOperator,
        commitOperator = this.commitOperator,
        dialogGenerator = this.dialogGenerator,
        filesOperator = this.filesOperator,
        projectOperator = this.projectOperator,
        prRouter = this.prRouter,
        uiProtector = this.uiProtector,
    )
    private val pullRequestActionsMenu: PullRequestActionsMenu = PullRequestActionsMenu(
        prRouter = this.prRouter,
        notificationsOperator = this.notificationsOperator,
        dialogGenerator = this.dialogGenerator,
        cloneOperator = this.cloneOperator,
        uiProtector = this.uiProtector,
    )
    private val tabPRsManage: PullRequestWindow = PullRequestWindow(
        prRouter = this.prRouter,
        serializationHolder = this.serializationHolder,
        uiProtector = this.uiProtector,
        pullRequestActionsMenu = this.pullRequestActionsMenu,
    )
    private val tabForks: ForksWindow = ForksWindow(
        prRouter = this.prRouter,
        notificationsOperator = this.notificationsOperator,
        uiProtector = this.uiProtector,
    )
    private val myBundle: MyBundle = mock { on { message(anyString(), anyVararg()) } doReturn UUID.randomUUID().toString() }
    private val myToolWindowFactory: MyToolWindowFactory = MyToolWindowFactory(
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

    @Test
    internal fun `everything lazy`() {
        val toolWindow = mock<ToolWindow> {
            on { contentManager } doReturn mock()
        }

        myToolWindowFactory.createToolWindowContent(mockProject, toolWindow)
    }
}
