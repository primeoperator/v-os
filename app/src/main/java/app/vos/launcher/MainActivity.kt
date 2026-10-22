package app.vos.launcher

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import app.vos.launcher.model.AppItem
import app.vos.launcher.ui.VosApp

class MainActivity : ComponentActivity() {

    private val vm: LauncherViewModel by viewModels()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart
            val replaced = intent?.action == Intent.ACTION_PACKAGE_REPLACED
            vm.onPackageEvent(pkg, replaced)
        }
    }

    private val roleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        vm.setDefaultHome(isDefaultHome())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, packageReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        // Кнопка «Домой», пока лаунчер уже открыт (launchMode = singleTask).
        addOnNewIntentListener(androidx.core.util.Consumer<Intent> { vm.onHomePressed() })

        setContent {
            VosApp(
                vm = vm,
                onLaunch = ::launchApp,
                onRequestHome = ::requestHomeRole,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        vm.setDefaultHome(isDefaultHome())
        vm.refreshApps()
    }

    override fun onDestroy() {
        unregisterReceiver(packageReceiver)
        super.onDestroy()
    }

    private fun launchApp(app: AppItem) {
        if (!vm.launch(this, app)) {
            Toast.makeText(this, "Не удалось открыть «${app.label}»", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolved?.activityInfo?.packageName == packageName
    }

    /** Запрашивает роль «Домой»; если система не поддерживает — открывает настройки домашнего приложения. */
    private fun requestHomeRole() {
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                roleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        try {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(this, "Откройте Настройки → Приложения → Приложение «Домой»", Toast.LENGTH_LONG).show()
            }
        }
    }
}
