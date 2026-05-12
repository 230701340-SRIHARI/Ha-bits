package com.example.ha_bits.view

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ha_bits.R
import com.example.ha_bits.model.Habit
import com.example.ha_bits.viewmodel.HabitViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener

class AnalyticsActivity : AppCompatActivity() {
    private lateinit var viewModel: HabitViewModel
    private lateinit var barChart: BarChart
    private lateinit var emptyStateText: TextView
    private lateinit var totalHabitsText: TextView
    private lateinit var bestStreakText: TextView
    private lateinit var completionRateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_analytics)

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

        barChart = findViewById(R.id.barChart)
        emptyStateText = findViewById(R.id.emptyStateText)
        totalHabitsText = findViewById(R.id.totalHabitsText)
        bestStreakText = findViewById(R.id.bestStreakText)
        completionRateText = findViewById(R.id.completionRateText)

        setupChart()

        viewModel = ViewModelProvider(this)[HabitViewModel::class.java]
        viewModel.allHabits.observe(this) { habits ->
            val activeHabits = habits.filter { !it.isArchived }
            updateUI(activeHabits)
        }
    }

    private fun setupChart() {
        barChart.description.isEnabled = false
        barChart.setDrawGridBackground(false)
        barChart.setDrawBarShadow(false)
        barChart.setDrawValueAboveBar(true)
        barChart.setPinchZoom(false)
        barChart.setScaleEnabled(false)
        
        val xAxis = barChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.granularity = 1f
        
        barChart.axisLeft.setDrawGridLines(false)
        barChart.axisRight.isEnabled = false
        barChart.legend.isEnabled = false
    }

    private fun updateUI(habits: List<Habit>) {
        if (habits.isEmpty()) {
            barChart.visibility = View.GONE
            emptyStateText.visibility = View.VISIBLE
            totalHabitsText.text = "Total Habits: 0"
            bestStreakText.text = "Best Streak: 0 days"
            completionRateText.text = "Completion Rate: 0%"
        } else {
            barChart.visibility = View.VISIBLE
            emptyStateText.visibility = View.GONE
            
            totalHabitsText.text = "Total Habits: ${habits.size}"
            val bestStreak = habits.maxOfOrNull { it.streak } ?: 0
            bestStreakText.text = "Best Streak: $bestStreak days"
            
            val completedToday = habits.count { isCompletedToday(it.lastCompleted) }
            val rate = (completedToday.toFloat() / habits.size * 100).toInt()
            completionRateText.text = "Completion Rate: $rate% (Today)"

            val entries = ArrayList<BarEntry>()
            val labels = ArrayList<String>()
            
            habits.take(7).forEachIndexed { index, habit ->
                entries.add(BarEntry(index.toFloat(), habit.streak.toFloat()))
                labels.add(if (habit.name.length > 8) habit.name.substring(0, 5) + "..." else habit.name)
            }

            barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            
            val dataSet = BarDataSet(entries, "Streaks")
            dataSet.color = getColor(R.color.material_dynamic_primary50)
            dataSet.valueTextSize = 10f
            
            barChart.data = BarData(dataSet)
            
            barChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val index = e?.x?.toInt() ?: -1
                    if (index >= 0 && index < habits.size) {
                        Toast.makeText(this@AnalyticsActivity, habits[index].name + ": " + habits[index].streak + " days", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onNothingSelected() {}
            })
            
            barChart.animateY(800)
            barChart.invalidate()
        }
    }

    private fun isCompletedToday(lastCompleted: Long): Boolean {
        val today = java.util.Calendar.getInstance()
        val last = java.util.Calendar.getInstance()
        last.timeInMillis = lastCompleted
        return today.get(java.util.Calendar.YEAR) == last.get(java.util.Calendar.YEAR) &&
               today.get(java.util.Calendar.DAY_OF_YEAR) == last.get(java.util.Calendar.DAY_OF_YEAR)
    }
}