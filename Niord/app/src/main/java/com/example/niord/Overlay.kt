package com.example.niord

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.example.niord.ui.theme.NiordTheme

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

open class OverlayManager(private val context: Context){
    private var winManager: WindowManager? = null
    private var lifecycleOwner: FloatingLifecycleOwner? = null
    protected var floatingView: ComposeView? = null
    init {
        winManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = FloatingLifecycleOwner().apply {
            onCreate()
            onResume()
        }
        floatingView = buildView()
    }
    var isInvoked = false
    var isVisible = true

    var composable: @Composable ()->Unit = { DefaultComposable() }

    @RequiresApi(Build.VERSION_CODES.O)
    private val layoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        gravity = Gravity.CENTER
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
    }

    fun buildView(): ComposeView {
        return ComposeView(context.applicationContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setContent {
                NiordTheme {
                    composable()
                }
            }
        }
    }

    //Change the view composable for state change
    fun refreshView(view: ComposeView?, newComposable: @Composable ()->Unit = {DefaultComposable()}){
        view?.setContent {
            NiordTheme {
                newComposable()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun invoke(){
        if(isInvoked){return}

        //refreshView(floatingView)
        floatingView?.let { view ->
            try {
                winManager?.addView(view, layoutParams)
                isInvoked = true
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle errors (e.g., missing permission)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun dismiss(){
        if(!isInvoked){return}
        floatingView?.let { view ->
            try {
                winManager?.removeView(view)
                isInvoked = false
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle errors (e.g., missing permission)
            }
        }
    }

    fun setVisibility(state: Boolean){
        if(state){
            floatingView?.visibility = View.VISIBLE
            isVisible = true
        }else{
            floatingView?.visibility = View.INVISIBLE
            isVisible = false
        }
    }

    @Composable
    open fun DefaultComposable(){
        /*
        Override this function to have different Composables in an overlay
         */
        Floating(text = "Hello float")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onDestroy(){
        dismiss()
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
    }
}



class FloatingLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val controller = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle = lifecycleRegistry
    override val viewModelStore: ViewModelStore = store
    override val savedStateRegistry: SavedStateRegistry = controller.savedStateRegistry

    fun onCreate() {
        controller.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}


class ExampleCustomOverlay(context: Context) : OverlayManager(context){
    private var text = "DEFAULT TEXT"

    @Composable
    override fun DefaultComposable(){
        Column() {
            Floating(
                text = text
            )
            Button(onClick = {
                //View update Example
                text = "NEW UPDATED TEXT"
                refreshView(floatingView, {DefaultComposable()})
            }){
                Text("Change")
            }
        }
    }
}

@Composable
fun Floating(text: String){
    Button(onClick = {println("Overlay Button Clicked")}
    ){
        Text(
            text=text
        )
    }
}