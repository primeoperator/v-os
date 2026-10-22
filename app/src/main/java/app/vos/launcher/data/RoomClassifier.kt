package app.vos.launcher.data

import android.content.pm.ApplicationInfo
import app.vos.launcher.model.AppItem
import app.vos.launcher.model.Room

/**
 * Определяет комнату по умолчанию для приложения и «красивые» стартовые позиции
 * для Telegram, браузера, камеры и GitHub. Пользователь всё может переставить вручную.
 */
object RoomClassifier {

    private val COMM_EXACT = setOf(
        "org.thoughtcrime.securesms", "com.discord", "com.slack", "us.zoom.videomeetings",
        "com.microsoft.teams", "com.skype.raider", "com.vkontakte.android", "com.vk.im",
        "ru.oneme.app", "com.tencent.mm", "jp.naver.line.android", "com.viber.voip",
        "com.google.android.gm", "com.microsoft.office.outlook", "com.android.email",
        "com.google.android.apps.tachyon", "com.instagram.android", "com.facebook.orca",
        "com.facebook.katana", "com.twitter.android",
    )
    private val COMM_PARTS = listOf(
        "telegram", "whatsapp", "messag", "dialer", "contacts", "challegram", "mms",
    )

    private val CREATE_EXACT = setOf("com.google.android.keep")
    private val CREATE_PARTS = listOf(
        "github", "termux", "docs.editors", "figma", "canva", "adobe", "capcut", "inshot",
        "notion", "obsidian", "jetbrains", "androidstudio", "anthropic", "openai", "snapseed",
        "picsart", "lightroom", "notes", "notepad", "office", "kinemaster", "filmora", "bandlab",
    )

    private val LEISURE_PARTS = listOf(
        "youtube", "spotify", "netflix", "twitch", "music", "game", "tiktok", "musically",
        "kinopoisk", "soundcloud", "podcast", "deezer", "vlc", "steam", "ebook", "audiobook",
        "litres", "video", "radio", "player",
    )

    fun roomFor(app: AppItem): Room {
        val pkg = app.packageName.lowercase()
        val label = app.label.lowercase()

        if (app.isBrowser) return Room.HOME
        if (pkg in COMM_EXACT || COMM_PARTS.any { it in pkg || it in label }) return Room.COMMUNICATION
        if (app.isCamera || pkg in CREATE_EXACT || CREATE_PARTS.any { it in pkg || it in label }) return Room.CREATE
        if (LEISURE_PARTS.any { it in pkg || it in label }) return Room.LEISURE

        return when (app.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> Room.COMMUNICATION
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Room.CREATE
            ApplicationInfo.CATEGORY_GAME,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_NEWS -> Room.LEISURE
            else -> Room.HOME
        }
    }

    /** Стартовая позиция (x — доля ширины, y — dp) для особых приложений или null. */
    fun featuredSpot(app: AppItem): Pair<Float, Float>? {
        val pkg = app.packageName.lowercase()
        val label = app.label.lowercase()
        return when {
            app.isBrowser -> 0.5f to 200f
            "telegram" in pkg || "challegram" in pkg || label == "telegram" -> 0.5f to 200f
            "github" in pkg || label == "github" -> 0.18f to 150f
            app.isCamera -> 0.82f to 270f
            else -> null
        }
    }
}
