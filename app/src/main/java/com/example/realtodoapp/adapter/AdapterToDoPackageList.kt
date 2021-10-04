package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.databinding.DialogDefaultBinding
import com.example.realtodoapp.databinding.ItemTodoPackageBinding
import com.example.realtodoapp.model.TodoPackageDto

class AdapterToDoPackageList(val context: Context, var list: List<TodoPackageDto>, var dialogDefaultBinding: DialogDefaultBinding) : RecyclerView.Adapter<TodoPackageHolder>(){
    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoPackageHolder {
        var bind = ItemTodoPackageBinding.inflate(LayoutInflater.from(context), parent, false)
        return TodoPackageHolder(context, bind, dialogDefaultBinding)
    }

    override fun onBindViewHolder(holder: TodoPackageHolder, position: Int) {
        var item = items.get(position)
        //
        holder.setItem(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class TodoPackageHolder(val context: Context, var bind: ItemTodoPackageBinding, var dialogDefaultBinding: DialogDefaultBinding) : RecyclerView.ViewHolder(bind.root) {
    fun setItem(item: TodoPackageDto){
        bind.todoNameTextView.setText(item.name)
        bind.todoTimeTextView.setText(item.time)
        if(item.time == "TODAY"){
            bind.backgroundTodo.setBackgroundColor(Color.parseColor("#DDDDDD"))
        }

        bind.todoNameTextView.setOnClickListener(){
            val dialog = Dialog(context)
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
            if(dialogDefaultBinding.root.parent != null){
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

        }
    }
}