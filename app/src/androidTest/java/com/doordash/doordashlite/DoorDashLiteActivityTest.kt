package com.doordash.doordashlite

import android.app.Application
import android.content.Intent
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.test.InstrumentationRegistry
import androidx.recyclerview.widget.RecyclerView
import com.doordash.doordashlite.api.DoorDashLiteApi
import com.doordash.doordashlite.repository.Repository
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Test to ensure data is displayed
 */
@RunWith(Parameterized::class)
class DoorDashLiteActivityTest(private val type: Repository.Type) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun params() = Repository.Type.values()
    }

    @get:Rule
    var testRule = CountingTaskExecutorRule()

    private val restaurantFactory = RestaurantFactory()
    @Before
    fun init() {
        val testApi = TestDoorDashLiteApi()
        testApi.addRestaurant(restaurantFactory.createRestaurant())
        testApi.addRestaurant(restaurantFactory.createRestaurant())
        testApi.addRestaurant(restaurantFactory.createRestaurant())
        val app = InstrumentationRegistry.getTargetContext().applicationContext as Application
        // use a controlled service locator w/ test API
        ServiceLocator.swap(
            object : DefaultServiceLocator(app = app) {
                override fun getDoorDashLiteApi(): DoorDashLiteApi = testApi
            }
        )
    }

    @Test
    @Throws(InterruptedException::class, TimeoutException::class)
    fun showSomeResults() {
        val intent = Intent(InstrumentationRegistry.getTargetContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent)
        val recyclerView = activity.findViewById<RecyclerView>(R.id.list)
        System.out.println("recyclerView: $recyclerView")
        assertThat(recyclerView.adapter, notNullValue())
        waitForAdapterChange(recyclerView)
        System.out.println("Adapter count: " + recyclerView.adapter?.itemCount)
        assertThat(recyclerView.adapter?.itemCount, `is`(3))
    }

    private fun waitForAdapterChange(recyclerView: RecyclerView) {
        val latch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            recyclerView.adapter?.registerAdapterDataObserver(
                object : RecyclerView.AdapterDataObserver() {
                    override fun onChanged() {
                        latch.countDown()
                    }

                    override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                        latch.countDown()
                    }
                })
        }
        testRule.drainTasks(1, TimeUnit.SECONDS)
//        if (recyclerView.adapter?.itemCount ?: 0 > 0) {
//            return
//        }
        assertThat(latch.await(10, TimeUnit.SECONDS), `is`(true))
    }
}