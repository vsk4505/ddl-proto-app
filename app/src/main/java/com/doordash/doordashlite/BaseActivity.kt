package com.doordash.doordashlite

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.doordash.doordashlite.repository.Repository
import com.doordash.doordashlite.repository.ui.SharedViewModel

open class BaseActivity : AppCompatActivity() {
    protected lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getSharedViewModel()
    }

    protected fun getSharedViewModel(): SharedViewModel {
        return ViewModelProviders.of(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                val repoType = Repository.Type.values()[Repository.Type.IN_MEMORY_BY_PAGE.ordinal]
                val repo = ServiceLocator.instance(applicationContext)
                    .getRepository(repoType)
                return SharedViewModel(repo) as T
            }
        })[SharedViewModel::class.java]
    }
}
