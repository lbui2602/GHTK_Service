package com.example.ghtk_service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.example.ghtk_service.R.raw.demo
import com.example.ghtk_service.databinding.ActivityMainBinding
import kotlinx.coroutines.*

class MusicService() : Service() {
    private lateinit var mediaPlayer: MediaPlayer
    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private lateinit var playlist: List<Song>
    private var currentSongIndex = 0
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    override fun onCreate() {
        super.onCreate()
        playlist = listOf(
            Song("demo", R.raw.demo),
            Song("Rồi ta sẽ ngắm pháo hoa cùng nhau",R.raw.roitasengam),
            Song("demo2", R.raw.demo2),
            Song("Kieu ngao",R.raw.kieungao),
            Song("Co the hay khong",R.raw.cothehaykhong),
            Song("Doi den thang 13",R.raw.doidenthang13),
            Song("Con di dau de thay hoa bay",R.raw.didaudethayhoabay))
        initializeMediaPlayer()
        startPositionUpdater()
    }
    private fun initializeMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, playlist[currentSongIndex].song)
        mediaPlayer.setOnCompletionListener {
            nextMusic()
        }
        sendSongNameBroadcast()
    }
    private fun startPositionUpdater() {
        serviceScope.launch {
            while (true) {
                if (mediaPlayer.isPlaying) {
                    val intent = Intent("MusicServicePositionUpdate")
                    intent.putExtra("position", mediaPlayer.currentPosition)
                    intent.putExtra("duration", mediaPlayer.duration)
                    sendBroadcast(intent)
                }
                delay(1000)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "BACK" -> back()
            "PLAY" -> playMusic()
            "PAUSE" -> pauseMusic()
            "NEXT" -> nextMusic()
            "STOP" -> stopSelf()
            "MusicServiceSeekTo" -> {
                val seekTo = intent.getIntExtra("seekTo", 0)
                Log.e("TAG",seekTo.toString())
                mediaPlayer.seekTo(seekTo)
            }
        }

        startForegroundService()
        return START_STICKY
    }


    private fun playMusic() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            sendSongNameBroadcast()
            updateNotification()
        }
    }
    private fun sendSongNameBroadcast() {
        val intent = Intent("MusicServiceSongUpdate")
        intent.putExtra("songName", playlist[currentSongIndex].name)
        sendBroadcast(intent)
    }

    private fun pauseMusic() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            updateNotification()
        }
    }

    private fun nextMusic() {
        mediaPlayer.release()
        currentSongIndex = (currentSongIndex + 1) % playlist.size
        initializeMediaPlayer()
        mediaPlayer.start()
        sendSongNameBroadcast()
        updateNotification()
    }
    private fun back() {
        Log.e("TAG","back")
        mediaPlayer.release()
        if(currentSongIndex==0){
            currentSongIndex= playlist.size - 1
        }else{
            currentSongIndex = (currentSongIndex - 1) % playlist.size
        }
        initializeMediaPlayer()
        mediaPlayer.start()
        sendSongNameBroadcast()
        updateNotification()
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(1, notification)
    }
    private fun createNotification(): Notification {
        val notificationLayout = RemoteViews(packageName, R.layout.custom_notification)

        // Update the layout with current song details
        notificationLayout.setTextViewText(R.id.notification_title, playlist[currentSongIndex].name)

        val playIntent = Intent(this, MusicService::class.java).apply {
            action = "PLAY"
        }
        val playPendingIntent = PendingIntent.getService(this, 0, playIntent, PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_play, playPendingIntent)

        val pauseIntent = Intent(this, MusicService::class.java).apply {
            action = "PAUSE"
        }
        val pausePendingIntent = PendingIntent.getService(this, 0, pauseIntent, PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_pause, pausePendingIntent)

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = "NEXT"
        }
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_back, nextPendingIntent)

        val backIntent = Intent(this, MusicService::class.java).apply {
            action = "BACK"
        }
        val backPendingIntent = PendingIntent.getService(this, 0, backIntent, PendingIntent.FLAG_IMMUTABLE)
        notificationLayout.setOnClickPendingIntent(R.id.notification_back, backPendingIntent)

        val notificationBuilder = NotificationCompat.Builder(this, "MusicChannel")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                "MusicChannel",
                "Music Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(notificationChannel)
        }

        return notificationBuilder.build()
    }



    @SuppressLint("NotificationPermission")
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
        serviceScope.cancel()
    }

}
