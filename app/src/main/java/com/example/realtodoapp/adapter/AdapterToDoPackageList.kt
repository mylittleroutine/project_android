package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.DialogDefaultBinding
import com.example.realtodoapp.databinding.DialogGraphBinding
import com.example.realtodoapp.databinding.DialogMapBinding
import com.example.realtodoapp.databinding.ItemTodoPackageBinding
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.gms.maps.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


class AdapterToDoPackageList(val activity: FragmentActivity, val fragment: Fragment, val context: Context, var list: List<TodoPackageDto>, var dialogDefaultBinding: DialogDefaultBinding,
                             var dialogGraphBinding: DialogGraphBinding, var dialogMapBinding: DialogMapBinding) : RecyclerView.Adapter<TodoPackageHolder>(){
    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoPackageHolder {
        var bind = ItemTodoPackageBinding.inflate(LayoutInflater.from(context), parent, false)
        return TodoPackageHolder(activity, fragment, context, bind, dialogDefaultBinding, dialogGraphBinding, dialogMapBinding)
    }

    override fun onBindViewHolder(holder: TodoPackageHolder, position: Int) {
        var item = items.get(position)
//        holder.dialogDefaultBinding.certButton.setOnClickListener(){
//            manualCertOnClickListener.onClick(it, position, item )
//        }
        holder.setItem(item, manualCertOnClickListener) // setItem에서 인증 버튼 인식해야지 제대로 된 item 인식
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface ManualCertOnClickListener{
        fun onClick(item: TodoPackageDto)
    }

    private lateinit var manualCertOnClickListener: ManualCertOnClickListener

    fun setManualCertOnClickListener(manualCertOnClickListener: ManualCertOnClickListener){
        this.manualCertOnClickListener = manualCertOnClickListener
    }
}

class TodoPackageHolder(val activity: FragmentActivity, val fragment:Fragment, val context: Context, var bind: ItemTodoPackageBinding, var dialogDefaultBinding: DialogDefaultBinding,
                        var dialogGraphBinding: DialogGraphBinding, var dialogMapBinding: DialogMapBinding) : RecyclerView.ViewHolder(bind.root){
    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor
    lateinit var map:GoogleMap

    fun setItem(item: TodoPackageDto, manualCertOnClickListener: AdapterToDoPackageList.ManualCertOnClickListener){
        bind.todoNameTextView.setText(item.name)
        bind.todoTimeTextView.setText(item.time)
        if(item.time == "TODAY"){
            bind.backgroundTodo.setBackgroundColor(Color.parseColor("#DDDDDD"))
        }
        if(item.success == true){
            bind.sucessOrFailImageView.setImageResource(R.drawable.success_check)
        }

        bind.todoNameTextView.setOnClickListener(){
            //수동 인식 기능
            if (item.certType == "") {
                val dialog = Dialog(context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                if (dialogDefaultBinding.root.parent != null) {
                    (dialogDefaultBinding.root.parent as ViewGroup).removeView(
                        dialogDefaultBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
                dialog.setContentView(dialogDefaultBinding.root)
                var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
                params.width = (context.getResources()
                    .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
                params.height = (context.getResources()
                    .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
                dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.getWindow()!!.setAttributes(params)
                dialog.getWindow()!!.setGravity(Gravity.CENTER)
                dialog.setCancelable(true)
                dialog.show()

                dialogDefaultBinding.textName.setText(item.name)

                dialogDefaultBinding.certButton.setOnClickListener() {
                    manualCertOnClickListener.onClick(item)
                }
            }

            // 자동 인식 테스트 현황 출력
            if(item.certType == "SCREEN_AUTO"){

                sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
                sharedPrefEditor = sharedPref.edit()

                var interActiveScreenRecord = mutableListOf<Boolean>()
                var emptyInterActiveScreenRecordJson = gson.toJson(interActiveScreenRecord)

                var interActiveScreenRecordJson = sharedPref.getString("interActiveScreenRecord"+
                    item.year.toString()+item.month.toString()+item.day.toString()+item.hour.toString()+item.minute.toString(),emptyInterActiveScreenRecordJson).toString()
                interActiveScreenRecord = gson.fromJson(interActiveScreenRecordJson)

                // dialog로 상태 출력
                val dialog = Dialog(context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                if (dialogGraphBinding.root.parent != null) {
                    (dialogGraphBinding.root.parent as ViewGroup).removeView(
                        dialogGraphBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
                dialog.setContentView(dialogGraphBinding.root)
                var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
                params.width = (context.getResources()
                    .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
                params.height = (context.getResources()
                    .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
                dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.getWindow()!!.setAttributes(params)
                dialog.getWindow()!!.setGravity(Gravity.CENTER)
                dialog.setCancelable(true)
                dialog.show()

                setChartData(interActiveScreenRecord)
            }

            if(item.certType == "LOCATE_AUTO"){
                sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
                sharedPrefEditor = sharedPref.edit()

                // dialog로 상태 출력
                val dialog = Dialog(context)
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                if (dialogMapBinding.root.parent != null) {
                    (dialogMapBinding.root.parent as ViewGroup).removeView(
                        dialogMapBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    dialog.dismiss()
                }
                dialog.setContentView(dialogMapBinding.root)
                var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
                params.width = (context.getResources()
                    .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
                params.height = (context.getResources()
                    .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
                dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                dialog.getWindow()!!.setAttributes(params)
                dialog.getWindow()!!.setGravity(Gravity.CENTER)
                dialog.setCancelable(true)
                dialog.show()

                dialogMapBinding.textView.setText("목표 위치 도달 시 성공")

                // google map 설정

                // 현재 저장된 위치 정보 가져옴
                var sharedPrefKey = "currentLocateRecord"+
                        item.year.toString()+item.month.toString()+
                        item.day.toString()+item.hour.toString()+item.minute.toString()
                var currentLocateRecord = mutableListOf<Double>()
                var emptyCurrentLocateRecord = gson.toJson(currentLocateRecord)

                var currentLocateRecordJson = sharedPref.getString(sharedPrefKey,emptyCurrentLocateRecord).toString()
                currentLocateRecord = gson.fromJson(currentLocateRecordJson)

                // 목표 위치 가져옴
                var sharedPrefKeyGoal = "goalLocateRecord"+
                        item.year.toString()+item.month.toString()+item.day+item.hour.toString()+item.minute.toString()
                var goalLocateRecord = mutableListOf<Double>()
                var emptyGoalLocateRecord = gson.toJson(goalLocateRecord)

                var goalLocateRecordJson = sharedPref.getString(sharedPrefKeyGoal,emptyGoalLocateRecord).toString()
                goalLocateRecord = gson.fromJson(goalLocateRecordJson)

                var mapView = dialogMapBinding.mapInDialog
                MapsInitializer.initialize(activity)
                mapView.onCreate(dialog.onSaveInstanceState())
                mapView.onResume()
                mapView.getMapAsync(OnMapReadyCallback{
                    map = it
                    // Add a marker in Sydney and move the camera
                    val goalLocate = LatLng(goalLocateRecord[0], goalLocateRecord[1])
                    map.addMarker(MarkerOptions().position(goalLocate).title("목표지점"))

                    var latitude = 0.0
                    var longitude = 0.0
                    if(!currentLocateRecord.isEmpty()){
                        latitude = currentLocateRecord[0]
                        longitude = currentLocateRecord[1]
                    }

                    val currentLocate = LatLng(latitude, longitude)
                    map.addMarker(MarkerOptions().position(currentLocate).title("my location"))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(goalLocate, 16F))
                })

            }
       }

        bind.reviewImageView.setOnClickListener(){
            val bundle = bundleOf("curYear" to item.year.toString(), "curMonth" to item.month.toString(), "curDay" to item.day.toString(),
                "todoName" to item.name, "todoSuccess" to item.success)
            findNavController(fragment).navigate(R.id.action_mainFragment_to_reviewFragment, bundle)
        }
    }

    @SuppressLint("SetTextI18n")
    fun setChartData(records: MutableList<Boolean>){
        var chartData = LineData()
        var entry = ArrayList<Entry>()
        var count = 0
        var offCount = 0

        for(record in records){
            if(record == false){
                entry.add(Entry(count.toFloat(), 0f))
                offCount +=1
            }
            if(record == true){
                entry.add(Entry(count.toFloat(), 1f))
            }
            count +=1
        }

        var lineDataSet = LineDataSet(entry, "화면")
        chartData.addDataSet(lineDataSet)
        dialogGraphBinding.linechart.setData(chartData)
        dialogGraphBinding.linechart.invalidate()

        var offRatio = ((offCount.toFloat() / count.toFloat()) *100).toInt()
        dialogGraphBinding.graphResultTextView.setText("화면 꺼짐 비율: "+ offRatio +"%")

    }

}