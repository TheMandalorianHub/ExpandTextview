package com.test.expandtextview

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {

    var array= arrayOf("1","2","3","4","5","6","7","8","9","0","a","b","c","d","e","f","t","g","q","w","e","p","l","k","i","n","m","G","H","J","I","L","C","V","B"
        ,"你","我","他","天","地","动","进","啊","去","改","酒","一","会","年","收","好","嗯","这","有","\r","\n","\r\n","\t"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var str1 = generateStr(120)
        var str2 = generateStr(150)

        tv1_ori.setText(str1)
        tv1_after.currentText = str1
        tv1_after.clickListener=object :ExpandTextView.ClickListener{
            override fun onContentTextClick() {
                str1 = generateStr(120)
                log.d("主内容点击")
                tv1_ori.setText(str1)
                tv1_after.currentText = str1
            }

            override fun onSpecialTextClick(currentExpand: Boolean) {
                log.d("展开/收起  内容被点击")
                tv1_after.isExpand = !currentExpand
            }

        }


        tv2_ori.setText(str2)
        tv2_after.currentText=str2
        tv2_after.clickListener=object :ExpandTextView.ClickListener{
            override fun onContentTextClick() {
                log.d("主内容点击")
                str2 = generateStr(150)
                log.d("主内容点击")
                tv2_ori.setText(str2)
                tv2_after.currentText = str2
            }

            override fun onSpecialTextClick(currentExpand: Boolean) {
                log.d("展开/收起  内容被点击")
                tv2_after.isExpand = !currentExpand
            }

        }


        btn.setOnClickListener {
            startActivity(Intent(this@MainActivity,ListActivity::class.java))
        }
    }

    fun generateStr(count:Int):String {
        var length = array.size
        var stringBuffer = StringBuffer()
        var random=Random()
        for (i in 0 until count) {
            stringBuffer.append(array[random.nextInt(length)])
        }
        return stringBuffer.toString()
    }
}
