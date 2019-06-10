package com.doordash.doordashlite

import android.app.Application
import android.content.Context
import androidx.annotation.VisibleForTesting
import com.doordash.doordashlite.api.DoorDashLiteApi
import com.doordash.doordashlite.repository.Repository
import com.doordash.doordashlite.repository.inMemory.byPage.InMemoryByPageKeyRepository
import com.doordash.doordashlite.util.Location
import com.doordash.doordashlite.util.LocationHelper
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * service locator implementation to allow us to replace default implementations
 * for testing.
 */
interface ServiceLocator {
    companion object {
        private val LOCK = Any()
        private var instance: ServiceLocator? = null
        fun instance(context: Context): ServiceLocator {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = DefaultServiceLocator(
                        app = context.applicationContext as Application
                    )
                }
                return instance!!
            }
        }

        /**
         * Allows tests to replace the default implementations.
         */
        @VisibleForTesting
        fun swap(locator: ServiceLocator) {
            instance = locator
        }
    }

    fun getRepository(type: Repository.Type): Repository

    fun getNetworkExecutor(): Executor

    fun getDoorDashLiteApi(): DoorDashLiteApi

    fun getLocation(): Location
}

/**
 * default implementation of ServiceLocator.
 */
open class DefaultServiceLocator(val app: Application) : ServiceLocator {
    // thread pool used for network requests
    @Suppress("PrivatePropertyName")
    private val NETWORK_IO = Executors.newFixedThreadPool(5)

    private val api by lazy {
        DoorDashLiteApi.create()
    }

    private val loc by lazy {
        LocationHelper.getLocation()
    }

    override fun getRepository(type: Repository.Type): Repository {
        return when (type) {
            Repository.Type.IN_MEMORY_BY_PAGE -> InMemoryByPageKeyRepository(
                location = getLocation(),
                doorDashLiteApi = getDoorDashLiteApi(),
                networkExecutor = getNetworkExecutor()
            )
            //else -> throw AssertionError()
        }
    }

    override fun getNetworkExecutor(): Executor = NETWORK_IO

    override fun getDoorDashLiteApi(): DoorDashLiteApi = api

    override fun getLocation(): Location = loc
}