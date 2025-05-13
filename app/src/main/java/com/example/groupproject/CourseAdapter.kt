package com.example.groupproject

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Adapter class for displaying a list of course names in a RecyclerView.
 * Each course item shows its name and a delete button.
 * @param courses - List of course titles
 * @param courseColors - Map associating each course with a display color
 * @param onDeleteClicked - Callback when delete button is clicked
 */
class CourseAdapter(private val courses: List<String>, private val courseColors: Map<String, Int>, private val onDeleteClicked: (String) -> Unit) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    /**
     * ViewHolder class that holds references to views in each course item.
     */
    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val courseName: TextView = view.findViewById(R.id.courseTitle)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    /**
     * Called when RecyclerView needs a new ViewHolder.
     * Inflates the layout or creates a view object for a single course item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.course, parent, false)
        return CourseViewHolder(view)
    }

    /**
     * Binds data to the ViewHolder at the given position.
     * Sets the course title text, color, and click listener for deletion.
     */
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

    /**
     * Returns the total number of items to display.
     */
    override fun getItemCount() = courses.size
}