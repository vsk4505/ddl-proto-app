package com.doordash.doordashlite

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.doordash.doordashlite.glide.GlideApp
import com.doordash.doordashlite.glide.GlideRequests
import com.doordash.doordashlite.repository.ui.SharedViewModel
import kotlinx.android.synthetic.main.fragment_details.*

class DetailsFragment : Fragment() {

    private var progressBar: ProgressBar? = null

    val shownId: Int by lazy {
        arguments?.getInt(EXTRAS_ID, -1) ?: -1
    }

    var glide: GlideRequests? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glide = GlideApp.with(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_details, container, false)
        progressBar = view.findViewById(R.id.progress_bar)
        return view;
    }

    override fun onDestroyView() {
        progressBar = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val model = getViewModel()
        model?.details.observe(this, Observer { details ->
            details_title.text = details?.name ?: ""
            details_status.text = details?.status ?: ""
            if (details?.coverImageUrl?.startsWith("http") == true) {
                details_thumbnail.visibility = View.VISIBLE
                glide?.load(details.coverImageUrl)
                    ?.fitCenter()
                    ?.placeholder(R.drawable.ic_insert_photo_black_48dp)
                    ?.into(details_thumbnail)
            } else {
                details_thumbnail.visibility = View.GONE
                glide?.clear(details_thumbnail)
            }
            hideProgress()
        })
        model?.detailsError.observe(this, Observer { errorMsg ->
            hideProgress()
            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
        })

        if (shownId != -1) {
            refresh(shownId)
        } else {
            details_title.text = ""
            details_status.text = getString(R.string.DetailsMessage)
        }
    }

    private fun getViewModel(): SharedViewModel {
        return ViewModelProviders.of(activity!!).get(SharedViewModel::class.java)
    }

    fun refresh(id: Int) {
        if (activity?.isFinishing == false ) {
            showProgress()
            getViewModel().getDetails(id)
        }
    }

    private fun showProgress() {
        progressBar?.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        progressBar?.visibility = View.GONE
    }

    companion object {
        const val EXTRAS_ID: String = "id"
        /**
         * Create a new instance of DetailsFragment, initialized to
         * show the details by 'id'.
         */
        fun newInstance(id: Int): DetailsFragment {
            val f = DetailsFragment()

            // Supply index input as an argument.
            val args = Bundle()
            args.putInt(EXTRAS_ID, id)
            f.arguments = args

            return f
        }
    }
}
