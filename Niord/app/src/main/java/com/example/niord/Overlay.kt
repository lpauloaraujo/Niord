package com.example.niord

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.service.chooser.AdditionalContentContract
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BrushPainter
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
import com.example.niord.ui.theme.NiordTheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

open class OverlayManager(private val context: Context, var lifecycleOwner: FloatingLifecycleOwner,
    var defaultPos: Pair<Int, Int> = Pair(500, 0)
    ){
    var winManager: WindowManager? = null
    //private var lifecycleOwner: FloatingLifecycleOwner? = null
    var floatingView: ComposeView? = null

    init {
        winManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = buildView()
    }
    var isInvoked = false
    var isVisible = true
    var composable: @Composable ()->Unit = { DefaultComposable() }


    //Starting X,Y coordinates
    //var defaultPos = Pair(500, 0)
    @RequiresApi(Build.VERSION_CODES.O)
    val layoutParams = WindowManager.LayoutParams().apply {
        format = PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE + WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        gravity = Gravity.START + Gravity.TOP
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        x = defaultPos.first
        y = defaultPos.second
    }

    var isDraggable = true
    var dragPaddingDp: Float = 100f

    //Used for calculations and defines the current position thereafter
    private var lastPos = Pair(0, 0)
    private var firstPos = Pair(0, 0)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var moved = false
    val displayMetrics = DisplayMetrics()

    protected open fun clickEvent(){
        println("Click")
    }

    protected open fun moveEvent(delta: Pair<Int, Int>){
    }


    @RequiresApi(Build.VERSION_CODES.O)
    fun buildOnTouchListener(): View.OnTouchListener {
        /*
         The default Button composable consumes the touch events
         Making it inconvenient to use them in a draggable view
         */

        return View.OnTouchListener{view, event ->
            val action = event.action
            var deltaPos: Pair<Int, Int>
            var totalDeltaPos: Pair<Int, Int>

            when(action){
                MotionEvent.ACTION_DOWN -> {
                    lastPos = Pair(event.rawX.toInt(), event.rawY.toInt())
                    firstPos = Pair(event.rawX.toInt(), event.rawY.toInt())
                    moved = false
                }

                MotionEvent.ACTION_MOVE -> {
                    val x = event.rawX.toInt()
                    val y = event.rawY.toInt()
                    deltaPos = Pair(
                        x - lastPos.first,
                        y - lastPos.second
                    )
                    totalDeltaPos = Pair(
                        x - firstPos.first,
                        y - firstPos.second
                    )
                    lastPos = Pair(x, y)
                    //Guarantees minimum distance from start
                    if (abs(totalDeltaPos.first) > touchSlop || abs(totalDeltaPos.second) > touchSlop) {
                        moved = true
                    }
                    if(isDraggable && moved) {
                        //Moves based on last "tick"
                        //TO-DO Restrain the Pos Values to the screen dimensions
                        layoutParams.x += deltaPos.first
                        layoutParams.y += deltaPos.second


                        winManager?.defaultDisplay?.getMetrics(displayMetrics)

                        val width = displayMetrics.widthPixels
                        val height = displayMetrics.heightPixels
                        val pxValue = dpToPx(context,dragPaddingDp)

                        println(pxValue)
                        layoutParams.x = intervalLimit(0, layoutParams.x, width - pxValue)
                        layoutParams.y = intervalLimit(0, layoutParams.y, height - pxValue)

                        winManager?.updateViewLayout(view, layoutParams)
                        moveEvent(deltaPos)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if(!moved) {
                        clickEvent()
                        view.performClick()
                    }
                }


            }
            moved
        }
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
    fun refreshView(newComposable: @Composable ()->Unit = composable){
        floatingView?.setContent {
            NiordTheme {
                newComposable()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun invoke(){
        if(isInvoked){return}
        floatingView?.setOnTouchListener(buildOnTouchListener())
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

    open fun setVisibility(state: Boolean){
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
        //lifecycleOwner.onDestroy()
        //lifecycleOwner = null
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

@RequiresApi(Build.VERSION_CODES.O)
class MainOverlayButton(context: Context, lifecycleOwner: FloatingLifecycleOwner) :
    OverlayManager(context, lifecycleOwner, defaultPos = Pair(0, 0)){
    init{
        composable = {FullComposable()}
        floatingView?.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }

    class StatePacket{
        var addIsVisible by mutableStateOf(false)
        var isVertical by mutableStateOf(false)
        var isLtr by mutableStateOf(true)
    }
    var statePacket = StatePacket()

    override fun moveEvent(delta: Pair<Int, Int>) {
        //Use layoutParams to get position to decide Row or Column
    }


    override fun clickEvent() {
        statePacket.addIsVisible = !statePacket.addIsVisible
    }

    //ltr == Left to Right
    fun setDirection(ltr: Boolean){
        statePacket.isLtr = ltr
        if (!ltr) floatingView?.layoutDirection = View.LAYOUT_DIRECTION_RTL
        else floatingView?.layoutDirection = View.LAYOUT_DIRECTION_LTR
    }

    var additionalButtons: List<@Composable ()->Unit> = listOf(
        {Floating("MOCK")},
        {Floating("MOCK")}
        )

    @Composable
    fun MainIcon(){
        Icon(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "Icon"
        )
    }

    @Composable
    fun FullButtonList(addIsVisible: Boolean){
        val fullList = remember(addIsVisible) {
            val list: MutableList<@Composable () -> Unit> = mutableListOf(
                { MainIcon() }
            )
            if (addIsVisible) list.addAll(additionalButtons)
            list
        }
        fullList.forEach { it() }
    }


    @Composable
    fun FullComposable(){
        ComposableUnit(
            statePacket.addIsVisible,
            statePacket.isVertical
        )
    }

    @Composable
    fun ComposableUnit(addIsVisible: Boolean, isVertical: Boolean){
        if (isVertical) Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            FullButtonList(addIsVisible)
        }
        if (!isVertical)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FullButtonList(addIsVisible)
            }

    }

}


//Use this class as an example of use case
class ExampleCustomOverlay(context: Context, lifecycleOwner: FloatingLifecycleOwner) : OverlayManager(context, lifecycleOwner){
    //It's possible to have the mutable variables inside the composable
    //But changing the variables is limited by functions only inside the composable
    class StatePacket{
        var text by mutableStateOf("DEFAULT TEXT")
    }
    var statePacket = StatePacket()

    fun exampleOutsideChange(){
        statePacket.text = "EXAMPLE CHANGE"
    }

    @Composable
    override fun DefaultComposable(){
        Column{
            Floating(
                text = statePacket.text
            )
            Button(
                onClick = {
                //View update Example
                statePacket.text = "NEW UPDATED TEXT"
            }
            ){
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