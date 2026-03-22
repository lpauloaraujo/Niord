package com.example.niord

import android.util.Log
import android.view.WindowManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleObserver
import androidx.test.annotation.UiThreadTest
import org.junit.Assert.*

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith



@RunWith(AndroidJUnit4::class)
class OverlayInstrumentedTest {


    val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    var lifecycleOwner = FloatingLifecycleOwner()
    val events = mutableListOf<Lifecycle.Event>()

    val observer = LifecycleEventObserver { _, event -> events.add(event) }

    @Test
    fun lifeTest(){
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            lifecycleOwner.onCreate()
            //Need to add observer in Main
            lifecycleOwner.lifecycle.addObserver(observer)
            lifecycleOwner.onResume()
        }
    }

    @UiThreadTest
    @Test
    fun useAppContext() {
        assertEquals("com.example.niord", appContext.packageName)
    }

    @UiThreadTest
    @Test
    fun invoke_isFlaggedCorrectly() {
        val overlayManager: OverlayManager = OverlayManager(appContext, lifecycleOwner)
        overlayManager.invoke()
        //assert(overlayManager.isInvoked) //Can't add window in the test environment
        assert(overlayManager.isVisible)
        assert(overlayManager.isDraggable)
    }

    @UiThreadTest
    @Test
    fun isVisibleToggle() {
        val overlayManager: OverlayManager = OverlayManager(appContext, lifecycleOwner)
        overlayManager.setVisibility(false)
        assert(!overlayManager.isVisible)
        overlayManager.setVisibility(true)
        assert(overlayManager.isVisible)
    }

    @Test
    fun build_isCorrect(){
        val overlayManager: OverlayManager = object : OverlayManager(appContext, lifecycleOwner){

        }
    }

    @Test
    fun clickOverride(){
        var returnVal: Boolean = false
        val overlayManager: OverlayManager = object : OverlayManager(appContext, lifecycleOwner){
            override fun clickEvent() {

                returnVal = true
            }

            init {
                clickEvent()
            }
        }
        assert(returnVal)
    }
}