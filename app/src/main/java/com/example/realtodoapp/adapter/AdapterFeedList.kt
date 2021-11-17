package com.example.realtodoapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.databinding.ItemAppInfoBinding
import com.example.realtodoapp.databinding.ItemFeedBinding
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.util.AppUtil

class AdapterFeedList(val context: Context, var list: List<FeedDto>): RecyclerView.Adapter<FeedListHolder>() {
    var items = list
        @SuppressLint("NotifyDataSetChanged")
        set(value){
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedListHolder {
        var bind = ItemFeedBinding.inflate(LayoutInflater.from(context), parent, false)
        return FeedListHolder(context, bind)
    }

    override fun onBindViewHolder(holder: FeedListHolder, position: Int) {
        var item = items.get(position)
        holder.setItem(item)
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

class FeedListHolder(val context: Context, var bind: ItemFeedBinding): RecyclerView.ViewHolder(bind.root) {
    fun setItem(item:FeedDto){
        bind.feedWriter.setText(item.uploader)
        bind.feedTitle.setText(item.title)
        bind.feedContents.setText(item.contents)
    }
}