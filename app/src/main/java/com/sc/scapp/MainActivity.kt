package com.sc.scapp

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.sc.settings.SettingsActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mDrawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.mipmap.ic_menu)
        }

        mDrawerLayout = findViewById(R.id.drawer_layout)

        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setCheckedItem(R.id.about)
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.setting -> SettingsActivity.actionStart(this)
                R.id.about -> Toast.makeText(this@MainActivity, "About!!!", Toast.LENGTH_SHORT).show()
            }
            //drawerLayout.closeDrawers();
            true
        }
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "FAB clicked!", Snackbar.LENGTH_SHORT)
                    .setAction("Action") { Toast.makeText(this@MainActivity, "Action clicked!", Toast.LENGTH_SHORT).show() }
                    .show()
        }
        val programList = listOf(Program(R.string.files, R.drawable.ic_files), Program(R.string.notebook,
                R.drawable.ic_notebook), Program(R.string.media, R.drawable.ic_media),
                Program(R.string.web, R.drawable.ic_web), Program(R.string.download, R.drawable.ic_download),
                Program(R.string.timer, R.drawable.ic_timer), Program(R.string.screen_recorder, R.drawable.ic_screen_recorder))
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        val layoutManager = GridLayoutManager(this, 2)
        recyclerView.layoutManager = layoutManager
        val adapter = ProgramAdapter(programList)
        recyclerView.adapter = adapter
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> mDrawerLayout.openDrawer(GravityCompat.START)
            R.id.setting ->
                //Intent intent = new Intent(Intent.ACTION_VIEW);
                //intent.setData(Uri.parse("https://www.baidu.com/"));
                //startActivity(intent);
                SettingsActivity.actionStart(this)
        }
        return true
    }
}