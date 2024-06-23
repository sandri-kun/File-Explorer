package com.raival.fileexplorer.activity.editor.language.xml

import com.raival.fileexplorer.App.Companion.grammarRegistry
import com.raival.fileexplorer.App.Companion.themeRegistry
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage

class XmlLanguage : TextMateLanguage(
    grammarRegistry.findGrammar("source.xml"),
    grammarRegistry.findLanguageConfiguration("source.xml"),
    grammarRegistry,
    themeRegistry,
    true
)
