package com.example.ha_bits.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ha_bits.R
import com.example.ha_bits.model.Habit
import com.google.android.material.button.MaterialButton

class HabitAdapter(
    private val onCompleteClick: (Habit) -> Unit,
    private val onDeleteClick: (Habit) -> Unit,
    private val onStartFreshClick: (Habit) -> Unit
) : RecyclerView.Adapter<HabitAdapter.HabitViewHolder>() {

    private var habitList = emptyList<Habit>()

    class HabitViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.habitName)
        val streak: TextView = itemView.findViewById(R.id.habitStreak)
        val brokenStatus: TextView = itemView.findViewById(R.id.brokenStatus)
        val btnComplete: MaterialButton = itemView.findViewById(R.id.btnComplete)
        val btnStartFresh: MaterialButton = itemView.findViewById(R.id.btnStartFresh)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val current = habitList[position]
        holder.name.text = current.name
        holder.streak.text = "Streak: ${current.streak} days"

        if (current.isBroken) {
            holder.brokenStatus.visibility = View.VISIBLE
            holder.btnComplete.visibility = View.GONE
            holder.btnStartFresh.visibility = View.VISIBLE
            holder.streak.setTextColor(holder.itemView.context.getColor(com.google.android.material.R.color.design_default_color_error))
        } else {
            holder.brokenStatus.visibility = View.GONE
            holder.btnComplete.visibility = View.VISIBLE
            holder.btnStartFresh.visibility = View.GONE
            holder.streak.setTextColor(holder.itemView.context.getColor(com.google.android.material.R.color.design_default_color_primary))
        }

        holder.btnComplete.setOnClickListener { onCompleteClick(current) }
        holder.btnStartFresh.setOnClickListener { onStartFreshClick(current) }
        
        holder.itemView.setOnLongClickListener {
            onDeleteClick(current)
            true
        }
    }

    override fun getItemCount() = habitList.size

    fun setData(habits: List<Habit>) {
        this.habitList = habits
        notifyDataSetChanged()
    }
}