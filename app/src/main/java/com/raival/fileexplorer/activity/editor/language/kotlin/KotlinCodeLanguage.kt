package com.raival.fileexplorer.activity.editor.language.kotlin

import com.raival.fileexplorer.App.Companion.grammarRegistry
import com.raival.fileexplorer.App.Companion.themeRegistry
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage

class KotlinCodeLanguage : TextMateLanguage(
    grammarRegistry.findGrammar("source.kotlin"),
    grammarRegistry.findLanguageConfiguration("source.kotlin"),
    grammarRegistry,
    themeRegistry,
    true
)
