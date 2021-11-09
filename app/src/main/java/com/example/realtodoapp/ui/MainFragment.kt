package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.R
import com.example.realtodoapp.adapter.AdapterAppInfoList
import com.example.realtodoapp.adapter.AdapterDateInfoList
import com.example.realtodoapp.adapter.AdapterToDoPackageList
import com.example.realtodoapp.databinding.*
import com.example.realtodoapp.model.DateInfoDto
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.AppUtil
import com.example.realtodoapp.util.LinearLayoutManagerWrapper
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapsInitializer
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainFragment : Fragment(){

    lateinit var fragmentMainBinding: FragmentMainBinding
    lateinit var itemTodoPackageBinding: ItemTodoPackageBinding
    lateinit var  dialogDefaultBinding: DialogDefaultBinding
    lateinit var dialogAddTodoBinding: DialogAddTodoBinding
    lateinit var dialogGraphBinding: DialogGraphBinding
    lateinit var dialogMapBinding: DialogMapBinding
    lateinit var dialogAppListBinding: DialogAppListBinding

    inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object: TypeToken<T>() {}.type)
    var gson: Gson = Gson()

    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor :SharedPreferences.Editor

    var curYear = 0
    var curMonth = 0
    var curDay = 0

    var saveLatitude = 0.0
    var saveLongitude = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        dialogGraphBinding = DialogGraphBinding.inflate(layoutInflater)
        dialogMapBinding = DialogMapBinding.inflate(layoutInflater)
        dialogAppListBinding = DialogAppListBinding.inflate(layoutInflater)

        // 테스트 애니메이션 실행
        testAnimation()

        var todoByDayRecyclerView = fragmentMainBinding.fragmentMainRecyclerView
        var todoByDayRecyclerViewAdapter = setTodoByDayRecyclerView(todoByDayRecyclerView)

        var todoByTimeRecyclerView = fragmentMainBinding.todoByTimeRecyclerView
        var todoByTimeRecyclerViewAdapter = setTodoByTimeRecyclerView(todoByTimeRecyclerView)

        var dateInfoRecyclerView = fragmentMainBinding.dateInfoRecyclerview
        var dateInfoRecyclerViewAdapter = setDateInfoRecyclerView(dateInfoRecyclerView)

        fragmentMainBinding.writeReviewButton.setOnClickListener(){
            // fragment에 선택된 날짜 넘겨줌
            val bundle = bundleOf("curYear" to curYear.toString(), "curMonth" to curMonth.toString(), "curDay" to curDay.toString())
            findNavController().navigate(R.id.action_mainFragment_to_reviewFragment, bundle)
        }

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

            // 현재 시간으로 default editText 설정
            val current = LocalDateTime.now()
            val yearFormatter = DateTimeFormatter.ofPattern("yyyy")
            val monthFormatter = DateTimeFormatter.ofPattern("MM")
            val dayFormatter = DateTimeFormatter.ofPattern("dd")
            val hourFormatter = DateTimeFormatter.ofPattern("HH")
            val minuteFormatter = DateTimeFormatter.ofPattern("mm")
            dialogAddTodoBinding.todoYearEditText.setText(current.format(yearFormatter))
            dialogAddTodoBinding.todoMonthEditText.setText(current.format(monthFormatter))
            dialogAddTodoBinding.todoDayEditText.setText(current.format(dayFormatter))
            dialogAddTodoBinding.todoHourEditText.setText(current.format(hourFormatter))
            dialogAddTodoBinding.todoMinuteEditText.setText(current.format(minuteFormatter))

            // 예외 앱 선택 기능 띄우기
            dialogAddTodoBinding.selectAppButton.setOnClickListener(){
                // dialog 띄움
                val appListDialog = Dialog(requireContext())
                appListDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                if(dialogAppListBinding.root.parent != null){
                    (dialogAppListBinding.root.parent as ViewGroup).removeView(
                        dialogAppListBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    appListDialog.dismiss()
                }
                appListDialog.setContentView(dialogAppListBinding.root)
                var params: WindowManager.LayoutParams = appListDialog.getWindow()!!.getAttributes()
                params.width = (requireContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
                params.height = (requireContext().getResources()
                    .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
                appListDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                appListDialog.getWindow()!!.setAttributes(params)
                appListDialog.getWindow()!!.setGravity(Gravity.CENTER)
                appListDialog.setCancelable(true)
                appListDialog.show()

                var appInfoListRecyclerview = dialogAppListBinding.appListRecyclerview
                var appInfoListRecyclerviewAdapter = setAppInfoListRecyclerview(appInfoListRecyclerview)

                dialogAppListBinding.okButton.setOnClickListener() {
                    if (dialogAppListBinding.root.parent != null) {
                        (dialogAppListBinding.root.parent as ViewGroup).removeView(
                            dialogAppListBinding.root
                        ) // 남아 있는 view 삭제
                        appListDialog.dismiss()
                    }
                }
            }

            // 지도에서 목표 위치 선택 기능 띄우기
            dialogAddTodoBinding.selectLocateButton.setOnClickListener(){
                // dialog 띄움
                val mapDialog = Dialog(requireContext())
                mapDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                if(dialogMapBinding.root.parent != null){
                    (dialogMapBinding.root.parent as ViewGroup).removeView(
                        dialogMapBinding.root
                    ) // 쓰기 위해 혹시라도 남아 있는 view 삭제
                    mapDialog.dismiss()
                }
                mapDialog.setContentView(dialogMapBinding.root)
                var params: WindowManager.LayoutParams = mapDialog.getWindow()!!.getAttributes()
                params.width = (requireContext().getResources()
                    .getDisplayMetrics().widthPixels * 0.9).toInt() // device의 가로 길이 비례하여 결정
                params.height = (requireContext().getResources()
                    .getDisplayMetrics().heightPixels * 0.7).toInt() // device의 세로 길이에 비례하여  결정
                mapDialog.getWindow()!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                mapDialog.getWindow()!!.setAttributes(params)
                mapDialog.getWindow()!!.setGravity(Gravity.CENTER)
                mapDialog.setCancelable(true)
                mapDialog.show()

                dialogMapBinding.textView.setText("목표 위치 설정")

                // 목표 위치를 지도에서 설정
                MapsInitializer.initialize(requireContext())
                dialogMapBinding.mapInDialog.onCreate(dialog.onSaveInstanceState())
                dialogMapBinding.mapInDialog.onResume()

                dialogMapBinding.mapInDialog.getMapAsync(OnMapReadyCallback {
                    if(saveLatitude == 0.0 && saveLongitude == 0.0){
                        it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(37.4921, 126.9730), 8F))
                    }
                    else{
                        it.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(saveLatitude, saveLongitude), 16F))
                    }

                    it.setOnMapClickListener (object: GoogleMap.OnMapClickListener {
                        override fun onMapClick(latLng: LatLng) {
                            it.clear()

                            it.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16F))

                            val location = LatLng(latLng.latitude,latLng.longitude)
                            it.addMarker(MarkerOptions().position(location))

                            // 현재 찍은 위치를 전역변수로 저장해둠
                            saveLatitude = latLng.latitude
                            saveLongitude = latLng.longitude
                        }

                    })
                })
                dialogMapBinding.okButton.setOnClickListener(){
                    if(dialogMapBinding.root.parent != null){
                        (dialogMapBinding.root.parent as ViewGroup).removeView(
                            dialogMapBinding.root
                        ) // 남아 있는 view 삭제
                        mapDialog.dismiss()
                    }
                }
            }

            //  체크 항목 변경
            dialogAddTodoBinding.autoScreenCheckBox.setOnCheckedChangeListener{ _, isChecked ->
                if(isChecked){
                    dialogAddTodoBinding.autoLocateCheckBox.setChecked(false)
                }
            }
            dialogAddTodoBinding.autoLocateCheckBox.setOnCheckedChangeListener{ _, isChecked ->
                if(isChecked){
                    dialogAddTodoBinding.autoScreenCheckBox.setChecked(false)
                }
            }


            dialogAddTodoBinding.okButton.setOnClickListener(){
                var newTodo = TodoPackageDto()
                var year = dialogAddTodoBinding.todoYearEditText.getText().toString()
                var month = dialogAddTodoBinding.todoMonthEditText.getText().toString()
                var day = dialogAddTodoBinding.todoDayEditText.getText().toString()
                var hour = dialogAddTodoBinding.todoHourEditText.getText().toString()
                var minute = dialogAddTodoBinding.todoMinuteEditText.getText().toString()

                // 종료 시간 있을 때 설정
                var endHour = dialogAddTodoBinding.todoEndHourEditText.getText().toString()
                var endMinute = dialogAddTodoBinding.todoEndMinuteEditText.getText().toString()

                newTodo.year = Integer.parseInt(year)
                newTodo.month = Integer.parseInt(month)
                newTodo.day = Integer.parseInt(day)

                if(dialogAddTodoBinding.autoScreenCheckBox.isChecked){
                    newTodo.certType = "SCREEN_AUTO"
                }
                else if(dialogAddTodoBinding.autoLocateCheckBox.isChecked){
                    newTodo.certType = "LOCATE_AUTO"
                }


                if(dialogAddTodoBinding.disableTimeCheckBox.isChecked){
                    newTodo.name = dialogAddTodoBinding.todoNameEditText.getText().toString()
                    newTodo.time = "TODAY"
                    todoList.add(newTodo)
                }
                else{
                    newTodo.name = dialogAddTodoBinding.todoNameEditText.getText().toString()
                    if(hour != "") newTodo.hour = Integer.parseInt(hour)
                    if(minute != "") newTodo.minute = Integer.parseInt(minute)
                    if(endHour != "") newTodo.endHour = Integer.parseInt(endHour)
                    if(endMinute != "") newTodo.endMinute = Integer.parseInt(endMinute)

                    newTodo.time = String.format("%02d",newTodo.hour) +":" + String.format("%02d",newTodo.minute)
                    todoList.add(newTodo)
                    
                    // 목표 위치 저장
                    if(newTodo.certType == "LOCATE_AUTO"){
                        // todo마다 다른 목표 위치를 저장할 필요가 있으므로 각기 다른 곳에 저장
                        var sharedPrefKeyGoal = "goalLocateRecord"+
                                newTodo.year+newTodo.month+newTodo.day+newTodo.hour.toString()+newTodo.minute.toString()
                        var goalLocateRecord = mutableListOf<Double>()

                        // 전역변수로 저장해둔 위치 불러오기
                        goalLocateRecord.add(saveLatitude)
                        goalLocateRecord.add(saveLongitude)

                        var goalLocateRecordJson = gson.toJson(goalLocateRecord)

                        sharedPrefEditor.putString(sharedPrefKeyGoal, goalLocateRecordJson) // 시간별로 따로 저장
                        sharedPrefEditor.commit()
                    }
                } // 시간 설정 여부에 따라 다른 방식으로 dto 추가

                todoListJson = gson.toJson(todoList)

                sharedPrefEditor.putString("myTodoList", todoListJson)
                sharedPrefEditor.commit()

                refreshTodoList()

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
            sharedPrefEditor.clear()
            sharedPrefEditor.commit()

            refreshTodoList()
        }

        fragmentMainBinding.userCharactor.setOnClickListener(){
            findNavController().navigate(R.id.action_mainFragment_to_myRoomFragment)
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

        recyclerView.adapter = AdapterToDoPackageList(requireActivity(), requireContext(), filteredTodoList, dialogDefaultBinding, dialogGraphBinding, dialogMapBinding)
        val adapter = recyclerView.adapter as AdapterToDoPackageList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.setManualCertOnClickListener(object: AdapterToDoPackageList.ManualCertOnClickListener{
            override fun onClick(item: TodoPackageDto) {
                updateSuccessTodo(item)
            }
        })

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

        recyclerView.adapter = AdapterToDoPackageList(requireActivity(), requireContext(), filteredTodoList, dialogDefaultBinding, dialogGraphBinding, dialogMapBinding)
        val adapter = recyclerView.adapter as AdapterToDoPackageList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.setManualCertOnClickListener(object: AdapterToDoPackageList.ManualCertOnClickListener{
            override fun onClick(item: TodoPackageDto) {
                updateSuccessTodo(item)
            }
        })

        return adapter
    }

    fun updateSuccessTodo(item: TodoPackageDto){
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

        refreshTodoList()
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

        Collections.sort(todoList, dayComparator)
        Collections.sort(todoList, monthComparator)
        Collections.sort(todoList, yearComparator)

        var lastTodo = TodoPackageDto()

        var todoCount = 0f
        var successTodoCount = 0f // 날짜별 성공횟수를 체크하기 위함
        for(todo in todoList){
            if(!(todo.year == lastTodo.year && todo.month == lastTodo.month && todo.day == lastTodo.day)){ // 날짜가 바뀔 경우
                if(lastTodo.year != 0) // 초기 상태가 아닐 시 날짜 dto 추가
                {
                    var lastDateInfo = DateInfoDto()
                    lastDateInfo.year = lastTodo.year
                    lastDateInfo.month = lastTodo.month
                    lastDateInfo.day = lastTodo.day
                    lastDateInfo.successProgress = successTodoCount / todoCount

                    dateInfoList.add(lastDateInfo)
                    successTodoCount = 0f
                    todoCount = 0f
                }
            }
            todoCount +=1
            if(todo.success == true) successTodoCount +=1
            lastTodo = todo
        }
        // 마지막 날짜 dto 추가
        if(lastTodo.year != 0) // 초기 상태가 아닐 시 날짜 dto 추가
        {
            var lastDateInfo = DateInfoDto()
            lastDateInfo.year = lastTodo.year
            lastDateInfo.month = lastTodo.month
            lastDateInfo.day = lastTodo.day
            lastDateInfo.successProgress = successTodoCount / todoCount

            dateInfoList.add(lastDateInfo)
        }

        recyclerView.adapter = AdapterDateInfoList(requireContext(),dateInfoList)
        val adapter = recyclerView.adapter as AdapterDateInfoList
        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        adapter.pickedYear = curYear
        adapter.pickedMonth = curMonth
        adapter.pickedDay = curDay
        adapter.notifyDataSetChanged() // 아이템 업데이트 - setItem을 다시 수행하여 pick된 날짜가 초록색으로 보이게 함

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
        setDateInfoRecyclerView(fragmentMainBinding.dateInfoRecyclerview)
    }

    fun testAnimation(){
        val scope = GlobalScope // 비동기 함수 진행
        scope.launch {
            while (true) {
                delay(100)
                fragmentMainBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                    fragmentMainBinding.userCharactor.setImageResource(R.drawable.test_charactor_1)
                }
                delay(100)
                fragmentMainBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                    fragmentMainBinding.userCharactor.setImageResource(R.drawable.test_charactor_2)
                }
                delay(100)
                fragmentMainBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                    fragmentMainBinding.userCharactor.setImageResource(R.drawable.test_charactor_3)
                }
            }
        }

    }

    fun setAppInfoListRecyclerview(recyclerView: RecyclerView): AdapterAppInfoList{
        val installedApps = AppUtil.getInstalledApp(requireContext())

        recyclerView.adapter = AdapterAppInfoList(requireContext(), installedApps)
        val adapter = recyclerView.adapter as AdapterAppInfoList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }

}