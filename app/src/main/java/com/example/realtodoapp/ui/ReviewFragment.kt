package com.example.realtodoapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.realtodoapp.R
import com.example.realtodoapp.databinding.FragmentReviewBinding
import com.example.realtodoapp.model.TodoPackageDto
import com.example.realtodoapp.util.TfliteModelUtil
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL
import kr.co.shineware.nlp.komoran.core.Komoran
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ReviewFragment: Fragment() {
    lateinit var fragmentReviewBinding: FragmentReviewBinding
    var lastResult = 50f
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor : SharedPreferences.Editor


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = requireContext().getSharedPreferences("sharedPref1", Context.MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()
    }

    @SuppressLint("CheckResult", "CommitPrefEdits", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        fragmentReviewBinding = FragmentReviewBinding.inflate(layoutInflater)
        val view = fragmentReviewBinding.root

        val curYear = arguments?.getString("curYear")
        val curMonth = arguments?.getString("curMonth")
        val curDay = arguments?.getString("curDay")

        fragmentReviewBinding.title.setText(curYear + "-" + curMonth + "-" + curDay + " 리뷰")

        fragmentReviewBinding.reviewEditText.setText(loadReview(curYear!!, curMonth!!, curDay!!))

        fragmentReviewBinding.saveButton.setOnClickListener(){
            var review = fragmentReviewBinding.reviewEditText.text.toString()
            saveReview(curYear!!, curMonth!!, curDay!!, review)
        }

        fragmentReviewBinding.submitButton.setOnClickListener(){
            runSentenceAI()
            runInterestAI()
        }

        return view
    }

    @SuppressLint("SetTextI18n")
    fun runInterestAI(){
        var input = fragmentReviewBinding.reviewEditText.text.toString()
        // 전체 문장일 경우만 형태소 분리하여 다시 합친 후 모델에 넣음
        var output = getInterestModelResult(input)
        fragmentReviewBinding.firstInterestTextView.setText("운동 : "+ (output.get(0)* 100).toInt().toString())
        fragmentReviewBinding.secondInterestTextView.setText("독서 : "+ (output.get(1)* 100).toInt().toString())
        fragmentReviewBinding.thirdInterestTextView.setText("여행 : "+ (output.get(2)* 100).toInt().toString())
        fragmentReviewBinding.forthInterestTextView.setText("요리 : "+ (output.get(3)* 100).toInt().toString())
    }

    fun runSentenceAI(){
        var input = fragmentReviewBinding.reviewEditText.text.toString()
        // 전체 문장일 경우만 형태소 분리하여 다시 합친 후 모델에 넣음
        var totalResult = getSentenceModelResult(input, true) * 100
        fragmentReviewBinding.resultProgressView.progress = totalResult

        if(totalResult > lastResult){
            upArrowAnimation()
        }
        else if(totalResult < lastResult){
            downArrowAnimation()
        }
        lastResult = totalResult

//        // 세부 글자 색상 변경 과정
//        fragmentReviewBinding.reviewEditText.text.clear()
//
//        // 공백 기준으로 나눔
//        var token = input.split(' ')
//        for (element in token){
//            if(element!= "") {
//                val builder = SpannableStringBuilder(element)
//                if (getSentenceModelResult(element, false) * 100 > 60) {
//                    builder.setSpan(
//                        ForegroundColorSpan(Color.BLUE), 0, element.length,
//                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                } else if(getSentenceModelResult(element, false) * 100 < 40){
//                    builder.setSpan(
//                        ForegroundColorSpan(Color.RED), 0, element.length,
//                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
//                else{
//                    builder.setSpan(
//                        ForegroundColorSpan(Color.BLACK), 0, element.length,
//                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
//                    )
//                }
//                fragmentReviewBinding.reviewEditText.text.append(builder)
//                fragmentReviewBinding.reviewEditText.text.append(" ")
//            }
//        }
    }

    fun getInterestModelResult(input: String):FloatBuffer{
        var modifiedInput = input

        var komoran = Komoran(DEFAULT_MODEL.FULL)
        var komoranResult = komoran.analyze(input)
        var tokenList = komoranResult.tokenList
        modifiedInput = ""

        for(token in tokenList){
            modifiedInput += token.morph + " "
        }

        var tfModel = TfliteModelUtil.loadInterestModel(requireActivity())
        var inputArray = Array(1){modifiedInput+" "}
        var output: ByteBuffer = ByteBuffer.allocate(4*4).order(ByteOrder.nativeOrder())
        tfModel.run(inputArray, output)

        // bytebuffer float 변환
        output.rewind()
        var pro = output.asFloatBuffer()
        return pro
    }

    fun getSentenceModelResult(input:String, toMorph:Boolean): Float{
        var modifiedInput = input
        if(toMorph == true){
            var komoran = Komoran(DEFAULT_MODEL.FULL)
            var komoranResult = komoran.analyze(input)
            var tokenList = komoranResult.tokenList
            modifiedInput = ""

            for(token in tokenList){
                modifiedInput += token.morph + " "
            }
            Log.d("modifiedInput", modifiedInput)
        }

        var tfModel = TfliteModelUtil.loadSentenceModel(requireActivity())
        var inputArray = Array(1){modifiedInput+" "}
        var output: ByteBuffer = ByteBuffer.allocate(2*4).order(ByteOrder.nativeOrder())
        tfModel.run(inputArray, output)

        // bytebuffer float 변환
        output.rewind()
        var pro = output.asFloatBuffer()
        Log.d("AITEST", pro.get(0).toString() + " "+ pro.get(1).toString())
        return pro.get(1)
    }

    fun loadReview(year:String, month:String, day:String):String{
        return sharedPref.getString("review-"+year+month+day ,"").toString()
    }

    fun saveReview(year:String, month:String, day:String, review:String){
        sharedPrefEditor.putString("review-"+year+month+day, review) // 시간별로 따로 저장
        sharedPrefEditor.commit()
    }

    fun shareReview(year:String, month:String, day:String, review:String){

    }

    fun upArrowAnimation(){
        val scope = GlobalScope // 비동기 함수 진행
        scope.launch {
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_1)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_2)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_3)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_4)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_5)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_6)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_7)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.up_arrow_8)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
        }
    }

    fun downArrowAnimation(){
        val scope = GlobalScope // 비동기 함수 진행
        scope.launch {
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_1)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_2)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_3)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_4)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_5)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_6)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_7)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(R.drawable.down_arrow_8)
            }
            delay(100)
            fragmentReviewBinding.root.post { // ui는 메인 thread에서 변경해야만 하기 때문에 이 작업 필요
                fragmentReviewBinding.arrow.setImageResource(0)
            }
        }
    }
}