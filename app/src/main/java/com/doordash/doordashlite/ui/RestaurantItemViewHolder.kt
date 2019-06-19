package com.doordash.doordashlite.repository.ui

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.doordash.doordashlite.R
import com.doordash.doordashlite.glide.GlideRequests
import com.doordash.doordashlite.model.Restaurant
import kotlinx.android.synthetic.main.restaurant_item.view.*

/**
 * A RecyclerView ViewHolder that displays restaurant item.
 */
class RestaurantItemViewHolder(
    view: View,
    private val glide: GlideRequests,
    private val adapterItemCallback: RestaurantsPagedListAdapter.AdapterCallback?
) : RecyclerView.ViewHolder(view) {
    private val title: TextView = view.findViewById(R.id.title)
    private val subtitle: TextView = view.findViewById(R.id.subtitle)
    private val status: TextView = view.findViewById(R.id.time_status)
    private val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
    private val like: Button = view.findViewById(R.id.like)
    private var post: Restaurant? = null

    init {
        view.setOnClickListener {
            adapterItemCallback?.onItemClicked(post?.id ?: -1)
        }
        like.setOnClickListener {
            adapterItemCallback?.onLikeClicked(adapterPosition, post?.id ?: -1)
        }
    }

    fun bind(post: Restaurant?) {
        this.post = post
        title.text = post?.name ?: "loading"
        subtitle.text = post?.description ?: "loading"
        status.text = if (post?.statusType == "open") post?.status else post?.statusType
        like.text =
            if (adapterItemCallback?.isLiked(post?.id) == true) {
                itemView.context.getString(R.string.Liked)
            } else {
                itemView.context.getString(R.string.Like)
            }
        if (post?.coverImageUrl?.startsWith("http") == true) {
            thumbnail.visibility = View.VISIBLE
            glide.load(post?.coverImageUrl)
                .fitCenter()
                .placeholder(R.drawable.ic_insert_photo_black_48dp)
                .into(thumbnail)
        } else {
            thumbnail.visibility = View.GONE
            glide.clear(thumbnail)
        }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            glide: GlideRequests,
            adapterCallback: RestaurantsPagedListAdapter.AdapterCallback?
        ): RestaurantItemViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.restaurant_item, parent, false)
            return RestaurantItemViewHolder(view, glide, adapterCallback)
        }
    }

    fun updateScore(item: Restaurant?) {
        post = item
        status.text = if (post?.statusType == "open") post?.status else post?.statusType
    }
}