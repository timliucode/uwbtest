package com.example.uwbtest.presentation.screen.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.uwbtest.BuildConfig
import com.example.uwbtest.R

private const val REPO_URL = "https://github.com/timliudev/uwb-tester"

private fun Context.openUrl(url: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, url, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── App icon + name + version ──────────────────────
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp),
            )
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                stringResource(
                    R.string.about_version,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = stringResource(R.string.about_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ── GitHub link ────────────────────────────────────
            Row(
                modifier = Modifier
                    .clickable { context.openUrl(REPO_URL) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.about_open_source),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            HorizontalDivider()

            // ── Dependencies ───────────────────────────────────
            Text(
                stringResource(R.string.about_dependencies_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.fillMaxWidth(),
            )

            DependenciesCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class Dep(
    val name: String,
    val version: String,
    @StringRes val licenseRes: Int,
    val url: String,
)

private val DEPENDENCIES = listOf(
    Dep("Jetpack Compose BOM", "2026.05.01", R.string.about_apache2, "https://developer.android.com/jetpack/compose"),
    Dep("Material 3", "(via BOM)", R.string.about_apache2, "https://m3.material.io"),
    Dep("AndroidX Navigation Compose", "2.9.8", R.string.about_apache2, "https://developer.android.com/jetpack/androidx/releases/navigation"),
    Dep("Hilt", "2.59.2", R.string.about_apache2, "https://dagger.dev/hilt"),
    Dep("AndroidX UWB", "1.0.0", R.string.about_apache2, "https://developer.android.com/jetpack/androidx/releases/core-uwb"),
    Dep("AndroidX Lifecycle", "2.10.0", R.string.about_apache2, "https://developer.android.com/jetpack/androidx/releases/lifecycle"),
    Dep("Kotlin Coroutines", "1.11.0", R.string.about_apache2, "https://github.com/Kotlin/kotlinx.coroutines"),
    Dep("qrcode-kotlin", "4.5.0", R.string.about_mit, "https://github.com/g0dkar/qrcode-kotlin"),
    Dep("quickie", "1.11.0", R.string.about_mit, "https://github.com/G00fY2/quickie"),
)

@Composable
private fun DependenciesCard() {
    val context = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DEPENDENCIES.forEachIndexed { idx, dep ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.openUrl(dep.url) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dep.name, style = MaterialTheme.typography.bodySmall)
                        Text(
                            dep.version,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(dep.licenseRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (idx < DEPENDENCIES.lastIndex) {
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        }
    }
}
