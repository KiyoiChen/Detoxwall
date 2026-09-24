package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import com.example.ui.components.ClerkAuthDialog
import com.example.ui.components.ChangePinDialog
import com.example.ui.components.PinSetupDialog
import com.example.ui.components.RemovePinDialog
import com.example.ui.theme.LimitRose
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DetoxWallLogo
import com.example.ui.screens.ChildModeScreen
import com.example.ui.screens.LimitsScreen
import com.example.ui.screens.SchedulesScreen
import com.example.ui.screens.UsageScreen
import com.example.ui.theme.DetoxMint
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBg
import com.example.ui.viewmodel.DetoxViewModel
import com.example.util.SecurityManager

class MainActivity : ComponentActivity() {

    private val viewModel: DetoxViewModel by viewModels()

    // Re-check permissions every time the user returns from another screen (e.g., system settings)
    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // ── Theme reactive to user preference ──────────────────────────────
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                SecurityManager.THEME_LIGHT -> false
                SecurityManager.THEME_DARK -> true
                else -> systemDark // THEME_SYSTEM
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                val isChildModeActive by viewModel.isChildModeActive.collectAsStateWithLifecycle()
                val hasParentPin by viewModel.hasParentPin.collectAsStateWithLifecycle()
                val isClerkSignedIn by viewModel.isClerkSignedIn.collectAsStateWithLifecycle()
                val clerkUser by viewModel.clerkUser.collectAsStateWithLifecycle()
                val clerkPublishableKey by viewModel.clerkPublishableKey.collectAsStateWithLifecycle()

                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var showInfoSheet by remember { mutableStateOf(false) }
                var showClerkAuthDialog by remember { mutableStateOf(false) }
                var showMenuPinSetupDialog by remember { mutableStateOf(false) }
                var showMenuChangePinDialog by remember { mutableStateOf(false) }
                var showMenuRemovePinDialog by remember { mutableStateOf(false) }
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val navItems = listOf(
                    Triple("Home", Icons.Default.Home, "nav_tab_home"),
                    Triple("App Limits", Icons.Default.HourglassEmpty, "nav_tab_limits"),
                    Triple("Usage", Icons.Default.BarChart, "nav_tab_usage"),
                    Triple("Schedules", Icons.Default.Schedule, "nav_tab_schedules")
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ObsidianBg,
                    topBar = {
                        TopAppBar(
                            title = {
                                if (selectedTabIndex == 2) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { selectedTabIndex = 0 },
                                            modifier = Modifier.size(36.dp).testTag("back_to_home_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "Back to Home",
                                                tint = Color.White
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Usage Insights",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        DetoxWallLogo(size = 32.dp, primaryColor = DetoxMint)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "DetoxWall",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            fontSize = 20.sp
                                        )
                                    }
                                }
                            },
                            actions = {
                                if (selectedTabIndex != 2) {
                                    // Clerk Login / User Profile Pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isClerkSignedIn) Color(0xFF6C47FF).copy(alpha = 0.2f) else Color(0xFF1E293B))
                                            .border(1.dp, if (isClerkSignedIn) Color(0xFF6C47FF) else Color(0xFF334155), RoundedCornerShape(16.dp))
                                            .clickable { showClerkAuthDialog = true }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                            .testTag("topbar_clerk_btn"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isClerkSignedIn) Color(0xFF6C47FF) else Color(0xFF475569)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("C", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isClerkSignedIn) (clerkUser?.firstName ?: "Account") else "Login",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isClerkSignedIn) Color(0xFFA78BFA) else Color(0xFF94A3B8)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFF0B2527))
                                            .border(1.dp, Color(0xFF0E4447), RoundedCornerShape(16.dp))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = if (isChildModeActive) "Child Mode" else "Adult Mode",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = DetoxMint
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { showInfoSheet = true },
                                        modifier = Modifier.testTag("main_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "Menu",
                                            tint = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = ObsidianBg)
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = Color(0xFF0D121B),
                            tonalElevation = 0.dp,
                            modifier = Modifier.border(width = 1.dp, color = Color(0xFF161E2E))
                        ) {
                            navItems.forEachIndexed { index, (title, icon, testTag) ->
                                val selected = selectedTabIndex == index
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { selectedTabIndex = index },
                                    icon = {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = title,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = title,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.testTag(testTag),
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = DetoxMint,
                                        selectedTextColor = DetoxMint,
                                        indicatorColor = Color.Transparent,
                                        unselectedIconColor = Color(0xFF64748B),
                                        unselectedTextColor = Color(0xFF64748B)
                                    )
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    when (selectedTabIndex) {
                        0 -> ChildModeScreen(viewModel = viewModel, contentPadding = innerPadding)
                        1 -> LimitsScreen(viewModel = viewModel, contentPadding = innerPadding)
                        2 -> UsageScreen(
                            viewModel = viewModel,
                            contentPadding = innerPadding,
                            onBackToHome = { selectedTabIndex = 0 }
                        )
                        3 -> SchedulesScreen(viewModel = viewModel, contentPadding = innerPadding)
                        else -> ChildModeScreen(viewModel = viewModel, contentPadding = innerPadding)
                    }

                    // ── Main Menu Bottom Sheet ────────────────────────────────────────
                    if (showInfoSheet) {
                        ModalBottomSheet(
                            onDismissRequest = { showInfoSheet = false },
                            sheetState = sheetState,
                            containerColor = Color(0xFF121722),
                            contentColor = Color.White
                        ) {
                            MainMenuContent(
                                themeMode = themeMode,
                                isClerkSignedIn = isClerkSignedIn,
                                clerkUser = clerkUser,
                                hasParentPin = hasParentPin,
                                onOpenClerkAuth = {
                                    showInfoSheet = false
                                    showClerkAuthDialog = true
                                },
                                onSetPin = {
                                    showInfoSheet = false
                                    showMenuPinSetupDialog = true
                                },
                                onChangePin = {
                                    showInfoSheet = false
                                    showMenuChangePinDialog = true
                                },
                                onRemovePin = {
                                    showInfoSheet = false
                                    showMenuRemovePinDialog = true
                                },
                                onThemeModeChanged = { mode ->
                                    viewModel.setThemeMode(mode)
                                },
                                onDismiss = { showInfoSheet = false }
                            )
                        }
                    }

                    // ── Clerk Auth Dialog ─────────────────────────────────────────────
                    if (showClerkAuthDialog) {
                        ClerkAuthDialog(
                            isSignedIn = isClerkSignedIn,
                            currentUser = clerkUser,
                            publishableKey = clerkPublishableKey,
                            onSignIn = { email, pass, onResult ->
                                viewModel.signInWithClerk(email, pass, onResult)
                            },
                            onSignUp = { email, pass, onResult ->
                                viewModel.signUpWithClerk(email, pass, onResult)
                            },
                            onSignOut = {
                                viewModel.signOutClerk()
                            },
                            onUpdatePublishableKey = { newKey ->
                                viewModel.updateClerkPublishableKey(newKey)
                            },
                            onSignInLocal = { localEmail ->
                                viewModel.signInLocalParent(localEmail)
                            },
                            onDismiss = { showClerkAuthDialog = false }
                        )
                    }

                    // ── Menu-triggered PIN Management Dialogs ─────────────────────────
                    if (showMenuPinSetupDialog) {
                        PinSetupDialog(
                            onSavePin = { viewModel.setParentPin(it) },
                            onDismiss = { showMenuPinSetupDialog = false }
                        )
                    }

                    if (showMenuChangePinDialog) {
                        ChangePinDialog(
                            onVerifyCurrentPin = { viewModel.verifyParentPin(it) },
                            onChangePin = { cur, new -> viewModel.changeParentPin(cur, new) },
                            onDismiss = { showMenuChangePinDialog = false }
                        )
                    }

                    if (showMenuRemovePinDialog) {
                        RemovePinDialog(
                            onRemovePin = { viewModel.removeParentPin(it) },
                            onDismiss = { showMenuRemovePinDialog = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuContent(
    themeMode: String,
    isClerkSignedIn: Boolean,
    clerkUser: com.example.auth.ClerkUserSummary?,
    hasParentPin: Boolean,
    onOpenClerkAuth: () -> Unit,
    onSetPin: () -> Unit,
    onChangePin: () -> Unit,
    onRemovePin: () -> Unit,
    onThemeModeChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DetoxWallLogo(size = 36.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Calm. Simple. Protective.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = DetoxMint
                )
                Text(
                    text = "A safe digital space for your child.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Clerk Parent Account ──────────────────────────────────────────
        Text(
            text = "Parent Account",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1938))
                .border(1.dp, Color(0xFF322E66), RoundedCornerShape(14.dp))
                .clickable { onOpenClerkAuth() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C47FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("C", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isClerkSignedIn) (clerkUser?.fullName ?: "Clerk User") else "Sign in with Clerk",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isClerkSignedIn) (clerkUser?.email ?: "") else "Sync parental controls with Clerk",
                        color = Color(0xFFA5A1C8),
                        fontSize = 12.sp
                    )
                }
            }

            Text(
                text = if (isClerkSignedIn) "Manage" else "Login",
                color = Color(0xFF8B70FF),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Parent Security PIN ───────────────────────────────────────────
        Text(
            text = "Parent Security PIN",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF141A28))
                .border(1.dp, Color(0xFF222F48), RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (hasParentPin) DetoxMint.copy(alpha = 0.18f) else Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = if (hasParentPin) DetoxMint else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (hasParentPin) "PIN Protection Active" else "No PIN Configured",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (hasParentPin) "Protects child settings & limits" else "Set 4-digit PIN to secure controls",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (hasParentPin) DetoxMint.copy(alpha = 0.15f) else Color(0xFF334155))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (hasParentPin) "Active" else "Not Set",
                        color = if (hasParentPin) DetoxMint else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (hasParentPin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onChangePin,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = onRemovePin,
                        modifier = Modifier.weight(1f).height(40.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = LimitRose),
                        border = androidx.compose.foundation.BorderStroke(1.dp, LimitRose.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove PIN", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Button(
                    onClick = onSetPin,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DetoxMint, contentColor = Color(0xFF042F2E))
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Set Parent PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Appearance / Theme Selector ──────────────────────────────────────
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        val themeOptions = listOf(
            SecurityManager.THEME_SYSTEM to "System Default",
            SecurityManager.THEME_LIGHT to "Light",
            SecurityManager.THEME_DARK to "Dark"
        )

        themeOptions.forEach { (key, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onThemeModeChanged(key) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = themeMode == key,
                    onClick = { onThemeModeChanged(key) },
                    colors = RadioButtonDefaults.colors(selectedColor = DetoxMint)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── Feature Info ─────────────────────────────────────────────────────
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        val features = listOf(
            "Child Protection" to "Enables intelligent screen safety with custom limits.",
            "Screen Blackout" to "Automatically turns screen black when time is up.",
            "Smart Presets" to "Quick time presets with custom slider control.",
            "Instant Test" to "Test blackout screen effect anytime.",
            "Siren Alarm" to "Loud alarm alerts you if bypass is attempted."
        )

        features.forEach { (feat, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(DetoxMint)
                        .align(Alignment.CenterVertically)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = feat,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = desc,
                        color = Color(0xFF8E9DAE),
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
