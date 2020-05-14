package com.example.myapplication1

//import com.

import android.content.Intent
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import be.tarsos.dsp.AudioEvent

import be.tarsos.dsp.GainProcessor

import be.tarsos.dsp.io.android.AudioDispatcherFactory
import be.tarsos.dsp.pitch.PitchDetectionHandler
import be.tarsos.dsp.pitch.PitchDetectionResult
import be.tarsos.dsp.pitch.PitchProcessor
import com.google.android.material.textfield.TextInputEditText

import java.util.LinkedList


import com.yashovardhan99.timeit.Stopwatch
import com.yashovardhan99.timeit.Split


class MainActivity : AppCompatActivity() {
    var pitch_detection_isRunning = true
    var last_low_sys_time:Long = 0//低电平的开头
    var last_high_sys_time:Long = 0//高电平的开头
    var last_state:Int = 0 //0 -> . 1 -> -
    var temp_decode_morse:String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //使用activity_main.xml文件作为程序主界面


        val REQUEST_EXTERNAL_STORAGE = 1
        val PERMISSIONS_STORAGE = arrayOf<String>(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        if (PackageManager.PERMISSION_GRANTED !=
            ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_CONTACTS)
        ) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE)
        }

        val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050,1024,0)
        val pdh = PitchDetectionHandler{ res, e ->
            val pitchInHz = res.pitch
            runOnUiThread{processPitch(pitchInHz, pitch_detection_isRunning)}
        }
//
        val pitchProcessor = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_YIN,22050f,1024,pdh)
        //val pitchProcessor1 = PitchProcessor(PitchProcessor.PitchEstimationAlgorithm.FFT_PITCH,22050f,1024,pdh)
        //val gainProcessor = GainProcessor(0.0)
        dispatcher.addAudioProcessor(pitchProcessor)
        val audioThread = Thread(dispatcher, "Audio Thread")
        audioThread.start()

        //val stopwatch = Stopwatch()
        //stopwatch.setTextView(findViewById(R.id.stop_time))
    }

    fun processPitch(pitchInHz:Float, pitch_detection_isRunning:Boolean){
        var tv:TextView = findViewById<TextView>(R.id.frequence)
        var tv_decode_result = findViewById<TextView>(R.id.decode_result)

        var current_sys_time:Long = System.currentTimeMillis()
        if(pitch_detection_isRunning){
            tv.text = pitchInHz.toString()
            tv_decode_result.text = temp_decode_morse
            if (pitchInHz < 1100 && pitchInHz > 900){//当前低电平是line或dit（高电平）
                if(last_state == 0){//这是一个上升沿
                    var Low_duratinog_t_ms = current_sys_time - last_low_sys_time
                    if (Low_duratinog_t_ms > 1000){//被认可的摩斯码符号间隔
                        this.last_high_sys_time = current_sys_time//更新系统状态
                        this.last_state = 1
                        temp_decode_morse = temp_decode_morse.plus('/')//更新摩斯码
                        println(temp_decode_morse)
                    }
                    else if (Low_duratinog_t_ms > 50)//码字间隔
                    {
                        this.last_state = 1
                        this.last_high_sys_time = current_sys_time
                    }
                    else{//不被接受的符号间隔和码字间隔对系统没有影响

                    }
                }
                else{//处于高电平当中
                }
            }
            else{//当前电平是低电平
                if (this.last_state == 1){//这是一个下降沿
                    var High_duration_t_ms = current_sys_time -last_high_sys_time
                    if (High_duration_t_ms >50){//被认可的摩斯码符号脉冲
                        this.last_low_sys_time = current_sys_time//更新
                        this.last_state = 0
                        if (High_duration_t_ms > 200){//判断符号类型
                            temp_decode_morse = temp_decode_morse.plus('-')
                        }
                        else{
                            temp_decode_morse = temp_decode_morse.plus('.')
                        }
                    }
                    else{//不被接受的摩斯码脉冲

                    }
                }
                else{//处于低电平当中

                }
            }

        }
        else{
            tv_decode_result.text = ""
        }

    }


    fun clickHandler(source: View)
    {
        //获取UI界面中ID为R.id.show的文本框
        var tv:TextView = findViewById<TextView>(R.id.show)
        var input_text:TextInputEditText = findViewById<TextInputEditText>(R.id.text_ready_to_morse_encode)
        tv.text = morse_encode_text(input_text.getText().toString())
    }
    fun morse_encode_text(string_ready_to_morse_encode: String):String{
        val morse_table:Map<Char,String> = mapOf('A' to ".-/",'B' to "-.../",'C' to "-.-./",'D' to "-../",
            'E' to "./",'F' to "..-./",'G' to "--./",'H' to "..../",'I' to "../",'J' to ".---/",'K' to "-.-/",
            'L' to ".-../",'M' to "--/",'N' to "-./",'O' to "---/",'P' to ".--./", 'Q' to "--.-/",'R' to ".-./",
            'S' to ".../",'T' to "-/",'U' to "..-/",'V' to "...-/",'W' to ".--/",'X' to "-..-/",'Y' to "-.--/", 'Z' to "--../",
            '0' to "-----/",'1' to ".----/",'2' to "..---/",'3' to "...--/",'4' to "....-/",'5' to "...../",
            '6' to "-..../",'7' to "--.../", '8' to "---../",'9' to "----./",
            '?' to "..--../",'/' to "-..-./", '(' to "-.--./" ,')' to "-.--.-/",'-' to "-....-/",
            '.' to ".-.-.-/",',' to "..-../", '!' to "..--./",':' to "---.../",';' to "-.-.-/",
            '+' to ".-.-./",'=' to "-...-/")
        var result_morse_string:String = ""
        for (char in string_ready_to_morse_encode){
            result_morse_string = result_morse_string.plus(morse_table[char])
            }
        return result_morse_string
    }

    fun clickHandler_voice(view: View) {
        //获取UI界面中ID为R.id.show的文本框
        var tv:TextView = findViewById<TextView>(R.id.show)
        var input_text:TextInputEditText = findViewById<TextInputEditText>(R.id.text_ready_to_morse_encode)

        val string_ready_to_morse_encode = input_text.getText().toString()
        morse_encode_voice(string_ready_to_morse_encode)
    }
    fun morse_encode_voice(string_ready_to_morse_encode: String){
        val morse_string = morse_encode_text(string_ready_to_morse_encode)
        val mPlayer_dit = MediaPlayer.create(this,R.raw.dit)
        val mPlayer_line = MediaPlayer.create(this,R.raw.line)
        for (char in morse_string){
            when(char) {
                '.' -> {
                    mPlayer_dit.start()
                    Thread.currentThread()
                    Thread.sleep(400)
                }
                '-' -> {
                    mPlayer_line.start()
                    Thread.currentThread()
                    Thread.sleep(500)
                }
                '/' -> {
                    Thread.currentThread()
                    Thread.sleep(1000)
                }
            }
        }
        mPlayer_dit.stop()
        mPlayer_line.stop()
    }

    fun clickHandler_decode(view: View) {
//val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(2048, 1024, 0)
        pitch_detection_isRunning = pitch_detection_isRunning == false
    }
}
