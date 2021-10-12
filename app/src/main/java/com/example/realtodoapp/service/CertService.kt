package com.example.realtodoapp.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.example.realtodoapp.R
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.ui.MainFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class CertService: Service(), SensorEventListener {
    lateinit var sensorManager:SensorManager
    lateinit var gravitySensor: Sensor
    var isAlreadyRunning = false

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "notification_channel"
    }

    override fun onCreate() {
        super.onCreate()

        sharedPref = this.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

            // SensorEventListener 등록 (이걸 해야 onSensorChanged에서 인식 가능)
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL)

            setForeGround("", "")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationChannel = NotificationChannel(
            CHANNEL_ID,
            "MyApp notification",
            NotificationManager.IMPORTANCE_LOW
        )
//        notificationChannel.enableLights(true)
//        notificationChannel.lightColor = Color.RED
//        notificationChannel.enableVibration(true)
//        notificationChannel.description = "AppApp Tests"

        val notificationManager = applicationContext.getSystemService(
            Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(
            notificationChannel)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {  // service 시작 시 수행됨

        if(isAlreadyRunning == false) { // 서비스 실행 중 한 번만 globalscope 생성
            isAlreadyRunning = true
            val scope = GlobalScope // 비동기 함수 진행
            scope.launch {
                while (true) {
                    delay(1000)

                    // 현재 시간 불러옴
                    var now = System.currentTimeMillis()
                    var date = Date(now)
                    val simpleDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    val simpleYear = SimpleDateFormat("yyyy")
                    val simpleMonth = SimpleDateFormat("MM")
                    val simpleDay = SimpleDateFormat("dd")
                    val simpleHour = SimpleDateFormat("HH") //24시간제 표시
                    val simpleMinute = SimpleDateFormat("mm")
                    val getYear: String = simpleYear.format(date)
                    val getMonth: String = simpleMonth.format(date)
                    val getDay: String = simpleDay.format(date)
                    val getHour: String = simpleHour.format(date)
                    val getMinute: String = simpleMinute.format(date)

                    //setForeGround("날짜", getTime  + "\n" + getYear + getMonth + getDay)

                    // todo목록 불러옴
                    var todoList = mutableListOf<TodoPackageDto>()
                    var emptyTodoListJson = gson.toJson(todoList)

                    var todoListJson =
                        sharedPref.getString("myTodoList", emptyTodoListJson).toString()
                    todoList = gson.fromJson(todoListJson)

                    var filteredTodoList = mutableListOf<TodoPackageDto>()

                    for (todo in todoList) {
                        if (todo.year == Integer.parseInt(getYear) && todo.month == Integer.parseInt(
                                getMonth
                            ) && todo.day == Integer.parseInt(getDay)
                        ) {
                            filteredTodoList.add(todo)
                        }
                    }

                    val comparator: Comparator<TodoPackageDto> =
                        Comparator<TodoPackageDto> { a, b -> a.hour * 60 + a.minute - b.hour * 60 - b.minute } // 시간순 정렬
                    Collections.sort(filteredTodoList, comparator)


                    // 다음 todo를 알려줌
                    for (todo in filteredTodoList) {  // 오늘 날짜의 todo에 한해서 체크
                        if (todo.certType == "auto") // 화면 인식 방식
                        {
                            var startHour = todo.hour
                            var startMinute = todo.minute
                            var endHour = todo.endHour
                            var endMinute = todo.endMinute

                            var offRatio = recordActionScreen(startHour, startMinute, endHour, endMinute)

                            if(offRatio != -1){ // -1은 인증 체크 시간이 아님을 의미
                                if(offRatio > 70) { // 화면 꺼짐 비율 70% 이상이면 성공
                                    // 성공했으면 todo에 업데이트
                                    updateCertSuccessTodo(todo)

                                    setForeGround("todo 인증 알림" , todo.name + " 성공")
                                }
                                else{
                                    setForeGround("todo 인증 알림" , todo.name + " 실패")
                                }
                            }

                        }
                    }

                }
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    fun setForeGround(title:String, text: String){ // 알림 표시 기능
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.meat)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // 센서 부분
    var x = 0f
    var y = 0f
    var z = 0f

    override fun onSensorChanged(event: SensorEvent?) {

        if(event!!.sensor == gravitySensor){
            x = event.values[0]
            y = event.values[1]
            z = event.values[2]
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    fun recordGravity(x: Float, y:Float, z:Float){
        setForeGround(
                        "중력 좌표",
                        "x " + x.toString() + "\n" + "y " + y.toString() + "\n" + "z " + z.toString()
                    )
    }

    fun resetRecordGravity(){

    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("SimpleDateFormat")
    fun recordActionScreen(startHour:Int, startMinute:Int, endHour:Int, endMinute:Int): Int{ // 기기에 화면 켜짐/꺼짐 기록 저장, 인증 현황 return
        // 현재 시간 불러옴
        var now = System.currentTimeMillis()
        var date = Date(now)
        val simpleYear = SimpleDateFormat("yyyy")
        val simpleMonth = SimpleDateFormat("MM")
        val simpleDay = SimpleDateFormat("dd")
        val simpleHour = SimpleDateFormat("HH") //24시간제 표시
        val simpleMinute = SimpleDateFormat("mm")
        val getYear: String = simpleYear.format(date)
        val getMonth: String = simpleMonth.format(date)
        val getDay: String = simpleDay.format(date)
        val getHour: String = simpleHour.format(date)
        val getMinute: String = simpleMinute.format(date)


        var sharedPrefKey = "interActiveScreenRecord"+
                            getYear+getMonth+getDay+startHour.toString()+startMinute.toString()

        // 이전 정보 가져옴
        var interActiveScreenRecord = mutableListOf<Boolean>()
        var emptyInterActiveScreenRecord = gson.toJson(interActiveScreenRecord)

        var interActiveScreenRecordJson = sharedPref.getString(sharedPrefKey,emptyInterActiveScreenRecord).toString()
        interActiveScreenRecord = gson.fromJson(interActiveScreenRecordJson)

        var startTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+String.format("%02d",startHour)+"-"+String.format("%02d",startMinute)
        var endTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+String.format("%02d",endHour)+"-"+String.format("%02d",endMinute) // 두자리수로 맞춰줌
        var currentTimeString = getYear+"-"+getMonth+"-"+getDay+"-"+getHour+"-"+getMinute

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm")

        val startTime: LocalDateTime = LocalDateTime.parse(startTimeString, formatter)
        val endTime: LocalDateTime = LocalDateTime.parse(endTimeString, formatter)
        val currentTime:LocalDateTime = LocalDateTime.parse(currentTimeString, formatter) // 시간 비교 위해 변환


        if(currentTime.isEqual(startTime)|| (currentTime.isAfter(startTime) && currentTime.isBefore(endTime))){ // 인증 시간에만 화면 꺼짐/켜짐 측정 수행

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (pm.isInteractive) {
                interActiveScreenRecord.add(true)
            } else {
                interActiveScreenRecord.add(false)
            }

            var interActiveScreenRecordJson = gson.toJson(interActiveScreenRecord)

            sharedPrefEditor.putString(sharedPrefKey, interActiveScreenRecordJson) // 시간별로 따로 저장
            sharedPrefEditor.commit()
        }
        else if(currentTime.isEqual(endTime)){ // 인증 시간 지나면 성공 여부 체크
            var count = 0
            var offCount = 0

            for (record in interActiveScreenRecord){
                if(record == false){
                    offCount +=1
                }
                count +=1
            }

            var offRatio = ((offCount.toFloat() / count.toFloat()) *100).toInt()

            return offRatio
        }

        return -1 // 인증 완료 전에는 -1 return
    }

    fun updateCertSuccessTodo(item: TodoPackageDto){ // 인증 성공 업데이트
        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        val yearComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.year - b.year } //  년도순 정렬

        val monthComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.month - b.month } //  월순 정렬

        val dayComparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.day - b.day } //  날짜순 정렬

        Collections.sort(todoList, dayComparator)
        Collections.sort(todoList, monthComparator)
        Collections.sort(todoList, yearComparator)

        for(todo in todoList){
            if(item.year == todo.year && item.month == todo.month && item.day == todo.day &&
                item.hour == todo.hour && item.minute == todo.minute && item.name == todo.name){
                todo.success = true

                todoListJson = gson.toJson(todoList)

                sharedPrefEditor.putString("myTodoList", todoListJson)
                sharedPrefEditor.commit()

                break
            }
        }
    }

}