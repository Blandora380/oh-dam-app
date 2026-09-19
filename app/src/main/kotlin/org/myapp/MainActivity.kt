package org.myapp

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView

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
    private lateinit var textGreeting: TextView
    private lateinit var textShizuku: TextView
    private lateinit var editName: EditText
    private lateinit var switchDarkMode: Switch
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var btnGreet: Button
    private lateinit var btnToast: Button
    private lateinit var btnReset: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)

        rootLayout = findViewById(R.id.rootLayout)
        sectionHome = findViewById(R.id.sectionHome)
        sectionShizuku = findViewById(R.id.sectionShizuku)
        sectionSettings = findViewById(R.id.sectionSettings)
        textGreeting = findViewById(R.id.textGreeting)
        textShizuku = findViewById(R.id.textShizuku)
        editName = findViewById(R.id.editName)
        switchDarkMode = findViewById(R.id.switchDarkMode)
        bottomNav = findViewById(R.id.bottomNav)
        btnGreet = findViewById(R.id.btnGreet)
        btnToast = findViewById(R.id.btnToast)
        btnReset = findViewById(R.id.btnReset)

        applyThemeInstantly(isDarkMode)
        switchDarkMode.isChecked = isDarkMode

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showSection(sectionHome)
                    true
                }
                R.id.nav_shizuku -> {
                    showSection(sectionShizuku)
                    true
                }
                R.id.nav_settings -> {
                    showSection(sectionSettings)
                    true
                }
                else -> false
            }
        }

        btnGreet.setOnClickListener {
            val name = editName.text.toString()
            if (name.isNotBlank()) {
                textGreeting.text = "Hello, $name!"
            } else {
                Toast.makeText(this, "Please submit your name first!", Toast.LENGTH_SHORT).show()
            }
        }

        btnToast.setOnClickListener {
            Toast.makeText(this, "This is a Toast message!", Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            textGreeting.text = "Hello from The!"
            editName.text.clear()
        }

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            animateThemeChange(isChecked)
        }
    }

    private fun showSection(sectionToShow: android.view.View) {
        sectionHome.visibility = if (sectionToShow == sectionHome) android.view.View.VISIBLE else android.view.View.GONE
        sectionShizuku.visibility = if (sectionToShow == sectionShizuku) android.view.View.VISIBLE else android.view.View.GONE
        sectionSettings.visibility = if (sectionToShow == sectionSettings) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun applyThemeInstantly(isDark: Boolean) {
        rootLayout.setBackgroundColor(if (isDark) colorBgDark else colorBgLight)
        bottomNav.setBackgroundColor(if (isDark) colorNavDark else colorNavLight)

        val textColor = if (isDark) colorTextDark else colorTextLight
        val hintColor = if (isDark) colorHintDark else colorHintLight

        textGreeting.setTextColor(textColor)
        textShizuku.setTextColor(textColor)
        editName.setTextColor(textColor)
        editName.setHintTextColor(hintColor)
        switchDarkMode.setTextColor(textColor)
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
        bgAnimator.addUpdateListener { animator ->
            rootLayout.setBackgroundColor(animator.animatedValue as Int)
        }

        val navAnimator = ValueAnimator.ofObject(ArgbEvaluator(), navFrom, navTo)
        navAnimator.duration = 400
        navAnimator.addUpdateListener { animator ->
            bottomNav.setBackgroundColor(animator.animatedValue as Int)
        }

        val textAnimator = ValueAnimator.ofObject(ArgbEvaluator(), textFrom, textTo)
        textAnimator.duration = 400
        textAnimator.addUpdateListener { animator ->
            val color = animator.animatedValue as Int
            textGreeting.setTextColor(color)
            textShizuku.setTextColor(color)
            editName.setTextColor(color)
            switchDarkMode.setTextColor(color)
            btnGreet.setTextColor(color)
            btnToast.setTextColor(color)
            btnReset.setTextColor(color)
            bottomNav.itemTextColor = ColorStateList.valueOf(color)
            bottomNav.itemIconTintList = ColorStateList.valueOf(color)
        }

        val hintAnimator = ValueAnimator.ofObject(ArgbEvaluator(), hintFrom, hintTo)
        hintAnimator.duration = 400
        hintAnimator.addUpdateListener { animator ->
            editName.setHintTextColor(animator.animatedValue as Int)
        }

        bgAnimator.start()
        navAnimator.start()
        textAnimator.start()
        hintAnimator.start()
    }
}