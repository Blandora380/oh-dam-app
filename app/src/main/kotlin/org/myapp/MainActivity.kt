package org.myapp

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val colorBgLight = Color.parseColor("#FFFFFF")
    private val colorBgDark = Color.parseColor("#121212")
    private val colorTextLight = Color.parseColor("#000000")
    private val colorTextDark = Color.parseColor("#FFFFFF")
    private val colorNavLight = Color.parseColor("#F5F5F5")
    private val colorNavDark = Color.parseColor("#1E1E1E")
    private val colorHintLight = Color.parseColor("#757575")
    private val colorHintDark = Color.parseColor("#BDBDBD")

    private lateinit var rootLayout: android.widget.LinearLayout
    private lateinit var sectionHome: android.view.View
    private lateinit var sectionShizuku: android.view.View
    private lateinit var sectionSettings: android.view.View
    private lateinit var sectionAbout: android.view.View

    private lateinit var textGreeting: TextView
    private lateinit var textShizuku: TextView
    private lateinit var textLanguageLabel: TextView
    private lateinit var textAboutTitle: TextView
    private lateinit var textAboutVersion: TextView

    private lateinit var editName: EditText
    private lateinit var switchDarkMode: Switch
    private lateinit var radioLanguage: RadioGroup
    private lateinit var radioIndonesian: RadioButton
    private lateinit var radioEnglish: RadioButton

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnGreet: Button
    private lateinit var btnToast: Button
    private lateinit var btnReset: Button

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val lang = prefs.getString("app_language", "en") ?: "en"
        super.attachBaseContext(updateLocale(newBase, lang))
    }

    private fun updateLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(0, 0)
        super.onCreate(savedInstanceState)

        val prefsEarly = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isDarkModeEarly = prefsEarly.getBoolean("dark_mode", true)
        window.setBackgroundDrawable(
            ColorDrawable(if (isDarkModeEarly) colorBgDark else colorBgLight)
        )

        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", true)

        rootLayout = findViewById(R.id.rootLayout)
        sectionHome = findViewById(R.id.sectionHome)
        sectionShizuku = findViewById(R.id.sectionShizuku)
        sectionSettings = findViewById(R.id.sectionSettings)
        sectionAbout = findViewById(R.id.sectionAbout)

        textGreeting = findViewById(R.id.textGreeting)
        textShizuku = findViewById(R.id.textShizuku)
        textLanguageLabel = findViewById(R.id.textLanguageLabel)
        textAboutTitle = findViewById(R.id.textAboutTitle)
        textAboutVersion = findViewById(R.id.textAboutVersion)

        editName = findViewById(R.id.editName)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        radioLanguage = findViewById(R.id.radioLanguage)
        radioIndonesian = findViewById(R.id.radioIndonesian)
        radioEnglish = findViewById(R.id.radioEnglish)

        bottomNav = findViewById(R.id.bottomNav)
        btnGreet = findViewById(R.id.btnGreet)
        btnToast = findViewById(R.id.btnToast)
        btnReset = findViewById(R.id.btnReset)

        val versionName = packageManager.getPackageInfo(packageName, 0).versionName
        textAboutVersion.text = getString(R.string.about_version, versionName)

        val savedLang = prefs.getString("app_language", "en") ?: "en"
        radioEnglish.isChecked = savedLang == "en"
        radioIndonesian.isChecked = savedLang == "id"

        applyThemeInstantly(isDarkMode)
        switchDarkMode.isChecked = isDarkMode

        val lastSectionId = savedInstanceState?.getInt("last_section_id") ?: R.id.nav_home
        bottomNav.selectedItemId = lastSectionId
        showSectionById(lastSectionId)

        bottomNav.setOnItemSelectedListener { item ->
            showSectionById(item.itemId)
            true
        }

        btnGreet.setOnClickListener {
            val name = editName.text.toString()
            if (name.isNotBlank()) {
                textGreeting.text = getString(R.string.hello_format, name)
            } else {
                Toast.makeText(this, getString(R.string.toast_no_name), Toast.LENGTH_SHORT).show()
            }
        }

        btnToast.setOnClickListener {
            Toast.makeText(this, getString(R.string.toast_message), Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            textGreeting.text = getString(R.string.greeting_default)
            editName.text.clear()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            animateThemeChange(isChecked)
        }

        radioLanguage.setOnCheckedChangeListener { _, checkedId ->
            val newLang = if (checkedId == R.id.radioEnglish) "en" else "id"
            val currentLang = prefs.getString("app_language", "en") ?: "en"
            if (currentLang != newLang) {
                prefs.edit().putString("app_language", newLang).apply()
                recreate()
                overridePendingTransition(0, 0)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("last_section_id", bottomNav.selectedItemId)
    }

    private fun showSectionById(itemId: Int) {
        sectionHome.visibility = if (itemId == R.id.nav_home) android.view.View.VISIBLE else android.view.View.GONE
        sectionShizuku.visibility = if (itemId == R.id.nav_shizuku) android.view.View.VISIBLE else android.view.View.GONE
        sectionSettings.visibility = if (itemId == R.id.nav_settings) android.view.View.VISIBLE else android.view.View.GONE
        sectionAbout.visibility = if (itemId == R.id.nav_about) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun applyThemeInstantly(isDark: Boolean) {
        rootLayout.setBackgroundColor(if (isDark) colorBgDark else colorBgLight)
        bottomNav.setBackgroundColor(if (isDark) colorNavDark else colorNavLight)

        val textColor = if (isDark) colorTextDark else colorTextLight
        val hintColor = if (isDark) colorHintDark else colorHintLight

        textGreeting.setTextColor(textColor)
        textShizuku.setTextColor(textColor)
        textLanguageLabel.setTextColor(textColor)
        textAboutTitle.setTextColor(textColor)
        textAboutVersion.setTextColor(textColor)
        editName.setTextColor(textColor)
        editName.setHintTextColor(hintColor)
        switchDarkMode.setTextColor(textColor)
        radioIndonesian.setTextColor(textColor)
        radioEnglish.setTextColor(textColor)
        btnGreet.setTextColor(textColor)
        btnToast.setTextColor(textColor)
        btnReset.setTextColor(textColor)

        bottomNav.itemTextColor = ColorStateList.valueOf(textColor)
        bottomNav.itemIconTintList = ColorStateList.valueOf(textColor)
    }

    private fun animateThemeChange(toDark: Boolean) {
        val bgFrom = if (toDark) colorBgLight else colorBgDark
        val bgTo = if (toDark) colorBgDark else colorBgLight
        val navFrom = if (toDark) colorNavLight else colorNavDark
        val navTo = if (toDark) colorNavDark else colorNavLight
        val textFrom = if (toDark) colorTextLight else colorTextDark
        val textTo = if (toDark) colorTextDark else colorTextLight
        val hintFrom = if (toDark) colorHintLight else colorHintDark
        val hintTo = if (toDark) colorHintDark else colorHintLight

        val bgAnimator = ValueAnimator.ofObject(ArgbEvaluator(), bgFrom, bgTo)
        bgAnimator.duration = 400
        bgAnimator.addUpdateListener { rootLayout.setBackgroundColor(it.animatedValue as Int) }

        val navAnimator = ValueAnimator.ofObject(ArgbEvaluator(), navFrom, navTo)
        navAnimator.duration = 400
        navAnimator.addUpdateListener { bottomNav.setBackgroundColor(it.animatedValue as Int) }

        val textAnimator = ValueAnimator.ofObject(ArgbEvaluator(), textFrom, textTo)
        textAnimator.duration = 400
        textAnimator.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            textGreeting.setTextColor(color)
            textShizuku.setTextColor(color)
            textLanguageLabel.setTextColor(color)
            textAboutTitle.setTextColor(color)
            textAboutVersion.setTextColor(color)
            editName.setTextColor(color)
            switchDarkMode.setTextColor(color)
            radioIndonesian.setTextColor(color)
            radioEnglish.setTextColor(color)
            btnGreet.setTextColor(color)
            btnToast.setTextColor(color)
            btnReset.setTextColor(color)
            bottomNav.itemTextColor = ColorStateList.valueOf(color)
            bottomNav.itemIconTintList = ColorStateList.valueOf(color)
        }

        val hintAnimator = ValueAnimator.ofObject(ArgbEvaluator(), hintFrom, hintTo)
        hintAnimator.duration = 400
        hintAnimator.addUpdateListener { editName.setHintTextColor(it.animatedValue as Int) }

        bgAnimator.start(); navAnimator.start(); textAnimator.start(); hintAnimator.start()
    }
}