package com.first.ridingpartnerinsiheung.scenarios.main.maps.routeSearchPage

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.first.ridingpartnerinsiheung.R
import com.first.ridingpartnerinsiheung.api.place.PlaceDetail

class PlaceAdapter(val context: Context, val places : List<PlaceDetail.Place>) : BaseAdapter() {
    override fun getView(position : Int, convertView: View?, parent : ViewGroup?): View {
        val view : View = LayoutInflater.from(context).inflate(R.layout.listview_place_item, null)

        val placeName = view.findViewById<TextView>(R.id.placeName)
        val placeAdr = view.findViewById<TextView>(R.id.placeAdr)
        val placeType = view.findViewById<TextView>(R.id.placeType)

        val place = places[position]
        placeName.text = place.title
        placeAdr.text = place.roadAddress
        placeType.text = place.ctg

        return view
    }
    override fun getCount(): Int {
        return places.size
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getItem(position : Int): Any {
        return places[position]
    }
}