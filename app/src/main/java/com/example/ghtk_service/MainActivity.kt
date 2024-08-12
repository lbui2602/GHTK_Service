package com.example.ghtk_service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.ghtk_service.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var positionUpdateReceiver: BroadcastReceiver
    private lateinit var binding: ActivityMainBinding
    private lateinit var audioManager: AudioManager
    private lateinit var volumeObserver: ContentObserver
    private lateinit var songUpdateReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.backButton.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply { action = "BACK" })
        }

        binding.playButton.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply { action = "PLAY" })
        }

        binding.pauseButton.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply { action = "PAUSE" })
        }

        binding.nextButton.setOnClickListener {
            startService(Intent(this, MusicService::class.java).apply { action = "NEXT" })
        }
        songUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val songName = intent?.getStringExtra("songName")
                binding.tvNameSong.text = songName
            }
        }
        val intentFilter = IntentFilter("MusicServiceSongUpdate")
        registerReceiver(songUpdateReceiver, intentFilter)

        setupVolumeSeekBar()
        setupPositionSeekBar()

        positionUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val position = intent.getIntExtra("position", 0)
                val duration = intent.getIntExtra("duration", 0)
                binding.seekBar.max = duration
                binding.seekBar.progress = position
            }
        }

        registerReceiver(positionUpdateReceiver, IntentFilter("MusicServicePositionUpdate"))

        volumeObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateVolumeSeekBar()
            }
        }
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
    }

    private fun setupVolumeSeekBar() {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        binding.volumeSeekBar.max = maxVolume
        binding.volumeSeekBar.progress = currentVolume

        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPositionSeekBar() {
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intent = Intent(this@MainActivity, MusicService::class.java)
                    intent.action = "MusicServiceSeekTo"
                    intent.putExtra("seekTo", progress)
                    startService(intent)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateVolumeSeekBar() {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.volumeSeekBar.progress = currentVolume
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(positionUpdateReceiver)
        contentResolver.unregisterContentObserver(volumeObserver)
    }
}
