package com.example.ha_bits.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ha_bits.R
import com.example.ha_bits.model.Habit
import com.example.ha_bits.viewmodel.HabitViewModel

class HistoryActivity : AppCompatActivity() {
    private lateinit var viewModel: HabitViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_history)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        val root = findViewById<android.view.ViewGroup>(R.id.main_container)
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        recyclerView = findViewById(R.id.historyRecyclerView)
        emptyText = findViewById(R.id.emptyHistoryText)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel = ViewModelProvider(this)[HabitViewModel::class.java]
        viewModel.allHabits.observe(this) { habits ->
            val historyHabits = habits.filter { it.isArchived || it.isBroken }
            if (historyHabits.isEmpty()) {
                emptyText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyText.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.adapter = HistoryAdapter(historyHabits)
            }
        }
    }

    class HistoryAdapter(private val habits: List<Habit>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val nameText: TextView = view.findViewById(android.R.id.text1)
            val detailText: TextView = view.findViewById(android.R.id.text2)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_2, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val habit = habits[position]
            holder.nameText.text = habit.name
            val status = if (habit.isArchived) "Completed (One-time)" else "Broken Streak"
            holder.detailText.text = "$status - Followed for ${habit.streak} days"
        }

        override fun getItemCount() = habits.size
    }
}