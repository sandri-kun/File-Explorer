package com.raival.fileexplorer.glide

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import com.raival.fileexplorer.glide.apk.ApkIconModelLoaderFactory
import com.raival.fileexplorer.glide.icon.IconModelLoaderFactory
import com.raival.fileexplorer.glide.model.IconRes
import com.raival.fileexplorer.glide.svg.SvgDecoder
import com.raival.fileexplorer.glide.svg.SvgDrawableTranscoder
import java.io.InputStream

@GlideModule
class FileExplorerGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(
            String::class.java,
            Drawable::class.java,
            ApkIconModelLoaderFactory(context)
        )
        registry.prepend(IconRes::class.java, Drawable::class.java, IconModelLoaderFactory(context))
        registry
            .register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder())
            .append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }
}
