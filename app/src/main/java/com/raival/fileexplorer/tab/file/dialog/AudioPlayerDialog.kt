package com.raival.fileexplorer.tab.file.dialog

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.slider.Slider
import com.raival.fileexplorer.R
import com.raival.fileexplorer.activity.model.MainViewModel
import com.raival.fileexplorer.databinding.AudioPlayerFragmentBinding
import java.io.File
import java.lang.Float.min

class AudioPlayerDialog(private val audioFile: File) :
    BottomSheetDialogFragment() {

    private lateinit var mainViewModel: MainViewModel
    private lateinit var binding: AudioPlayerFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = AudioPlayerFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        val metadata = MediaMetadataRetriever()
        metadata.setDataSource(audioFile.absolutePath)

        val duration =
            metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toFloat()

        val mediaPlayer = MediaPlayer.create(requireContext(), Uri.fromFile(audioFile))
        mediaPlayer.start()

        binding.audioFile.text = audioFile.name

        binding.slider.valueTo = duration
        binding.slider.setLabelFormatter { value: Float ->
            getDurationString(value.toInt() / 1000)
        }

        binding.slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {}

            override fun onStopTrackingTouch(slider: Slider) {
                mediaPlayer.seekTo(slider.value.toInt())
                binding.progress.text = getDurationString(slider.value.toInt() / 1000)
            }
        })

        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                if (mediaPlayer.isPlaying) {
                    binding.slider.value = min(mediaPlayer.currentPosition.toFloat(), duration)
                    binding.progress.text = getDurationString(mediaPlayer.currentPosition / 1000)
                    Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                }
            }
        })

        mediaPlayer.setOnCompletionListener {
            binding.pause.setText(R.string.play)
        }

        binding.pause.setOnClickListener {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                binding.pause.setText(R.string.play)
            } else {
                if (binding.slider.value == duration) {
                    mediaPlayer.seekTo(0)
                }
                mediaPlayer.start()
                binding.pause.setText(R.string.pause)

                Handler(Looper.getMainLooper()).post(object : Runnable {
                    override fun run() {
                        if (mediaPlayer.isPlaying) {
                            binding.slider.value = mediaPlayer.currentPosition.toFloat()
                            Handler(Looper.getMainLooper()).postDelayed(this, 1000)
                        }
                    }
                })
            }
        }

        binding.close.setOnClickListener {
            mediaPlayer.stop()
            mediaPlayer.release()
            dismiss()
        }
    }

    private fun getDurationString(duration: Int): String {
        val hours = duration / 3600
        val minutes = (duration % 3600) / 60
        val seconds = duration % 60
        return if (hours > 0) {
            "${hours}:${minutes}:${seconds}"
        } else {
            "${minutes}:${seconds}"
        }
    }

    override fun getTheme(): Int {
        return R.style.ThemeOverlay_Material3_BottomSheetDialog
    }

}
