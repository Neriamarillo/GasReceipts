package com.jn769.gasreceipts

import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import com.jn769.gasreceipts.ListAdapter.MyViewHolder

/**
 * Created by Jorge Nieves on 12/1/18.
 */
private class ListAdapter(private val mDataset: Array<String>) : RecyclerView.Adapter<ListAdapter.MyViewHolder>() {

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    internal class MyViewHolder(// each data item is just a string in this case
            var mTextView: TextView) : RecyclerView.ViewHolder(mTextView)

    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): ListAdapter.MyViewHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.content_main, parent, false) as TextView

        return MyViewHolder(v)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        holder.mTextView.text = mDataset[position]

    }

    override fun getItemCount(): Int {
        return mDataset.size
    }
}
