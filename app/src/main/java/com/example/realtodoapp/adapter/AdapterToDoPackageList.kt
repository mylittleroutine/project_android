package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.DialogDefaultBinding
import com.example.realtodoapp.databinding.DialogGraphBinding
import com.example.realtodoapp.databinding.ItemTodoPackageBinding
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.ArrayList

class AdapterToDoPackageList(val context: Context, var list: List<TodoPackageDto>, var dialogDefaultBinding: DialogDefaultBinding, var dialogGraphBinding: DialogGraphBinding) : RecyclerView.Adapter<TodoPackageHolder>(){
    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoPackageHolder {
        var bind = ItemTodoPackageBinding.inflate(LayoutInflater.from(context), parent, false)
        return TodoPackageHolder(context, bind, dialogDefaultBinding, dialogGraphBinding)
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

class TodoPackageHolder(val context: Context, var bind: ItemTodoPackageBinding, var dialogDefaultBinding: DialogDefaultBinding, var dialogGraphBinding: DialogGraphBinding) : RecyclerView.ViewHolder(bind.root) {
    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor

    fun setItem(item: TodoPackageDto, manualCertOnClickListener: AdapterToDoPackageList.ManualCertOnClickListener){
        bind.todoNameTextView.setText(item.name)
        bind.todoTimeTextView.setText(item.time)
        if(item.time == "TODAY"){
            bind.backgroundTodo.setBackgroundColor(Color.parseColor("#DDDDDD"))
        }
        if(item.success == true){
            bind.sucessOrFailImageView.setImageResource(R.drawable.round_layout)
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

            // 자동 인식 테스트
            if(item.certType == "auto"){

                sharedPref = context.getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
                sharedPrefEditor = sharedPref.edit()

                var interActiveScreenRecord = mutableListOf<Boolean>()
                var emptyInterActiveScreenRecordJson = gson.toJson(interActiveScreenRecord)

                var interActiveScreenRecordJson = sharedPref.getString("interActiveScreenRecord"+
                    item.year.toString()+item.month.toString()+item.day.toString()+item.hour.toString()+item.minute.toString(),emptyInterActiveScreenRecordJson).toString()
                interActiveScreenRecord = gson.fromJson(interActiveScreenRecordJson)

                var testRecord = ""

                for(record in interActiveScreenRecord){
                    testRecord = testRecord.plus(record.toString())
                }

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

                // chart 데이터 설정하여 출력력
                setChartData(interActiveScreenRecord)
            }
       }
    }

    fun setChartData(records: MutableList<Boolean>){
        var chartData = LineData()
        var entry = ArrayList<Entry>()
        var count = 0

        for(record in records){
            if(record == false){
                entry.add(Entry(count.toFloat(), 0f))
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
    }
}