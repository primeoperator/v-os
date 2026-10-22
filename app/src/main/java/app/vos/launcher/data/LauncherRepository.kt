package app.vos.launcher.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import app.vos.launcher.model.AppItem
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Работа с PackageManager: список запускаемых приложений, иконки, запуск. */
class LauncherRepository(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    /** Все приложения с иконкой в списке запуска (кроме самого V-OS), отсортированные по имени. */
    suspend fun loadApps(): List<AppItem> = withContext(Dispatchers.Default) {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
            .addCategory(Intent.CATEGORY_BROWSABLE)

        val browserPackages = queryActivities(browserIntent)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

        queryActivities(launcherIntent)
            .mapNotNull { toAppItem(it, browserPackages) }
            .distinctBy { it.key }
            .sortedBy { it.label.lowercase() }
    }

    /** Запускает приложение. Возвращает false, если запуск не удался (например, приложение удалено). */
    fun launch(context: Context, app: AppItem): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    /** Сбрасывает кэш иконок пакета (после обновления приложения). */
    fun invalidate(packageName: String) {
        iconCache.entries.removeAll { it.key.startsWith("$packageName/") }
    }

    @Suppress("DEPRECATION")
    private fun queryActivities(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= 33) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            pm.queryIntentActivities(intent, 0)
        }

    private fun toAppItem(info: ResolveInfo, browserPackages: Set<String>): AppItem? {
        val activity = info.activityInfo ?: return null
        if (activity.packageName == appContext.packageName) return null

        val key = "${activity.packageName}/${activity.name}"
        val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifEmpty { activity.packageName }
        val lowerLabel = label.lowercase()
        val lowerPackage = activity.packageName.lowercase()

        val icon = iconCache[key] ?: runCatching { render(info.loadIcon(pm)) }
            .getOrNull()
            ?.also { iconCache[key] = it }

        return AppItem(
            packageName = activity.packageName,
            activityName = activity.name,
            label = label,
            icon = icon,
            category = activity.applicationInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED,
            isBrowser = activity.packageName in browserPackages,
            isCamera = "camera" in lowerPackage || "camera" in lowerLabel || "камера" in lowerLabel,
        )
    }

    private fun render(drawable: Drawable): ImageBitmap =
        drawable.toBitmap(ICON_PX, ICON_PX, Bitmap.Config.ARGB_8888).asImageBitmap()

    private companion object {
        const val ICON_PX = 168
    }
}
