package com.example.terramaster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.squareup.picasso.Picasso
import de.hdodenhof.circleimageview.CircleImageView

class RecentSearchAdapter(context: Context, private val items: ArrayList<SearchItem>) :
    ArrayAdapter<SearchItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView

        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.search_list, parent, false)
        }

        val item = getItem(position)

        if (item != null) {
            val searchImage = view!!.findViewById<CircleImageView>(R.id.imageSearch)
            val searchName = view.findViewById<TextView>(R.id.textName)

            searchName.text = item.name

            if (item.imageSearch.isNotEmpty()) {
                Picasso.get().load(item.imageSearch).into(searchImage)
            } else {
                searchImage.setImageResource(R.drawable.circle_background)
            }
        }


        return view!!
    }
}