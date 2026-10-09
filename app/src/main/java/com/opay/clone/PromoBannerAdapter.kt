package com.opay.clone

import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PromoBannerAdapter(private val images: List<Int>) : RecyclerView.Adapter<PromoBannerAdapter.VH>() {

    class VH(v: ImageView) : RecyclerView.ViewHolder(v)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val iv = ImageView(parent.context)
        iv.layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        iv.scaleType = ImageView.ScaleType.CENTER_CROP
        return VH(iv)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        (holder.itemView as ImageView).setImageResource(images[position])
    }

    override fun getItemCount() = images.size
}
