package com.ominext.demowm

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_custom_view.*

class CustomViewActivity : AppCompatActivity() {

    private val listMember = mutableListOf(1, 2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_view)
        rvMember
                .apply {
                    adapter = MemberAdapter()
                    layoutManager = LinearLayoutManager(context)
                }
        weekView.rvMember = rvMember
    }


    inner class MemberAdapter : RecyclerView.Adapter<MemberViewHolder>() {
        override fun onBindViewHolder(p0: MemberViewHolder, p1: Int) {
            p0.bindData()
        }

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): MemberViewHolder {
            val view = LayoutInflater.from(p0.context).inflate(R.layout.item_day_calendar_member, p0, false)
            return MemberViewHolder(view)
        }

        override fun getItemCount(): Int {
            return 10
        }

    }

    inner class MemberViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private var imgAvatar: ImageView = itemView.findViewById(R.id.img_avatar)
        private var btnCancel: ImageView = itemView.findViewById(R.id.btn_cancel)
        private var tvName: TextView = itemView.findViewById(R.id.tv_name)

        @SuppressLint("SetTextI18n")
        fun bindData() {
            if (adapterPosition < listMember.size) {
                imgAvatar.visibility = View.VISIBLE
                if (adapterPosition == 0) {
                    btnCancel.visibility = View.GONE
                } else {
                    btnCancel.visibility = View.VISIBLE
                }

                tvName.text = "Name ${listMember[adapterPosition]}"
            } else {
                imgAvatar.visibility = View.GONE
                btnCancel.visibility = View.GONE
                tvName.visibility = View.GONE
            }
        }

    }
}
