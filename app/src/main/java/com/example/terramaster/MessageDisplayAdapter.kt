package com.example.terramaster

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import de.hdodenhof.circleimageview.CircleImageView
import com.squareup.picasso.Picasso

class MessageDisplayAdapter(
    context: Context,
    private val items: ArrayList<SearchItem>,
    private val listener: ClickListener
) : ArrayAdapter<SearchItem>(context, 0, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var currentView = convertView

        if (currentView == null) {
            currentView = LayoutInflater.from(context).inflate(R.layout.search_list, parent, false)
        }

        val currentItem = items[position]
        val itemImage: CircleImageView = currentView!!.findViewById(R.id.imageSearch)
        val itemName: TextView = currentView.findViewById(R.id.textName)

        // Check for valid image path
        val imageUrl = currentItem.imageSearch
        if (!imageUrl.isNullOrEmpty()) {
            Picasso.get().load(imageUrl).placeholder(R.drawable.profile) // Placeholder while loading
                .error(R.drawable.profile
                ) // Error image if loading fails
                .into(itemImage)
        } else {
            // Set a default image if the URL is null or empty
            itemImage.setImageResource(R.drawable.profile)
        }

        // Set item name
        itemName.text = currentItem.name

        // Set click listener
        currentView.setOnClickListener {
            listener.onItemClick(currentItem.userId, currentItem.name, currentItem.imageSearch)
        }

        return currentView
    }
}
