package tw.lospot.kin.call.notification

import android.annotation.TargetApi
import android.app.*
import android.app.Notification.CATEGORY_CALL
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Icon
import android.os.Build
import tw.lospot.kin.call.InCallActivity
import tw.lospot.kin.call.R
import tw.lospot.kin.call.connection.Call
import tw.lospot.kin.call.connection.CallList

class StatusBarNotifier(private val context: Context) : CallList.Listener {
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val GROUP_KEY = "tw.lospot.kin.call.bubble"
        private const val CHANNEL_ID = "inCallChannel"
    }

    private val notificationManager = context.getSystemService(NotificationManager::class.java)!!
    private val contentIntent by lazy { PendingIntent.getActivity(context, 0, Intent(context, InCallActivity::class.java), 0) }
    private val bubbleIcon by lazy { createBubbleIcon() }
    private val bubbleSet = HashSet<Call>()
    override fun onCallListChanged() {
        val liveCalls = CallList.rootCalls
        val shouldRemoved = bubbleSet.filter { !liveCalls.contains(it) }
        shouldRemoved.forEach {
            bubbleSet.remove(it)
            notificationManager.cancel("${it.id}", NOTIFICATION_ID)
        }


        liveCalls.forEach {
            bubbleSet.add(it)

            val builder = NotificationHelper.createBuilder(context, CHANNEL_ID)
            builder.apply {
                setSmallIcon(R.drawable.notification_small_icon)
                setCategory(CATEGORY_CALL)
                setOngoing(true)
                setGroup(GROUP_KEY)
                setContentTitle(it.name)
            }
            notificationManager.notify("${it.id}", NOTIFICATION_ID, builder.build())
        }
    }

    fun setUp() {
        NotificationHelper.createChannel(context, CHANNEL_ID, "Ongoing Call", NotificationManager.IMPORTANCE_DEFAULT)
        if (context is Service) {
            context.startForeground(NOTIFICATION_ID, createSummaryNotification())
        }
        CallList.addListener(this)
    }

    fun cleanUp() {
        CallList.removeListener(this)
        if (context is Service) {
            context.stopForeground(true)
        }
    }

    private fun createSummaryNotification(): Notification {
        val builder = NotificationHelper.createBuilder(context, CHANNEL_ID)
        builder.apply {
            setContentIntent(contentIntent)
            setSmallIcon(R.drawable.notification_small_icon)
            setCategory(CATEGORY_CALL)
            setOngoing(true)
            setGroup(GROUP_KEY)
            setGroupSummary(true)
            setContentTitle("InCall")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bubbleData = Notification.BubbleMetadata.Builder()
                    .setDesiredHeight(600)
                    .setIcon(bubbleIcon)
                    .setIntent(contentIntent)
                    .build()
            val person = Person.Builder()
                    .setBot(true)
                    .setImportant(true)
                    .build()
            builder.apply {
                setBubbleMetadata(bubbleData)
                addPerson(person)
            }
        }
        return builder.build()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun createBubbleIcon(): Icon {
        val rawIcon = context.resources.getDrawable(R.drawable.ic_launcher_foreground, context.theme).apply {
            bounds = Rect(0, 0, intrinsicWidth, intrinsicHeight)
        }
        val bitmap = Bitmap.createBitmap(rawIcon.intrinsicWidth, rawIcon.intrinsicHeight, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(context.getColor(R.color.ic_launcher_background))
            rawIcon.draw(this)
        }
        return Icon.createWithAdaptiveBitmap(bitmap)
    }
}