package com.raival.fileexplorer.activity.editor.language.java

import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.langs.java.JavaLanguage
import io.github.rosemoe.sora.langs.java.JavaTextTokenizer
import io.github.rosemoe.sora.langs.java.Tokens
import io.github.rosemoe.sora.text.ContentReference

open class JavaCodeLanguage : JavaLanguage() {

    private val javaFormatter = JavaFormatter()

    override fun getFormatter(): Formatter {
        return javaFormatter
    }

    override fun getIndentAdvance(text: ContentReference, line: Int, column: Int): Int {
        val content = text.getLine(line).substring(0, column)
        return getIndentAdvance(content)
    }

    override fun useTab(): Boolean {
        return false
    }


    private fun getIndentAdvance(content: String): Int {
        val t = JavaTextTokenizer(content)
        var token: Tokens
        var advance = 0

        while (t.nextToken().also { token = it } !== Tokens.EOF) {
            if (token === Tokens.LBRACE) {
                advance++
            }
            if (token === Tokens.LPAREN) {
                advance++
            }

            if (advance > 0) {
                if (token === Tokens.RBRACE) {
                    advance--
                }
                if (token === Tokens.RPAREN) {
                    advance--
                }
            }
        }

        advance = 0.coerceAtLeast(advance)

        if (advance > 0) return 4
        return 0
    }
}
