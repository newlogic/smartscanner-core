package org.newlogic.smartscanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import org.newlogic.smartscanner.R

class RecyclerResultAdapter (private var results: HashMap<String, String>): RecyclerView.Adapter<RecyclerResultAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val itemLabel: TextView = itemView.findViewById(R.id.tv_label)
        val itemValue: TextView = itemView.findViewById(R.id.tv_value)

        // no click listener here
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.recycler_result_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val keyByIndex = results.keys.elementAt(position)
        val valueByKey = results.getValue(keyByIndex)
        holder.itemLabel.text = keyByIndex
        holder.itemValue.text = valueByKey
    }

    override fun getItemCount(): Int {
        return results.size
    }
}