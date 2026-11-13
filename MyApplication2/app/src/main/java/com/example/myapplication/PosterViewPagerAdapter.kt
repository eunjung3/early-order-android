package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ItemPosterBinding

class PosterViewPagerAdapter : RecyclerView.Adapter<PosterViewPagerAdapter.PosterViewHolder>() {
    private var posters: List<PosterData> = emptyList()

    inner class PosterViewHolder(private val binding: ItemPosterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(posterUrl: String) {
            Glide.with(binding.root)
                .load(posterUrl)
                .centerCrop()
                .placeholder(R.color.loading_color)
                .error(R.color.error_color)
                .into(binding.ivPoster)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PosterViewHolder {
        return PosterViewHolder(
            ItemPosterBinding.inflate(
                LayoutInflater.from(parent.context), 
                parent, 
                false
            )
        )
    }

    override fun onBindViewHolder(holder: PosterViewHolder, position: Int) {
        holder.bind(posters[position].image_url)
    }

    override fun getItemCount() = posters.size

    fun submitList(newPosters: List<PosterData>) {
        posters = newPosters
        notifyDataSetChanged()
    }
} 