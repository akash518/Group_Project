package com.example.groupproject

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

/**
 * Data class representing a task associated with a course.
 * @param courseId The name/ID of the course this task belongs to
 * @param taskName The name of the task
 * @param isCompleted Whether the task is marked complete
 * @param dueDateFormatted A human-readable due date string
 * @param dueDate The actual Date object for internal logic/sorting
 */
data class Task(
    val courseId: String,
    val taskName: String,
    val isCompleted: Boolean,
    val dueDateFormatted: String,
    val dueDate: Date?
)

/**
 * RecyclerView adapter that displays a list of tasks with associated course info,
 * due dates, colors, and a completion button.
 */
class TaskAdapter(
    val tasks: MutableList<Task>,
    private val onTaskCompleted: (Task) -> Unit,
    private val courseColors: Map<String, Int>,
    private val onTaskLongPressed: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {
    /**
     * ViewHolder class to hold references to task layout views.
     */
    class TaskViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val courseName: TextView = view.findViewById(R.id.courseName)
        val taskName: TextView = view.findViewById(R.id.taskName)
        val dueDate: TextView = view.findViewById(R.id.dueDate)
        val complete: ImageButton = view.findViewById(R.id.complete)
        val card: CardView = view as CardView
    }

    /**
     * Inflates a task item layout and creates a ViewHolder for it.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.task, parent, false)
        return TaskViewHolder(view)
    }

    /**
     * Binds data from a Task object into the corresponding ViewHolder.
     */
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.courseName.text = task.courseId
        holder.taskName.text = task.taskName
        holder.dueDate.text = task.dueDateFormatted
        val color = courseColors[task.courseId] ?: Color.LTGRAY
        holder.itemView.findViewById<View>(R.id.colorBlock).setBackgroundColor(color)
        holder.complete.setOnClickListener {
            onTaskCompleted(task)
        }

        if (task.isCompleted) {
            holder.complete.setColorFilter(Color.LTGRAY)
            holder.complete.isEnabled = false
            holder.complete.setOnClickListener(null)
        } else {
            holder.complete.clearColorFilter()
            holder.complete.isEnabled = true
            holder.complete.setOnClickListener {
                onTaskCompleted(task)
            }
        }

        holder.itemView.setOnLongClickListener {
            onTaskLongPressed(task)
            true
        }
    }

    override fun getItemCount(): Int = tasks.size
}