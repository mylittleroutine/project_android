package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.realtodoapp.adapter.AdapterFeedList
import com.example.realtodoapp.databinding.FragmentCommunityBinding
import com.example.realtodoapp.model.FeedDto
import com.example.realtodoapp.util.LinearLayoutManagerWrapper

class CommunityFragment: Fragment() {
    lateinit var fragmentCommunityBinding: FragmentCommunityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("CheckResult", "CommitPrefEdits")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentCommunityBinding = FragmentCommunityBinding.inflate(layoutInflater)

        var feedList = mutableListOf<FeedDto>()

        var testFeedDto= FeedDto()
        testFeedDto.title = "테스트 제목"
        testFeedDto.contents = "테스트 내용"

        feedList.add(testFeedDto)

        var feedRecyclerView = fragmentCommunityBinding.feedRecyclerView
        var feedRecyclerViewAdapter = setFeedRecyclerView(feedRecyclerView, feedList)


        val view = fragmentCommunityBinding.root
        return view
    }

    fun setFeedRecyclerView(recyclerView: RecyclerView, list:List<FeedDto>): AdapterFeedList {

        recyclerView.adapter = AdapterFeedList(requireContext(), list)
        val adapter = recyclerView.adapter as AdapterFeedList
        val linearLayoutManager = LinearLayoutManagerWrapper(requireContext())
        recyclerView.layoutManager = linearLayoutManager
        recyclerView.setHasFixedSize(true)

        return adapter
    }
}