package com.raival.fileexplorer.activity

import android.os.Bundle
import android.text.Html
import com.github.difflib.text.DiffRowGenerator
import com.raival.fileexplorer.databinding.ActivityTextCompareBinding
import java.io.File

class TextCompareActivity : BaseActivity<ActivityTextCompareBinding>() {

    override fun getViewBinding() = ActivityTextCompareBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val first = (intent.getSerializableExtra("file1")!! as File).readLines()
        val second = (intent.getSerializableExtra("file2")!! as File).readLines()

        val generator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .build()

        val rows = generator.generateDiffRows(
            first,
            second
        )

        val unified = rows.joinToString("\n") { it.oldLine }

        binding.editor.text = Html.fromHtml(unified, Html.FROM_HTML_MODE_COMPACT)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Text Compare"
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }
}
