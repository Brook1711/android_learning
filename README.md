# android_learning
老哥交个朋友，给个fork star follow三连，PS：码农不易请对他们温柔一点
## 参与贡献
郭代码 魏院士 胡经理 张青年 冯长江
## 开箱即用
主目录下app-debug.apk是项目成品，可以直接安装（只支持安卓9以上的手机固件）
## 视频演示：
主目录下：视频演示.mp4
## 介绍
{这是一个完整的Android Studio开发工程（利用kotlin语言开发），开发了一个安卓端app。该APP可以实现Morse编码和解码，编码：在文本框内输入你想要发送的文本（支持大写英文字母和基础标点符号，点击屏幕下方“Morse”按键，可以将文本编码为“.”和“-”并显示在屏幕上。输入文本后，点击“voice”按键可以将文本对应的滴答声音通过手机扬声器播放出来。发出声音后，另一个安装有该软件的安卓手机可以收听滴答声并实时判断“.”、“-”并输出在屏幕上。点击“decode”按键即可将收听到的滴答声翻译为文本}

## 软件架构
主程序在MyApplication1\app\src\main\java\com\example\myapplication1\MainActivity.kt
该文件包含了主模块和功能函数
### 1、主模块如下：

```
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
```
### 2、后台音频处理进程：
音频处理引用了TarsosDSP库，他们的代码非常好参见GitHub.com/TarsosDSP。这是一个简单易用、功能强大的适用于安卓开发的音频处理库
本工程用这个API库中的PitchDetection定义一个触发器——类似于中断函数、每当一个能量集中的声波到来的时候便会触发以下模块：

```
       val pdh = PitchDetectionHandler{ res, e ->
            val pitchInHz = res.pitch
            runOnUiThread{processPitch(pitchInHz, pitch_detection_isRunning)}
        }
```
可以看到这个进程默认始终运行，运行一个功能函数——processPitch
该模块是本工程的核心，它识别一个声波群的持续时长并判决到底是“.”还是“-”。并将结果暂存。特别注意，只有频率为1000Hz左右（900Hz~1100Hz）的声波群才被接受——1000Hz是本工程Morse编码中发出的声音频率

```
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
                        if (High_duration_t_ms > 250){//判断符号类型
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

```

### 3、decode按钮：
这里的每个按钮都和一个事件监控函数绑定，decode和以下函数绑定：
该模块实现将声音处理得到的“.”“-”形式的Morse码翻译为正常的文本

```
    fun clickHandler_decode(view: View) {
//val dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(2048, 1024, 0)
        var result_text = findViewById<TextView>(R.id.decode_result).text.toString()
        var tv_decode_text = findViewById<TextView>(R.id.decode_text)
        tv_decode_text.text = from_morse_to_text(result_text)
    }
    fun from_morse_to_text(result_text:String):String{
        val morse_decode_table:Map<String,Char> = mapOf(".-/" to 'A',"-.../" to 'B' ,"-.-./" to 'C',"-../" to 'D',
            "./" to 'E',"..-./" to 'F', "--./" to 'G',"..../" to 'H',"../" to 'I', ".---/" to 'J',"-.-/" to 'K',
            ".-../" to 'L', "--/" to 'M', "-./" to 'N', "---/" to 'O',".--./" to 'P' ,  "--.-/" to 'Q',".-./" to 'R',
            ".../" to 'S', "-/" to 'T', "..-/" to 'U', "...-/" to 'V', ".--/" to 'W', "-..-/" to 'X',"-.--/" to 'Y' , "--../" to  'Z',
            "-----/" to '0', ".----/" to '1',"..---/" to '2', "...--/" to '3', "....-/" to '4', "...../" to '5',
            "-..../" to '6', "--.../" to '7', "---../" to '8',"----./" to '9' ,
            "..--../" to '?',"-..-./" to  '/', "-.--./" to '(', "-.--.-/" to ')', "-....-/" to '-',
            ".-.-.-/" to '.', "..-../" to ',' , "..--./" to '!', "---.../" to ':', "-.-.-/" to ';',
            ".-.-./" to '+', "-...-/" to '=')
        var finnal_result = ""
        var temp_str = ""
        var index = 0
        for(char in result_text){
            if(char == '/'){
                if(temp_str != ""){
                    temp_str = temp_str.plus('/')
                    finnal_result = finnal_result.plus(morse_decode_table[temp_str])
                }
                temp_str = ""
            }
            else if(index == result_text.length - 1){
                temp_str = temp_str.plus(char)
                temp_str = temp_str.plus('/')
                if(temp_str != ""){
                    println(temp_str)
                    finnal_result = finnal_result.plus(morse_decode_table[temp_str])
                }
            }
            else{
                temp_str = temp_str.plus(char)
            }
            index = index + 1
        }
        return finnal_result
    }
}
```


### 4、Morse code 按钮
和以下函数绑定：
这是一个简单的将文本映射到Morse编码的函数

```
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

```

### 5、Voice 按钮
该部分利用Android多媒体管理模块控制Morse编码的声音播放

```
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
                    Thread.sleep(500)
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

```




## 安装教程

1.  你可以直接利用工程调试
2.  或者直接安装app——MyApplication1\app\release\app-release.apk

## 使用说明

1.  编码：在文本框内输入你想要发送的文本（支持大写英文字母和基础标点符号，点击屏幕下方“Morse”按键，可以将文本编码为“.”和“-”并显示在屏幕上。
2.  输入文本后，点击“voice”按键可以将文本对应的滴答声音通过手机扬声器播放出来。
3.  另一个安装有该软件的安卓手机可以收听滴答声并实时判断“.”、“-”并输出在屏幕上。点击“decode”按键即可将收听到的滴答声翻译为文本




