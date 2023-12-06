package org.newlogic.smartscanner.adapters

import android.R.attr.label
import android.R.attr.text
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.RecyclerView
import org.newlogic.smartscanner.R


class RecyclerResultAdapter(private var results: HashMap<String, String>) :
    RecyclerView.Adapter<RecyclerResultAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val container: View = itemView.findViewById(R.id.container)
        val itemLabel: TextView = itemView.findViewById(R.id.tv_label)
        val itemValue: TextView = itemView.findViewById(R.id.tv_value)

        init {
            itemValue.setOnClickListener {
                val key = it.tag.toString()
                val value = results.getValue(key)
                val clipboard: ClipboardManager =
                    it.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("key", value)
                clipboard.setPrimaryClip(clip)

                Toast.makeText(it.context, "$key value copied to clipboard", Toast.LENGTH_SHORT)
                    .show()
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_result_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val keyByIndex = results.keys.elementAt(position)
        val valueByKey = results.getValue(keyByIndex)
        holder.container.visibility = if (valueByKey.isEmpty()) View.GONE else View.VISIBLE
        holder.itemLabel.text = splitCamelCase(keyByIndex)
        holder.itemValue.text = valueByKey
        holder.itemValue.tag = keyByIndex
    }

    override fun getItemCount(): Int {
        return results.size
    }

    private fun splitCamelCase(camelCaseString: String): String {
        val regex = "(?<=\\p{Ll})(?=\\p{Lu})".toRegex()
        val array = regex.split(camelCaseString)
        var key = ""
        for (a in array) {
            key += "$a "
        }
        return key
    }
}