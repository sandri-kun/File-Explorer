package com.raival.fileexplorer

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Process
import android.widget.Toast
import com.pixplicity.easyprefs.library.Prefs
import com.raival.fileexplorer.util.Log
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import java.io.FileNotFoundException
import kotlin.system.exitProcess

class App : Application() {
    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler { _: Thread?, throwable: Throwable? ->
            Log.e("AppCrash", "", throwable)
            Process.killProcess(Process.myPid())
            exitProcess(2)
        }

        super.onCreate()
        appContext = this

        Prefs.Builder()
            .setContext(applicationContext)
            .setPrefsName("Prefs")
            .setMode(MODE_PRIVATE)
            .build()

        loadTextmateTheme()
        Log.start(appContext)
    }

    private fun loadTextmateTheme() {
        FileProviderRegistry.getInstance().addFileProvider(AssetsFileResolver(assets))

        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

        val themeRegistry = ThemeRegistry.getInstance()
        themeRegistry.loadTheme(loadTheme("dark.json", "darcula"))
        themeRegistry.loadTheme(loadTheme("QuietLight.json", "QuietLight"))
    }

    private fun loadTheme(fileName: String, themeName: String): ThemeModel {
        val inputStream =
            FileProviderRegistry.getInstance().tryGetInputStream("textmate/$fileName")
                ?: throw FileNotFoundException("Theme file not found: $fileName")
        val source = IThemeSource.fromInputStream(inputStream, fileName, null)
        return ThemeModel(source, themeName)
    }

    companion object {
        lateinit var appContext: Context

        @JvmStatic
        fun showMsg(message: String?) {
            Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show()
        }

        @JvmStatic
        fun copyString(string: String?) {
            (appContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(
                ClipData.newPlainText("clipboard", string)
            )
        }

        val grammarRegistry = GrammarRegistry.getInstance()
        val themeRegistry = ThemeRegistry.getInstance()
    }
}
