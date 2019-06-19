package com.doordash.doordashlite

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.Observer

class MainActivity : BaseActivity() {
    private var dualPane: Boolean = false
    private var currentId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
        setContentView(R.layout.activity_main)

        // Check to see if we have a frame in which to embed the details
        // fragment directly in the containing UI.
        val detailsFrame: View? = findViewById(R.id.details)
        dualPane = detailsFrame?.visibility == View.VISIBLE

        if (dualPane) {
            // Make sure our UI is in the correct state.
            showDetails(-1)
        }
    }

    private fun init() {
        viewModel.showDetails.observe(this, Observer { id ->
            if (id >= 0) {
                showDetails(id)
                // Clear value once showDetails is called
                viewModel.showDetails.postValue(-1)
            }
        })
    }

    /**
     * Helper function to show the details of a selected item, either by
     * displaying a fragment in-place in the current UI, or starting a
     * whole new activity in which it is displayed.
     */
    private fun showDetails(id: Int) {
        currentId = id

        if (dualPane) {
            // Check what fragment is currently shown, replace if needed.
            var details = supportFragmentManager?.findFragmentById(R.id.details) as? DetailsFragment
            if (details == null) {
                // Make new fragment to show this selection.
                details = DetailsFragment.newInstance(id)
                // Execute a transaction, replacing any existing fragment
                // with this one inside the frame.
                supportFragmentManager?.beginTransaction()?.apply {
                    replace(R.id.details, details)
                    setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                    commit()
                }
                details?.refresh(id)
            } else if (details?.shownId != id) {
                details?.refresh(id)
            }
        } else {
            // Otherwise we need to launch a new activity to display
            // the dialog fragment with selected text.
            val intent = Intent().apply {
                setClass(this@MainActivity, DetailsActivity::class.java)
                putExtra("id", currentId)
            }
            startActivity(intent)
        }
    }
}
