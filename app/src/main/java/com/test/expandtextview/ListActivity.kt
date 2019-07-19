package com.test.expandtextview

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_list.*
import java.util.*
import kotlin.collections.ArrayList

class ListActivity : AppCompatActivity() {

    lateinit var adapter: Adapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        var list = generateList(160)

        adapter = Adapter(list)
        rv.layoutManager=LinearLayoutManager(this,LinearLayoutManager.VERTICAL,false)
        rv.adapter=adapter
    }

    private fun generateList(size:Int): ArrayList<String> {
        var list = arrayListOf<String>()
        var random= Random()
        for (i in 0 until size) {

            list.add(generateStr(random.nextInt(size*3)))
        }
        return list

    }
    fun generateStr(count:Int):String {
        var length = array.size
        var stringBuffer = StringBuffer()
        var random= Random()
        for (i in 0 until count) {
            stringBuffer.append(array[random.nextInt(length)])
        }
        return stringBuffer.toString()
    }
    var array= arrayOf("1","2","3","4","5","6","7","8","9","0","a","b","c","d","e","f","t","g","q","w","e","p","l","k","i","n","m","G","H","J","I","L","C","V","B"
        ,"你","我","他","天","地","动","进","啊","去","改","酒","一","会","年","收","好","嗯","这","有","\r","\n","\r\n","\t"
    )
    inner class Adapter : RecyclerView.Adapter<Adapter.Holder> {
        var list = arrayListOf<String>()
        var sparseArray = SparseArray<Int>()

        constructor(list: ArrayList<String>) : super() {
            this.list = list
        }


        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): Holder {
            var view = layoutInflater.inflate(R.layout.item_list, p0, false)
            var holder = Holder(view)
            return holder

        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: Holder, p1: Int) {
            var s = list.get(holder.adapterPosition)
            holder.tvOri.text = "${holder.adapterPosition}->原文:\r\n$s"
            holder.tvAfter.currentText = s
            if (sparseArray.indexOfKey(holder.adapterPosition) != -1) {
                val get = sparseArray.get(holder.adapterPosition)
                var target=if (get==0) true else false
                if (target != holder.tvAfter.isExpand) {
                    holder.tvAfter.isExpand=target
                }
            }

            holder.tvAfter.clickListener = object : ExpandTextView.ClickListener {
                override fun onContentTextClick() {


                }

                override fun onSpecialTextClick(currentExpand: Boolean) {
                    holder.tvAfter.isExpand = !currentExpand
                    //需要保存状态，下次滑动回来后再恢复状态
                    sparseArray.put(holder.adapterPosition, if (holder.tvAfter.isExpand) 0 else 1)
                }

            }
            if (holder.adapterPosition % 2 == 0) {
                holder.iv.setBackgroundResource(R.drawable.ic_launcher_background)
            } else {
                holder.iv.setBackgroundResource(R.drawable.ic_launcher_foreground)
            }
        }

        inner class Holder : RecyclerView.ViewHolder {
            var tvOri: TextView
            var tvAfter: ExpandTextView
            var iv: ImageView

            constructor(view: View) : super(view) {
                tvOri = view.findViewById(R.id.tv_ori)
                tvAfter = view.findViewById(R.id.tv_after)
                iv = view.findViewById(R.id.iv)
            }

        }

    }
}
