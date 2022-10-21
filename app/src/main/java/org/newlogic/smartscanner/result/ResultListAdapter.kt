package org.newlogic.smartscanner.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONArray
import org.json.JSONObject
import org.newlogic.smartscanner.databinding.RowResultBinding

class ResultListAdapter(private var results: JSONArray, private var keys: List<String>) :
    RecyclerView.Adapter<ResultListAdapter.ViewHolder?>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val itemBinding = RowResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return results.length()
    }

    inner class ViewHolder(val binding: RowResultBinding) : RecyclerView.ViewHolder(binding.root) {
        internal fun bind(position: Int) {
            val key = keys[position]
            val result = results[position] as JSONObject

            binding.txtKey.text = key
            binding.txtValue.text = result.optString(key)

        }
    }
}