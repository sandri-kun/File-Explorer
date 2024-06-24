package com.raival.fileexplorer.activity

import android.os.Bundle
import com.bumptech.glide.Glide
import com.raival.fileexplorer.databinding.ActivityPhotoViewerBinding
import java.io.File

class ImageViewerActivity : BaseActivity<ActivityPhotoViewerBinding>() {

    override fun getViewBinding() = ActivityPhotoViewerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val image = File(intent.getStringExtra("file")!!)

        Glide.with(this).load(image).into(binding.image)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setTitle(image.name)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
}
