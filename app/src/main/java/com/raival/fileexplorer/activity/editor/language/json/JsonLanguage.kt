package com.raival.fileexplorer.activity.editor.language.json

import com.raival.fileexplorer.App.Companion.grammarRegistry
import com.raival.fileexplorer.App.Companion.themeRegistry
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage

class JsonLanguage : TextMateLanguage(
    grammarRegistry.findGrammar("source.json"),
    grammarRegistry.findLanguageConfiguration("source.json"),
    grammarRegistry,
    themeRegistry,
    true
)
