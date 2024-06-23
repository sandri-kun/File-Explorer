package com.raival.fileexplorer.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.menu.MenuBuilder
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.elevation.SurfaceColors
import com.raival.fileexplorer.App.Companion.showMsg
import com.raival.fileexplorer.R
import com.raival.fileexplorer.activity.editor.autocomplete.CustomCompletionItemAdapter
import com.raival.fileexplorer.activity.editor.autocomplete.CustomCompletionLayout
import com.raival.fileexplorer.activity.editor.language.java.JavaCodeLanguage
import com.raival.fileexplorer.activity.editor.language.json.JsonLanguage
import com.raival.fileexplorer.activity.editor.language.kotlin.KotlinCodeLanguage
import com.raival.fileexplorer.activity.editor.language.xml.XmlLanguage
import com.raival.fileexplorer.activity.editor.scheme.DarkScheme
import com.raival.fileexplorer.activity.editor.scheme.LightScheme
import com.raival.fileexplorer.activity.model.TextEditorViewModel
import com.raival.fileexplorer.databinding.TextEditorActivityBinding
import com.raival.fileexplorer.tab.file.misc.FileMimeTypes
import com.raival.fileexplorer.util.Log
import com.raival.fileexplorer.util.PrefsUtils
import com.raival.fileexplorer.util.Utils
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.widget.EditorSearcher.SearchOptions
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.Magnifier
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Locale

class TextEditorActivity : BaseActivity<TextEditorActivityBinding>() {
    private lateinit var editorViewModel: TextEditorViewModel

    override fun getViewBinding() = TextEditorActivityBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        editorViewModel = ViewModelProvider(this).get(TextEditorViewModel::class.java)

        setupSearchPanel()

        binding.symbolInput.apply {
            bindEditor(binding.editor)
                .setTextColor(
                    Utils.getColorAttribute(
                        R.attr.colorOnSurface,
                        this@TextEditorActivity
                    )
                )
                .setBackgroundColor(SurfaceColors.SURFACE_2.getColor(this@TextEditorActivity))
            addSymbol("->", "    ")
            addSymbols(arrayOf("_", "=", "{", "}", "<", ">", "|", "\\", "?", "+", "-", "*", "/"))
        }

        binding.editor.apply {
            getComponent(EditorAutoCompletion::class.java).setLayout(CustomCompletionLayout())
            getComponent(EditorAutoCompletion::class.java)
                .setAdapter(CustomCompletionItemAdapter())
            typefaceText = Typeface.createFromAsset(assets, "font/JetBrainsMono-Regular.ttf")
            props.useICULibToSelectWords = false
            props.symbolPairAutoCompletion = false
            props.deleteMultiSpaces = -1
            props.deleteEmptyLineFast = false
        }

        loadEditorPrefs()

        if (editorViewModel.file == null) editorViewModel.file =
            File(intent.getStringExtra("file")!!)
        detectLanguage(editorViewModel.file!!)

        binding.toolbar.title = editorViewModel.file!!.name
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }

        if (!editorViewModel.file!!.exists()) {
            showMsg("File not found")
            finish()
        }

        if (editorViewModel.file!!.isDirectory) {
            showMsg("Invalid file")
            finish()
        }

        try {
            if (editorViewModel.content != null) {
                binding.editor.setText(editorViewModel.content.toString())
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    val content = editorViewModel.file!!.readText()

                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.editor.setText(content)
                    }
                }
            }
        } catch (exception: Exception) {
            Log.e(
                TAG,
                Log.SOMETHING_WENT_WRONG + " while reading file: " + editorViewModel.file!!.absolutePath,
                exception
            )
            showMsg("Failed to read file: " + editorViewModel.file!!.absolutePath)
            finish()
        }
    }

    private fun detectLanguage(file: File) {
        when (file.extension.lowercase(Locale.getDefault())) {
            FileMimeTypes.javaType -> setEditorLanguage(LANGUAGE_JAVA)
            FileMimeTypes.kotlinType -> setEditorLanguage(LANGUAGE_KOTLIN)
            "json" -> setEditorLanguage(LANGUAGE_JSON)
            "xml" -> setEditorLanguage(LANGUAGE_XML)
            else -> setEditorLanguage(-1)
        }
    }

    private fun setupSearchPanel() {
        binding.findInput.hint = "Find text"
        binding.replaceInput.hint = "Replacement"

        binding.findInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                if (editable.isNotEmpty()) {
                    binding.editor.searcher.search(
                        editable.toString(),
                        SearchOptions(false, false)
                    )
                } else {
                    binding.editor.searcher.stopSearch()
                }
            }
        })
        binding.searchPanel.apply {
            binding.next.setOnClickListener { if (binding.editor.searcher.hasQuery()) binding.editor.searcher.gotoNext() }
            binding.previous.setOnClickListener { if (binding.editor.searcher.hasQuery()) binding.editor.searcher.gotoPrevious() }
            binding.replace.setOnClickListener {
                if (binding.editor.searcher.hasQuery()) binding.editor.searcher.replaceThis(
                    binding.replaceInput.text.toString()
                )
            }
            binding.replaceAll.setOnClickListener {
                if (binding.editor.searcher.hasQuery()) binding.editor.searcher.replaceAll(
                    binding.replaceInput.text.toString()
                )
            }
        }
    }

    public override fun onStop() {
        super.onStop()
        editorViewModel.content = binding.editor.text
    }

    override fun onBackPressed() {
        if (binding.searchPanel.visibility == View.VISIBLE) {
            binding.searchPanel.visibility = View.GONE
            binding.editor.searcher.stopSearch()
            return
        }
        try {
            if (editorViewModel.file?.readText() != binding.editor.text.toString()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Save File")
                    .setMessage("Do you want to save this file before exit?")
                    .setPositiveButton("Yes") { _, _ ->
                        saveFile(binding.editor.text.toString())
                        finish()
                    }
                    .setNegativeButton("No") { _, _ -> finish() }
                    .show()
                return
            }
        } catch (exception: Exception) {
            Log.w(TAG, exception)
        }
        super.onBackPressed()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.text_editor_menu, menu)
        (menu as MenuBuilder).setOptionalIconsVisible(true)
        menu.findItem(R.id.editor_option_wordwrap).isChecked =
            PrefsUtils.TextEditor.textEditorWordwrap
        menu.findItem(R.id.editor_option_magnifier).isChecked =
            PrefsUtils.TextEditor.textEditorMagnifier
        menu.findItem(R.id.editor_option_pin_line_number).isChecked =
            PrefsUtils.TextEditor.textEditorPinLineNumber
        menu.findItem(R.id.editor_option_line_number).isChecked =
            PrefsUtils.TextEditor.textEditorShowLineNumber
        menu.findItem(R.id.editor_option_read_only).isChecked =
            PrefsUtils.TextEditor.textEditorReadOnly
        menu.findItem(R.id.editor_option_autocomplete).isChecked =
            PrefsUtils.TextEditor.textEditorAutocomplete
        return super.onCreateOptionsMenu(menu)
    }

    private fun loadEditorPrefs() {
        binding.editor.apply {
            setPinLineNumber(PrefsUtils.TextEditor.textEditorPinLineNumber)
            isWordwrap = PrefsUtils.TextEditor.textEditorWordwrap
            isLineNumberEnabled = PrefsUtils.TextEditor.textEditorShowLineNumber
            getComponent(Magnifier::class.java).isEnabled =
                PrefsUtils.TextEditor.textEditorMagnifier
            isEditable = !PrefsUtils.TextEditor.textEditorReadOnly
            getComponent(EditorAutoCompletion::class.java).isEnabled =
                PrefsUtils.TextEditor.textEditorAutocomplete
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        binding.apply {
            if (id == R.id.editor_format) {
                editor.formatCodeAsync()
            } else if (id == R.id.editor_language_def) {
                item.isChecked = true
                editor.setEditorLanguage(null)
            } else if (id == R.id.editor_language_java) {
                item.isChecked = true
                setEditorLanguage(LANGUAGE_JAVA)
            } else if (id == R.id.editor_language_kotlin) {
                item.isChecked = true
                setEditorLanguage(LANGUAGE_KOTLIN)
            } else if (id == R.id.editor_option_read_only) {
                item.isChecked = !item.isChecked
                PrefsUtils.TextEditor.textEditorReadOnly = item.isChecked
                editor.isEditable = !item.isChecked
            } else if (id == R.id.editor_option_search) {
                if (searchPanel.visibility == View.GONE) {
                    searchPanel.visibility = View.VISIBLE
                } else {
                    searchPanel.visibility = View.GONE
                    editor.searcher.stopSearch()
                }
            } else if (id == R.id.editor_option_save) {
                saveFile(editor.text.toString())
                showMsg("Saved successfully")
            } else if (id == R.id.editor_option_text_undo) {
                editor.undo()
            } else if (id == R.id.editor_option_text_redo) {
                editor.redo()
            } else if (id == R.id.editor_option_wordwrap) {
                item.isChecked = !item.isChecked
                PrefsUtils.TextEditor.textEditorWordwrap = item.isChecked
                editor.isWordwrap = item.isChecked
            } else if (id == R.id.editor_option_magnifier) {
                item.isChecked = !item.isChecked
                editor.getComponent(Magnifier::class.java).isEnabled =
                    item.isChecked
                PrefsUtils.TextEditor.textEditorMagnifier = item.isChecked
            } else if (id == R.id.editor_option_line_number) {
                item.isChecked = !item.isChecked
                PrefsUtils.TextEditor.textEditorShowLineNumber = item.isChecked
                editor.isLineNumberEnabled = item.isChecked
            } else if (id == R.id.editor_option_pin_line_number) {
                item.isChecked = !item.isChecked
                PrefsUtils.TextEditor.textEditorPinLineNumber = item.isChecked
                editor.setPinLineNumber(item.isChecked)
            } else if (id == R.id.editor_option_autocomplete) {
                item.isChecked = !item.isChecked
                PrefsUtils.TextEditor.textEditorAutocomplete = item.isChecked
                editor.getComponent(EditorAutoCompletion::class.java).isEnabled = item.isChecked
            } else if (id == R.id.editor_option_smooth_mode) {
                item.isChecked = !item.isChecked
                if (item.isChecked) {
                    editor.setEditorLanguage(EmptyLanguage())
                } else {
                    detectLanguage(editorViewModel.file!!)
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setEditorLanguage(language: Int) {
        binding.editor.apply {
            when (language) {
                LANGUAGE_JAVA -> {
                    colorScheme = getColorScheme(false)
                    setEditorLanguage(javaLanguage)
                }

                LANGUAGE_KOTLIN -> {
                    colorScheme = getColorScheme(true)
                    setEditorLanguage(kotlinLang)
                }

                LANGUAGE_XML -> {
                    colorScheme = getColorScheme(true)
                    setEditorLanguage(xmlLang)
                }

                LANGUAGE_JSON -> {
                    colorScheme = getColorScheme(true)
                    setEditorLanguage(jsonLang)
                }

                else -> {
                    colorScheme = getColorScheme(false)
                    setEditorLanguage(EmptyLanguage())
                }
            }
        }
    }

    private val javaLanguage: Language
        get() = JavaCodeLanguage()

    private val jsonLang: Language
        get() = JsonLanguage()
    private val xmlLang: Language
        get() = XmlLanguage()
    private val kotlinLang: Language
        get() = KotlinCodeLanguage()

    private fun getColorScheme(isTextmate: Boolean): EditorColorScheme {
        return if (Utils.isDarkMode) getDarkScheme(isTextmate) else getLightScheme(isTextmate)
    }

    private fun getLightScheme(isTextmate: Boolean): EditorColorScheme {
        val scheme: EditorColorScheme = if (isTextmate) {
            try {
                ThemeRegistry.getInstance().findThemeByThemeName("QuietLight")?.let {
                    TextMateColorScheme.create(ThemeModel(it.themeSource))
                } ?: LightScheme()
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    Log.SOMETHING_WENT_WRONG + " while creating light scheme for textmate language",
                    e
                )
                showMsg(Log.UNABLE_TO + " load: textmate/light.tmTheme")
                LightScheme()
            }
        } else {
            LightScheme()
        }
        scheme.apply {
            setColor(
                EditorColorScheme.WHOLE_BACKGROUND,
                SurfaceColors.SURFACE_0.getColor(this@TextEditorActivity)
            )
            setColor(
                EditorColorScheme.LINE_NUMBER_BACKGROUND,
                SurfaceColors.SURFACE_0.getColor(this@TextEditorActivity)
            )
            setColor(
                EditorColorScheme.COMPLETION_WND_BACKGROUND,
                SurfaceColors.SURFACE_1.getColor(this@TextEditorActivity)
            )
            setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, Color.RED)
        }
        return scheme
    }

    private fun getDarkScheme(isTextmate: Boolean): EditorColorScheme {
        val scheme: EditorColorScheme = if (isTextmate) {
            try {
                ThemeRegistry.getInstance().findThemeByThemeName("darcula")?.let {
                    TextMateColorScheme.create(ThemeModel(it.themeSource))
                } ?: DarkScheme()
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    Log.SOMETHING_WENT_WRONG + " while creating dark scheme for textmate language",
                    e
                )
                showMsg(Log.UNABLE_TO + " load: textmate/dark.json")
                DarkScheme()
            }
        } else {
            DarkScheme()
        }
        scheme.apply {
            setColor(
                EditorColorScheme.WHOLE_BACKGROUND,
                SurfaceColors.SURFACE_0.getColor(this@TextEditorActivity)
            )
            setColor(
                EditorColorScheme.LINE_NUMBER_BACKGROUND,
                SurfaceColors.SURFACE_0.getColor(this@TextEditorActivity)
            )
            setColor(
                EditorColorScheme.COMPLETION_WND_BACKGROUND,
                SurfaceColors.SURFACE_1.getColor(this@TextEditorActivity)
            )
            setColor(EditorColorScheme.HIGHLIGHTED_DELIMITERS_FOREGROUND, Color.RED)
        }
        return scheme
    }

    private fun saveFile(content: String) {
        try {
            editorViewModel.file?.writeText(content)
        } catch (e: IOException) {
            Log.e(TAG, Log.UNABLE_TO + " write to file " + editorViewModel.file, e)
            showMsg(Log.SOMETHING_WENT_WRONG + ", check app debug for more details")
        }
    }

    companion object {
        private const val TAG = "TextEditorActivity"
        private const val LANGUAGE_JAVA = 0
        private const val LANGUAGE_KOTLIN = 1
        private const val LANGUAGE_JSON = 2
        private const val LANGUAGE_XML = 3
    }
}
