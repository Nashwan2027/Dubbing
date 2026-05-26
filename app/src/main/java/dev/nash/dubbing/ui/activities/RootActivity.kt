package dev.nash.dubbing.ui.activities

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import dev.nash.dubbing.R
import dev.nash.dubbing.data.repository.FileBasedProjectStorageRepository
import dev.nash.dubbing.data.repository.ProjectRepositoryImpl
import dev.nash.dubbing.ui.root.DubbingRootView
import dev.nash.dubbing.ui.screens.AiSettingsView

class RootActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var mainContentContainer: FrameLayout
    private lateinit var repo: ProjectRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        repo = ProjectRepositoryImpl(FileBasedProjectStorageRepository(this))

        drawerLayout = DrawerLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = DrawerLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#0A0A0C"))
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 150)
            setBackgroundColor(Color.parseColor("#121212"))
            setPadding(30, 0, 30, 0)
            gravity = Gravity.CENTER_VERTICAL
            
            val menuIcon = TextView(this@RootActivity).apply {
                text = "☰"
                textSize = 28f
                setTextColor(Color.WHITE)
                setOnClickListener { drawerLayout.openDrawer(Gravity.LEFT) }
            }
            
            val title = TextView(this@RootActivity).apply {
                text = "  استوديو الدبلجة"
                textSize = 20f
                setTextColor(Color.WHITE)
                setTypeface(null, android.graphics.Typeface.BOLD)
            }
            
            addView(menuIcon)
            addView(title)
        }

        mainContentContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        mainLayout.addView(toolbar)
        mainLayout.addView(mainContentContainer)

        val navigationView = NavigationView(this).apply {
            layoutParams = DrawerLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.LEFT
            }
            inflateHeaderView(R.layout.nav_header)
            menu.add(0, 1, 0, "الرئيسية")
            menu.add(0, 2, 0, "إعدادات الذكاء الاصطناعي")
            menu.add(0, 3, 0, "المظهر (ليلي/نهاري)")
            
            setBackgroundColor(Color.parseColor("#121212"))
            itemTextColor = android.content.res.ColorStateList.valueOf(Color.WHITE)
            
            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    1 -> showHome()
                    2 -> showAiSettings()
                    3 -> toggleTheme()
                }
                drawerLayout.closeDrawers()
                true
            }
        }

        drawerLayout.addView(mainLayout)
        drawerLayout.addView(navigationView)
        setContentView(drawerLayout)
        showHome()
    }

    private fun showHome() {
        mainContentContainer.removeAllViews()
        // تم استبدال context بـ this هنا
        val rootView = DubbingRootView(this, repo) { id ->
            startActivity(Intent(this, EditorActivity::class.java).putExtra("projectId", id))
        }
        mainContentContainer.addView(rootView)
    }

    private fun showAiSettings() {
        mainContentContainer.removeAllViews()
        mainContentContainer.addView(AiSettingsView(this))
    }

    private fun toggleTheme() {
        val current = AppCompatDelegate.getDefaultNightMode()
        AppCompatDelegate.setDefaultNightMode(if (current == AppCompatDelegate.MODE_NIGHT_YES) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(Gravity.LEFT)) {
            drawerLayout.closeDrawer(Gravity.LEFT)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        fun clearAppData(context: Context) {
            context.cacheDir.deleteRecursively()
            context.filesDir.listFiles()?.forEach { if (it.name != "projects.json") it.deleteRecursively() }
        }
    }
}
