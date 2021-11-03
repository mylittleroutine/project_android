package com.example.realtodoapp.util

import android.app.Activity
import com.example.realtodoapp.ui.MainActivity
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.lang.Exception
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import java.nio.ByteBuffer

class TfliteModelUtil {
    companion object {
        fun loadTfModel(activity: Activity):Interpreter {
            return Interpreter(loadModelFile(activity))
        }

        private fun loadModelFile(activity: Activity): ByteBuffer {
            val assetFileDescriptor: AssetFileDescriptor = activity.getAssets().openFd("sentence_model.tflite")
            val fileInputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
            val fileChannel = fileInputStream.channel
            val startOffset = assetFileDescriptor.startOffset
            val len = assetFileDescriptor.length
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, len)
        }
    }
}

