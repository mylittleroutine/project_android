package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.adapter.AdapterDateInfoList
import com.example.realtodoapp.adapter.AdapterToDoPackageList
import com.example.realtodoapp.databinding.DialogAddTodoBinding
import com.example.realtodoapp.databinding.DialogDefaultBinding
import com.example.realtodoapp.databinding.FragmentMainBinding
import com.example.realtodoapp.databinding.ItemTodoPackageBinding
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class MainFragment : Fragment(){
    lateinit var fragmentMainBinding: FragmentMainBinding
    lateinit var itemTodoPackageBinding: ItemTodoPackageBinding
    lateinit var dialogDefaultBinding: DialogDefaultBinding
    lateinit var dialogAddTodoBinding: DialogAddTodoBinding

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor :SharedPreferences.Editor

    var curYear = 0
    var curMonth = 0
    var curDay = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("CheckResult", "CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()

        // Inflate the layout for this fragment
        fragmentMainBinding = FragmentMainBinding.inflate(layoutInflater)
        itemTodoPackageBinding = ItemTodoPackageBinding.inflate(layoutInflater)
        dialogDefaultBinding = DialogDefaultBinding.inflate(layoutInflater)
        dialogAddTodoBinding = DialogAddTodoBinding.inflate(layoutInflater)

        var todoByDayRecyclerView = fragmentMainBinding.fragmentMainRecyclerView
        var todoByDayRecyclerViewAdapter = setTodoByDayRecyclerView(todoByDayRecyclerView)

        var todoByTimeRecyclerView = fragmentMainBinding.todoByTimeRecyclerView
        var todoByTimeRecyclerViewAdapter = setTodoByTimeRecyclerView(todoByTimeRecyclerView)

        var dateInfoRecyclerView = fragmentMainBinding.dateInfoRecyclerview
        var dateInfoRecyclerViewAdapter = setDateInfoRecyclerView(dateInfoRecyclerView)

        fragmentMainBinding.addTodoButton.setOnClickListener(){
            var todoList = mutableListOf<TodoPackageDto>()
            var emptyTodoListJson = gson.toJson(todoList)

            var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
            todoList = gson.fromJson(todoListJson) // 기기에 있는 todoList 가져옴

            // todolist 생성 dialog 띄움
            val dialog = Dialog(requireContext())
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if(dialogAddTodoBinding.root.parent != null){
                (dialogAddTodoBinding.root.parent as ViewGroup).removeView(
                    dialogAddTodoBinding.root
                ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                dialog.dismiss()
            }
            dialog.setContentView(dialogAddTodoBinding.root)
            var params: WindowManager.LayoutParams = dialog.getWindow()!!.getAttributes()
            params.width = (requireContext().getResources()
                .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
            params.height = (requireContext().getResources()
                .getDisplayMetrics().heightPixels * 0.5).toInt() // device의 세로 길이에 비례하여  결정
            dialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.getWindow()!!.setAttributes(params)
            dialog.getWindow()!!.setGravity(Gravity.CENTER)
            dialog.setCancelable(true)
            dialog.show()

            dialogAddTodoBinding.okButton.setOnClickListener(){
                var newTodo = TodoPackageDto()
                var year = dialogAddTodoBinding.todoYearEditText.getText().toString()
                var month = dialogAddTodoBinding.todoMonthEditText.getText().toString()
                var day = dialogAddTodoBinding.todoDayEditText.getText().toString()
                var hour = dialogAddTodoBinding.todoHourEditText.getText().toString()
                var minute = dialogAddTodoBinding.todoMinuteEditText.getText().toString()

                newTodo.year = Integer.parseInt(year)
                newTodo.month = Integer.parseInt(month)
                newTodo.day = Integer.parseInt(day)

                if(dialogAddTodoBinding.diableTimeCheckBox.isChecked){
                    newTodo.name = dialogAddTodoBinding.todoNameEditText.getText().toString()
                    newTodo.time = "TODAY"
                    todoList.add(newTodo)
                }
                else{
                    newTodo.name = dialogAddTodoBinding.todoNameEditText.getText().toString()
                    newTodo.hour = Integer.parseInt(hour)
                    newTodo.minute = Integer.parseInt(minute)
                    newTodo.time = hour +":" + minute
                    todoList.add(newTodo)
                } // 시간 설정 여부에 따라 다른 방식으로 dto 추가

                todoListJson = gson.toJson(todoList)

                sharedPrefEditor.putString("myTodoList", todoListJson)
                sharedPrefEditor.commit()

                refreshTodoList()
                dateInfoRecyclerViewAdapter = setDateInfoRecyclerView(dateInfoRecyclerView)

                if(dialogAddTodoBinding.root.parent != null) {
                    dialog.dismiss()
                    (dialogAddTodoBinding.root.parent as ViewGroup).removeView(
                        dialogAddTodoBinding.root
                    ) // 다음에 쓰기 위해 view 삭제
                }
            }

//            var sample1 = TodoPackageDto()
//            sample1.name = "TODAY"
//            todoList.add(sample1)

        }

        fragmentMainBinding.deleteAllButton.setOnClickListener(){
            var emptyTodoList = mutableListOf<TodoPackageDto>()
            var emptyTodoListJson = gson.toJson(emptyTodoList)

            sharedPrefEditor.putString("myTodoList", emptyTodoListJson)
            sharedPrefEditor.commit()

            refreshTodoList()
            dateInfoRecyclerViewAdapter = setDateInfoRecyclerView(dateInfoRecyclerView)
        }


        val view = fragmentMainBinding.root
        return view
    }

    fun setTodoByDayRecyclerView(recyclerView: RecyclerView): AdapterToDoPackageList{
        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        var filteredTodoList = mutableListOf<TodoPackageDto>()

        for(todo in todoList){
            if(todo.time == "TODAY" && todo.year == curYear && todo.month == curMonth && todo.day == curDay)
            {
                filteredTodoList.add(todo)
            }
        }

        recyclerView.adapter = AdapterToDoPackageList(requireContext(), filteredTodoList, dialogDefaultBinding)
        val adapter = recyclerView.adapter as AdapterToDoPackageList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }

    fun setTodoByTimeRecyclerView(recyclerView: RecyclerView): AdapterToDoPackageList{
        var todoList = mutableListOf<TodoPackageDto>()
        var emptyTodoListJson = gson.toJson(todoList)

        var todoListJson = sharedPref.getString("myTodoList",emptyTodoListJson).toString()
        todoList = gson.fromJson(todoListJson)

        var filteredTodoList = mutableListOf<TodoPackageDto>()

        for(todo in todoList){
            if(todo.time != "TODAY" && todo.year == curYear && todo.month == curMonth && todo.day == curDay)
            {
                filteredTodoList.add(todo)
            }
        }

        val comparator: Comparator<TodoPackageDto> =
            Comparator<TodoPackageDto> { a, b -> a.hour * 60 + a.minute - b.hour * 60 - b.minute } // 시간순 정렬

        Collections.sort(filteredTodoList, comparator)

        recyclerView.adapter = AdapterToDoPackageList(requireContext(), filteredTodoList, dialogDefaultBinding)
        val adapter = recyclerView.adapter as AdapterToDoPackageList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }

    fun setDateInfoRecyclerView(recyclerView: RecyclerView): AdapterDateInfoList{
        var dateInfoList = mutableListOf<DateInfoDto>()

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

        Collections.sort(todoList, yearComparator)
        Collections.sort(todoList, monthComparator)
        Collections.sort(todoList, dayComparator)

        var lastTodo = TodoPackageDto()

        for(todo in todoList){
            if(!(todo.year == lastTodo.year && todo.month == lastTodo.month && todo.day == lastTodo.day)){ // 날짜가 바뀔 경우
                var newDateInfo = DateInfoDto()
                newDateInfo.year = todo.year
                newDateInfo.month = todo.month
                newDateInfo.day = todo.day

                dateInfoList.add(newDateInfo)
                lastTodo = todo
            }
        }

        recyclerView.adapter = AdapterDateInfoList(requireContext(),dateInfoList)
        val adapter = recyclerView.adapter as AdapterDateInfoList
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.setDateInfoOnClickListener(object:AdapterDateInfoList.DateInfoOnClickListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onClick(view: View, position: Int, item: DateInfoDto) {
                curYear = item.year
                curMonth = item.month
                curDay = item.day
                adapter.pickedYear = curYear
                adapter.pickedMonth = curMonth
                adapter.pickedDay = curDay
                adapter.notifyDataSetChanged() // 아이템 업데이트 - setItem을 다시 수행하여 pick된 날짜가 초록색으로 보이게 함

                refreshTodoList()
            }

        })

        return adapter
    }

    fun refreshTodoList(){
        setTodoByDayRecyclerView(fragmentMainBinding.fragmentMainRecyclerView)
        setTodoByTimeRecyclerView(fragmentMainBinding.todoByTimeRecyclerView)
    }
}