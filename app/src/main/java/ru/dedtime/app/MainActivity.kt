package ru.dedtime.app

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.dedtime.app.data.Profile
import ru.dedtime.app.data.UiSettings
import ru.dedtime.app.domain.GameState
import ru.dedtime.app.ui.AboutScreen
import ru.dedtime.app.ui.DIcon
import ru.dedtime.app.ui.DedtimeTheme
import ru.dedtime.app.ui.OnboardingScreen
import ru.dedtime.app.ui.RewardsScreen
import ru.dedtime.app.ui.SettingsScreen
import ru.dedtime.app.ui.ShareScreen
import ru.dedtime.app.ui.StatsScreen
import ru.dedtime.app.ui.T
import ru.dedtime.app.ui.TimerScreen
import ru.dedtime.app.ui.TodayScreen
import ru.dedtime.app.ui.dc

class MainVm(app: Application) : AndroidViewModel(app) {
    val repo = (app as DedtimeApp).repo
    val settingsStore = app.settings

    /** null — ещё грузится; Loaded(null) — профиля нет */
    data class Loaded(val profile: Profile?)

    val profile: StateFlow<Loaded?> = repo.profile.map { Loaded(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val settings: StateFlow<UiSettings?> = settingsStore.flow.map<UiSettings, UiSettings?> { it }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val state: StateFlow<GameState?> = repo.state.map<GameState, GameState?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val categories = repo.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val habits = repo.habits.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val logs = repo.logs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rewards = repo.rewards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val active = repo.activeSession.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Команды из интентов (виджеты, уведомление) */
    val commands = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    fun updateSettings(block: (UiSettings) -> UiSettings) = launch { settingsStore.update(block) }
}

class MainActivity : ComponentActivity() {
    private lateinit var vm: MainVm

    private val notifPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: MainVm = viewModel()
            vm = model
            LaunchedEffect(Unit) { handleIntent(intent) }
            val settings by model.settings.collectAsStateWithLifecycle()
            val loaded by model.profile.collectAsStateWithLifecycle()
            settings?.let { s ->
                DedtimeTheme(s) {
                    Box(Modifier.fillMaxSize().background(dc.bg)) {
                        val p = loaded
                        if (p != null) {
                            val profile = p.profile
                            if (profile == null) OnboardingScreen(model) else MainNav(model, profile)
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null || !::vm.isInitialized) return
        val cat = intent.getLongExtra(EXTRA_START_CATEGORY, -1L)
        if (cat > 0) {
            intent.removeExtra(EXTRA_START_CATEGORY)
            ensureNotificationPermission()
            vm.launch { vm.repo.startTimer(cat, "") }
            vm.commands.tryEmit("timer")
        }
        intent.getStringExtra(EXTRA_OPEN)?.let {
            intent.removeExtra(EXTRA_OPEN)
            vm.commands.tryEmit(it)
        }
    }

    companion object {
        const val EXTRA_START_CATEGORY = "start_category"
        const val EXTRA_OPEN = "open"
    }
}

private data class Tab(val route: String, val label: String, val icon: Int)

private val tabs = listOf(
    Tab("today", "Сегодня", R.drawable.ic_today),
    Tab("timer", "Таймер", R.drawable.ic_timer),
    Tab("stats", "Статистика", R.drawable.ic_stats),
    Tab("rewards", "Награды", R.drawable.ic_rewards),
)

@Composable
private fun MainNav(vm: MainVm, profile: Profile) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    LaunchedEffect(Unit) {
        vm.commands.collect { r -> if (tabs.any { it.route == r }) nav.goTab(r) }
    }
    Column(Modifier.fillMaxSize().background(dc.bg)) {
        Box(Modifier.weight(1f).statusBarsPadding()) {
            NavHost(nav, startDestination = "today") {
                composable("today") { TodayScreen(vm, profile, onSettings = { nav.navigate("settings") }, onTimer = { nav.goTab("timer") }) }
                composable("timer") { TimerScreen(vm) }
                composable("stats") { StatsScreen(vm, onShare = { nav.navigate("share/day") }) }
                composable("rewards") { RewardsScreen(vm, onShare = { nav.navigate("share/ach") }) }
                composable("settings") { SettingsScreen(vm, profile, onBack = { nav.popBackStack() }, onAbout = { nav.navigate("about") }) }
                composable("about") { AboutScreen(onBack = { nav.popBackStack() }) }
                composable("share/{mode}") { e ->
                    ShareScreen(vm, profile, e.arguments?.getString("mode") ?: "day", onBack = { nav.popBackStack() })
                }
            }
        }
        if (tabs.any { it.route == route }) {
            HorizontalDivider(color = dc.line)
            Row(
                Modifier.fillMaxWidth().background(dc.surface).navigationBarsPadding().height(68.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                tabs.forEach { t ->
                    val on = t.route == route
                    Column(
                        Modifier.weight(1f).height(68.dp).clickable(role = Role.Tab) { nav.goTab(t.route) }.padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        DIcon(t.icon, if (on) dc.ink else dc.muted, 24.dp, t.label)
                        Text(t.label, style = T.body(12.sp, if (on) FontWeight.ExtraBold else FontWeight.SemiBold, if (on) dc.ink else dc.muted))
                    }
                }
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo("today") { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
