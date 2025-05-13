package com.example.groupproject

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity implementing a Pomodoro timer with optional duration customization,
 * swipe-to-exit gesture, and break logic based on session count.
 */
class PomodoroActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private lateinit var timerText: TextView
    private lateinit var customizeCheckBox: CheckBox
    private lateinit var seekBar: SeekBar
    private lateinit var durationText: TextView
    private lateinit var startButton: Button
    private var durationSelected = 25 //25 minutes if not customized
    private var isWorkSession = true
    private var workSessionCount = 0
    private var timer: CountDownTimer? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var resetButton: Button
    private var isTimerRunning = false
    private var remainingTimeInMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomodoro_timer)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "All Tasks"

        status = findViewById(R.id.status)
        timerText = findViewById(R.id.timer_text)
        customizeCheckBox = findViewById(R.id.customize_timer)
        seekBar = findViewById(R.id.seek_bar)
        durationText = findViewById(R.id.duration_text)
        startButton = findViewById(R.id.start_Button)
        resetButton = findViewById(R.id.reset_Button)


//        listener for customizing duration
        customizeCheckBox.setOnCheckedChangeListener(CheckListener())
//        listener for seekBar
        seekBar.setOnSeekBarChangeListener(SeekListener())
//        exits this activity when swiping left to right
        gestureDetector = GestureDetector(this, SwipeGestureListener())

//        Button Listeners
        startButton.setOnClickListener {
            Log.d("PomodoroDebug", "START clicked. isTimerRunning=$isTimerRunning, remainingTimeInMillis=$remainingTimeInMillis")

            if (isTimerRunning) {
                // Pause
                timer?.cancel()
                isTimerRunning = false
                startButton.text = getString(R.string.resume)
                Log.d("PomodoroDebug", "TIMER PAUSED")
            } else {
                //start from beginning
                if (remainingTimeInMillis <= 0L) {
                    if (customizeCheckBox.isChecked) {
                        durationSelected = seekBar.progress.coerceAtLeast(1)
                    }
                    remainingTimeInMillis = durationSelected * 60 * 1000L
                    Log.d("PomodoroDebug", "TIMER STARTING FRESH via startPomodoro()")
                    startPomodoro()

                }else{
                    Log.d("PomodoroDebug", "TIMER STARTING FROM REMAINING TIME: $remainingTimeInMillis")
                    timer = PomodoroTimer(remainingTimeInMillis).start()
                    isTimerRunning = true
                }
                startButton.text = getString(R.string.pause)
            }
        }
        // Reset button resets timer
        resetButton.setOnClickListener {
            timer?.cancel()
            startButton.text = getString(R.string.start_pomodoro)
            isTimerRunning = false
            if (customizeCheckBox.isChecked) {
                durationSelected = seekBar.progress.coerceAtLeast(1)
            }else {
                durationSelected = 25
            }
            remainingTimeInMillis = 0L
            timerText.text = String.format("%02d:00", durationSelected)
            Log.d("PomodoroDebug", "RESET: durationSelected=$durationSelected, remainingTimeInMillis=$remainingTimeInMillis")
        }
    }

    /**
     * Starts a new Pomodoro session.
     * Applies long break every 4 sessions.
     */
    private fun startPomodoro(){
        var durationInMinutes = 0
        if (customizeCheckBox.isChecked) {
            durationSelected = seekBar.progress.coerceAtLeast(1)
        }

        if(isWorkSession){
            durationInMinutes = durationSelected
            status.text = getString(R.string.work_session)
        }else if (workSessionCount % 4 == 0){ //15 minutes break after 4 study sessions
            durationInMinutes = 15
            status.text = getString(R.string.long_break)
        }else{ //5 minute break after each study session
            durationInMinutes = (durationSelected * 0.2).toInt().coerceAtLeast(1)
            status.text = getString(R.string.short_break)
        }
        remainingTimeInMillis = durationInMinutes * 60 * 1000L
        timer = PomodoroTimer(remainingTimeInMillis).start()
        isTimerRunning = true
        Log.d("PomodoroDebug", "START POMODORO: durationInMinutes=$durationInMinutes, remainingTimeInMillis=$remainingTimeInMillis")

    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    /**
     * Inner class managing the countdown logic.
     * Updates UI each second and handles session switching.
     */
    inner class PomodoroTimer(private val durationInMillis: Long) :
        CountDownTimer(durationInMillis, 1000){

        override fun onTick(millisUntilFinished: Long) {
            remainingTimeInMillis = millisUntilFinished
            val minutes = (millisUntilFinished / 1000) / 60
            val seconds = (millisUntilFinished / 1000) % 60
            timerText.text = String.format("%02d:%02d", minutes, seconds)
        }

        override fun onFinish() {
            isTimerRunning = false
            if (isWorkSession) {
                workSessionCount++
            }
            isWorkSession = !isWorkSession
            startPomodoro()
        }
        }

    /**
     * Checkbox listener for showing/hiding seek bar for custom time.
     */
    inner class CheckListener : CompoundButton.OnCheckedChangeListener{
        override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
           if(isChecked){
               seekBar.visibility = View.VISIBLE
               durationText.visibility = View.VISIBLE
           }else{
               seekBar.visibility = View.GONE
               durationText.visibility = View.GONE
           }
        }
    }

    /**
     * SeekBar listener for setting custom Pomodoro duration.
     */
    inner class SeekListener : SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            durationSelected = if (progress < 1) { 1 } else{ progress }
            durationText.text = "$durationSelected minutes"
            timerText.text = String.format("%02d:00", durationSelected)
        }
        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
        override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    /**
     * Handles swipe gesture: right to left finishes the activity.
     */
    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val swipeThreshold = 100
        private val swipeVelocityThreshold = 100

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            if (kotlin.math.abs(diffX) > kotlin.math.abs(diffY) &&
                kotlin.math.abs(diffX) > swipeThreshold &&
                kotlin.math.abs(velocityX) > swipeVelocityThreshold
            ) {
                if (diffX < 0) {
                    finish() //ends activity when user swipes right to left
                }
                return true
            }
            return false
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}