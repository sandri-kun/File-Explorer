package com.raival.fileexplorer.activity

import android.graphics.drawable.PictureDrawable
import android.os.Bundle
import com.bumptech.glide.Glide
import com.raival.fileexplorer.databinding.ActivityPhotoViewerBinding
import com.raival.fileexplorer.glide.svg.SvgSoftwareLayerSetter
import java.io.File

class ImageViewerActivity : BaseActivity<ActivityPhotoViewerBinding>() {

    override fun getViewBinding() = ActivityPhotoViewerBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val image = File(intent.getStringExtra("file")!!)

        if (image.extension == "svg") {
            Glide.with(this)
                .`as`(PictureDrawable::class.java)
                .listener(SvgSoftwareLayerSetter())
                .fitCenter()
                .load(image)
                .into(binding.image)
        } else {
            Glide.with(this).load(image).fitCenter().into(binding.image)
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = image.name
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
}
