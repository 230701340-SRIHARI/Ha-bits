package com.example.ha_bits.view

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ha_bits.R
import com.example.ha_bits.viewmodel.HabitViewModel
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: HabitViewModel
    private lateinit var adapter: HabitAdapter
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val root = findViewById<android.view.ViewGroup>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        
        viewModel = ViewModelProvider(this)[HabitViewModel::class.java]

        adapter = HabitAdapter(
            onCompleteClick = { habit ->
                viewModel.completeHabit(habit)
            },
            onArchiveClick = { habit ->
                viewModel.archiveHabit(habit)
            },
            onDeleteClick = { habit ->
                showDeleteConfirmationDialog(habit)
            },
            onStartFreshClick = { habit ->
                viewModel.startFresh(habit)
            }
        )
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.allHabits.observe(this) { habits ->
            adapter.setData(habits.filter { !it.isArchived })
        }

        findViewById<com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton>(R.id.fab).setOnClickListener {
            showAddHabitDialog()
        }
        
        viewModel.refreshStreaks()
    }

    private fun showDeleteConfirmationDialog(habit: com.example.ha_bits.model.Habit) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Ha-bit")
            .setMessage("Are you sure you want to delete \"${habit.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteHabit(habit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddHabitDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_habit, null)
        val timeBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.timePickerBtn)
        
        var selectedHour = 12
        var selectedMinute = 0

        timeBtn.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText("Select Reminder Time")
                .build()

            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute
                timeBtn.text = String.format("Set Time: %02d:%02d", selectedHour, selectedMinute)
            }
            picker.show(supportFragmentManager, "TIME_PICKER")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("New Ha-bit")
            .setView(view)
            .setPositiveButton("Save") { _, _ ->
                val name = view.findViewById<TextInputEditText>(R.id.editName).text.toString()
                if (name.isNotBlank()) {
                    viewModel.addCustomHabit(name, selectedHour, selectedMinute, false)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_analytics -> {
                startActivity(Intent(this, AnalyticsActivity::class.java))
                return true
            }
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                return true
            }
            R.id.action_logout -> {
                viewModel.clearLocalData()
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}