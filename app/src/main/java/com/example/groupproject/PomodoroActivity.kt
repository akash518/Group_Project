package com.example.groupproject

import android.os.Bundle
import android.os.CountDownTimer
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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
            if (isTimerRunning) {
                // Pause
                timer?.cancel()
                isTimerRunning = false
                startButton.text = "Resume"
            } else {
                //start from beginning
                if (remainingTimeInMillis <= 0L) {
                    startPomodoro()

                }else{
                    timer = PomodoroTimer(remainingTimeInMillis).start()
                    isTimerRunning = true
                }
                startButton.text = "Pause"
            }
        }
        resetButton.setOnClickListener {
            timer?.cancel()
            startButton.text = "Start Pomodoro"
            isTimerRunning = false
            remainingTimeInMillis = durationSelected * 60 * 1000L
            timerText.text = String.format("%02d:00", durationSelected)
        }

    }


//    changes duration according to status
    private fun startPomodoro(){
        var durationInMinutes = 0
        if(isWorkSession){
            durationInMinutes = durationSelected
            status.text = "Work Session"
        }else if (workSessionCount % 4 == 0){ //15 minutes break after 4 study sessions
            durationInMinutes = 15
            status.text = "Long Break"
        }else{ //5 minute break after each study session
            durationInMinutes = (durationSelected * 0.2).toInt().coerceAtLeast(1)
            status.text = "Short Break"
        }
        remainingTimeInMillis = durationInMinutes * 60 * 1000L
        timer = PomodoroTimer(remainingTimeInMillis).start()
        isTimerRunning = true
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    //updating timer
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

    //
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

    //handels when user customizes timer
    inner class SeekListener : SeekBar.OnSeekBarChangeListener{
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            if (progress < 1) {
                durationSelected = 1
            }else{
                durationSelected = progress
            }
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

    inner class SwipeGestureListener : GestureDetector.SimpleOnGestureListener() {
        private val SWIPE_THRESHOLD = 100
        private val SWIPE_VELOCITY_THRESHOLD = 100

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
                kotlin.math.abs(diffX) > SWIPE_THRESHOLD &&
                kotlin.math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD
            ) {
                if (diffX > 0) {
                    finish() //ends activity when user swipes left to right
                }
                return true
            }
            return false
        }
    }
}