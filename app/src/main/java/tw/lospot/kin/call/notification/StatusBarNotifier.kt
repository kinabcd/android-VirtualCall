package tw.lospot.kin.call.notification

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.Notification
import android.app.Notification.CATEGORY_CALL
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.telecom.Connection
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import tw.lospot.kin.call.InCallActivity
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.CallList
import tw.lospot.kin.call.connection.CallSnapshot
import tw.lospot.kin.call.connection.InCallReceiver
import tw.lospot.kin.call.connection.State

class StatusBarNotifier(private val context: Service) {
    companion object {
        private const val NOTIFICATION_ID = 1000
        private const val GROUP_KEY = "calls"
        private const val CHANNEL_ID = "inCallChannel"
    }

    private val jobs = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + jobs)
    private val notificationManager = NotificationManagerCompat.from(context)
    private val contentIntent by lazy {
        PendingIntent.getActivity(
            context, 0, Intent(context, InCallActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        )
    }
    private val bubbleSet = HashMap<Int, CallSnapshot>()
    private fun onCallListChanged(liveCalls: List<CallSnapshot>) {
        val newIds = liveCalls.map { it.id }
        val shouldRemoved = bubbleSet.filter { (id, _) -> id !in newIds }
        ShortcutManagerCompat.removeDynamicShortcuts(
            context,
            shouldRemoved.values.map { "tel:${it.name}" })
        ShortcutManagerCompat.addDynamicShortcuts(context, liveCalls.map { call ->
            // This shortcut must be long-lived and have Person data attached for one or more persons
            ShortcutInfoCompat.Builder(context, "tel:${call.name}")
                .setPerson(call.person())
                .setLongLived(true)
                .setShortLabel(call.name)
                .setIntent(Intent(context, InCallActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                })
                .build()
        })

        shouldRemoved.forEach {
            bubbleSet.remove(it.key)
            notificationManager.cancel(NOTIFICATION_ID + it.value.id)
        }

        if (ContextCompat.checkSelfPermission(context, POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
            return
        }
        liveCalls.forEach { call ->
            bubbleSet[call.id] = call
            val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
                setSmallIcon(R.drawable.notification_small_icon)
                // If the app targets Android 11 or higher, the notification is associated with
                // a valid long-lived dynamic or cached sharing shortcut.
                setShortcutId("tel:${call.name}")
                setCategory(CATEGORY_CALL)
                setOngoing(true)
                setGroup(GROUP_KEY)
                setContentTitle(call.name)
                createActions(call).forEach { action -> addAction(action) }
                addPerson(Person.Builder().apply {
                    setUri("tel:${call.name}")
                }.build())
                setStyle(createMessagingStyle(call)) // Notification must use MessagingStyle for bubble.
                bubbleMetadata = createBubbleMetadata(call)
            }
            notificationManager.notify(NOTIFICATION_ID + call.id, builder.build())
        }
    }

    private fun createMessagingStyle(call: CallSnapshot): NotificationCompat.Style {
        val person = call.person()
        val style = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(call.name)
            .setGroupConversation(call.isConference)
        if (call.isConference) {
            call.children.forEach { child ->
                style.addMessage("Child", System.currentTimeMillis(), child.person())
            }
        } else {
            style.addMessage("${State.find(call.state)}", System.currentTimeMillis(), person)
        }
        return style
    }

    private fun createBubbleMetadata(call: CallSnapshot): NotificationCompat.BubbleMetadata {
        val bubbleIntent = Intent("tw.lospot.kin.call.BubbleContent").apply {
            setClass(context, BubbleActivity::class.java)
            putExtra("callId", call.id)
            data = Uri.fromParts("KinCall", "bubble", "${call.id}")
        }
        val bubblePendingIntent = PendingIntent.getActivity(
            context, 0, bubbleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        )
        val icon = IconCompat.createWithResource(context, R.drawable.notification_small_icon)
        return NotificationCompat.BubbleMetadata.Builder(bubblePendingIntent, icon)
            .setAutoExpandBubble(false)
            .build()
    }

    fun setUp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Ongoing Call",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        context.startForeground(NOTIFICATION_ID, createSummaryNotification())
        scope.launch { CallList.rootCalls.collect { onCallListChanged(it) } }
    }

    fun cleanUp() {
        jobs.cancelChildren()
        context.stopForeground(Service.STOP_FOREGROUND_REMOVE)
    }

    private fun createActions(call: CallSnapshot): List<NotificationCompat.Action> {
        return when (call.state) {
            Connection.STATE_DIALING -> arrayListOf(
                createAnswerAction(call.id),
                createDisconnectAction(call.id)
            )

            else -> arrayListOf(
                createDisconnectAction(call.id)
            )
        }
    }

    private fun createAnswerAction(callId: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_answer_call),
            context.getString(R.string.answer_call),
            PendingIntent.getBroadcast(
                context,
                callId,
                createSelfIntent(InCallReceiver.ACTION_ANSWER, callId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }

    private fun createDisconnectAction(callId: Int): NotificationCompat.Action {
        return NotificationCompat.Action.Builder(
            IconCompat.createWithResource(context, R.drawable.ic_end_call),
            context.getString(R.string.disconnect_call),
            PendingIntent.getBroadcast(
                context,
                callId,
                createSelfIntent(InCallReceiver.ACTION_DISCONNECT, callId),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        ).build()
    }

    private fun createSelfIntent(action: String, callId: Int): Intent {
        return Intent(action)
            .setPackage(context.packageName)
            .putExtra(InCallReceiver.EXTRA_CALL_ID, callId)
    }

    private fun createSummaryNotification(): Notification {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID).apply {
            setContentIntent(contentIntent)
            setSmallIcon(R.drawable.notification_small_icon)
            setCategory(CATEGORY_CALL)
            setOngoing(true)
            setGroup(GROUP_KEY)
            setGroupSummary(true)
            setContentTitle("InCall")
        }
        return builder.build()
    }

    private fun CallSnapshot.person(): Person = Person.Builder()
        .setName(name)
        .setKey("Call_$id")
        .setUri("tel:$name")
        .build()
}