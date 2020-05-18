# android_learning

#### 介绍
{这是一个完整的Android Studio开发工程（利用kotlin语言开发），开发了一个安卓端app。该APP可以实现Morse编码和解码，编码：在文本框内输入你想要发送的文本（支持大写英文字母和基础标点符号，点击屏幕下方“Morse”按键，可以将文本编码为“.”和“-”并显示在屏幕上。输入文本后，点击“voice”按键可以将文本对应的滴答声音通过手机扬声器播放出来。发出声音后，另一个安装有该软件的安卓手机可以收听滴答声并实时判断“.”、“-”并输出在屏幕上。点击“decode”按键即可将收听到的滴答声翻译为文本}

#### 软件架构
主程序在MyApplication1\app\src\main\java\com\example\myapplication1\MainActivity.kt
该文件包含了主模块和功能函数
1、主模块如下：

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


#### 安装教程

1.  你可以直接利用工程调试
2.  或者直接安装app——MyApplication1\app\release\app-release.apk

#### 使用说明

1.  编码：在文本框内输入你想要发送的文本（支持大写英文字母和基础标点符号，点击屏幕下方“Morse”按键，可以将文本编码为“.”和“-”并显示在屏幕上。
2.  输入文本后，点击“voice”按键可以将文本对应的滴答声音通过手机扬声器播放出来。
3.  另一个安装有该软件的安卓手机可以收听滴答声并实时判断“.”、“-”并输出在屏幕上。点击“decode”按键即可将收听到的滴答声翻译为文本

#### 参与贡献

1.  Fork 本仓库
2.  新建 Feat_xxx 分支
3.  提交代码
4.  新建 Pull Request


