package ipvc.estg.sensorandmultimediaapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, SensorEventListener {

    private var tts: TextToSpeech? = null
    private val RQ_SPEECH_REC = 102

    var userName: String = ""

    var lightSensor: Sensor? = null
    var accSensor: Sensor? = null
    var rotationSensor: Sensor? = null
    var sensorManager: SensorManager? = null

    lateinit var ivStickman: ImageView
    lateinit var btnSpeech: Button
    lateinit var rlMain: RelativeLayout
    lateinit var ivStickmanSick: ImageView
    lateinit var ivStickmanReverse: ImageView

    var isLightSensorOn = false
    var updateTime: Long? = null
    var isSickOn = false
    var shakesNum: Int = 0
    var isPhoneReverse = false

    //TODO: Virar o telemovel ao contrario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this, this)

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        lightSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_LIGHT)
        accSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotationSensor = sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        ivStickman = findViewById(R.id.ivStickman)
        btnSpeech = findViewById(R.id.btnSpeech)
        rlMain = findViewById(R.id.rlMain)
        ivStickmanSick = findViewById(R.id.ivStickmanSick)
        ivStickmanReverse = findViewById(R.id.ivStickmanReverse)

        ivStickman.setOnClickListener {view ->
            if (isLightSensorOn){
                speakOut("It's a beautiful night...", TextToSpeech.QUEUE_ADD)
            }else {
                speakOut("Welcome!", TextToSpeech.QUEUE_ADD)
                speakOut("What's your name?", TextToSpeech.QUEUE_ADD)
                btnSpeech.isClickable = true
                btnSpeech.visibility = View.VISIBLE
            } //if sick, put it back
        }

        ivStickmanSick.setOnClickListener {
            ivStickmanSick.visibility = View.INVISIBLE
            ivStickman.visibility = View.VISIBLE
            speakOut("Thank you good sir", TextToSpeech.QUEUE_FLUSH)
            isSickOn = false
            shakesNum = 0
        }

        ivStickmanReverse.setOnClickListener {
            speakOut("Please put the phone right $userName, I'm getting tired", TextToSpeech.QUEUE_FLUSH)
        }

        btnSpeech.setOnClickListener {
            askSpeechInput()
        }

        updateTime = System.currentTimeMillis()
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RQ_SPEECH_REC && resultCode == Activity.RESULT_OK) {
            val result = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            speakOut("Hello $result", TextToSpeech.QUEUE_ADD)
            userName = result.toString()
            btnSpeech.isClickable = false
            btnSpeech.visibility = View.INVISIBLE
        }
    }

    private fun askSpeechInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show()
        } else {
            val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak with me")
            startActivityForResult(i, RQ_SPEECH_REC)
        }
    }

    private fun speakOut(text: String, type: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts!!.speak(text, type, null, "")
        } else {
            Toast.makeText(this, "Your android version does not support this", Toast.LENGTH_SHORT).show()
        }
        // With Flush, if I click 2 times, the first will stop
        // If I want the 2 click to wait for the 1 to finish I can USE QUEUE_ADD
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts!!.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported")
            }
        } else {
            Log.e("TTS", "Initialization Failed!")
        }
    }

    override fun onDestroy() {
        if (tts != null) {
            tts!!.stop()
            tts!!.shutdown()
        }

        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager!!.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Not implemented
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            accelerometerCalculations(event)
        }

        if (event!!.sensor.type == Sensor.TYPE_LIGHT) {
            try {
                if (event!!.values[0] < 12) { //15 is good
                    ivStickman.setImageResource(R.drawable.stickman_sleep)
                    rlMain.setBackgroundResource(R.drawable.night)
                    isLightSensorOn = true
                } else {
                    ivStickman.setImageResource(R.drawable.stickman_hello)
                    rlMain.setBackgroundColor(Color.parseColor("#ffffff"))
                    isLightSensorOn = false
                }
            } catch (e: IOException) {
            }
        }

        if (event!!.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            handleRotationVector(event)
        }
    }

    private fun handleRotationVector(event: SensorEvent) {
        val rotationMatrix = FloatArray(16)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event?.values)

        val remappedRotationMatrix = FloatArray(16)
        SensorManager.remapCoordinateSystem(
                rotationMatrix,
                SensorManager.AXIS_X,
                SensorManager.AXIS_Z,
                remappedRotationMatrix
        )

        var orientations: FloatArray = FloatArray(3)
        SensorManager.getOrientation(remappedRotationMatrix, orientations)

        for (i in 0..2) {
            orientations[i] = (Math.toDegrees(orientations[i].toDouble()).toFloat())
        }

        if (orientations[2] > 130) {
            ivStickman.visibility = View.INVISIBLE
            ivStickmanReverse.visibility = View.VISIBLE
            isPhoneReverse = true
        } else if (orientations[2] <= 30) {
            ivStickmanReverse.visibility = View.INVISIBLE
            ivStickman.visibility = View.VISIBLE
        }
    }

    private fun accelerometerCalculations(event: SensorEvent) {
        val values: FloatArray? = event.values
        val x: Float = values!![0]
        val y: Float = values[1]
        val z: Float = values[2]

        val timeNow: Long = System.currentTimeMillis()

        val acceleration =
            (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH)

        if (acceleration >= 10) {
            if (timeNow - updateTime!! < 1000) { return }

            updateTime = timeNow

            if (!isSickOn) {
                ivStickman.visibility = View.INVISIBLE
                ivStickmanSick.visibility = View.VISIBLE
                isSickOn = true
                shakesNum++
            }

            if (shakesNum in 1..3) {
                speakOut("Please stop I can't handle this", TextToSpeech.QUEUE_ADD)
                shakesNum++
            }
            if (shakesNum >= 4) speakOut("Help me please, I'm very sick", TextToSpeech.QUEUE_ADD)

        }
    }
}