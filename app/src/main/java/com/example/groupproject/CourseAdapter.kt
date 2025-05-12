package com.example.groupproject

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(private val courses: List<String>, private val courseColors: Map<String, Int>, private val onDeleteClicked: (String) -> Unit) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseName: TextView = view.findViewById(R.id.courseTitle)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.courseName.text = course
        val color = courseColors[course] ?: Color.BLACK
        holder.courseName.setTextColor(color)
        holder.deleteButton.setOnClickListener {
            onDeleteClicked(course)
        }
        holder.deleteButton.setColorFilter(Color.BLACK)
    }

    override fun getItemCount() = courses.size
}