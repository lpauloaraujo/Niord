package com.example.niord

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
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
import kotlin.Pair
import kotlin.math.abs

open class OverlayManager(private val context: Context, var lifecycleOwner: FloatingLifecycleOwner,
    var defaultPos: Pair<Int, Int> = Pair(500, 0)
    ){
    var winManager: WindowManager? = null
    //private var lifecycleOwner: FloatingLifecycleOwner? = null
    var floatingView: ComposeView? = null

    val displayMetrics = DisplayMetrics()

    init {
        winManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        floatingView = buildView()
        winManager?.defaultDisplay?.getMetrics(displayMetrics)
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
        windowAnimations = 0
    }

    var isDraggable = true
    var dragPaddingDp: Float = 100f

    //Used for calculations and defines the current position thereafter
    private var lastPos = Pair(0, 0)
    private var firstPos = Pair(0, 0)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    var moved = false


    protected open fun clickEvent(){
        println("Click")
    }

    protected open fun moveEvent(delta: Pair<Int, Int>){
    }

    protected open fun downEvent(){

    }

    protected open fun upEvent(){

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
                    downEvent()
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

                        layoutParams.x = intervalLimit(0, layoutParams.x, width - pxValue)
                        layoutParams.y = intervalLimit(0, layoutParams.y, height - (pxValue*2))

                        winManager?.updateViewLayout(view, layoutParams)
                        moveEvent(deltaPos)
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if(!moved) {
                        clickEvent()
                        view.performClick()
                        upEvent()
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
    open fun onDestroy(){
        if(isInvoked){
            if(floatingView?.isAttachedToWindow == true){
                winManager?.removeViewImmediate(floatingView)
            }
        }
        floatingView?.disposeComposition()
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
class MainOverlayButton(var context: Context,
                        lifecycleOwner: FloatingLifecycleOwner,
                        localDefaultPos: Pair<Int, Int> = Pair(100, 100)) :
    OverlayManager(context, lifecycleOwner, defaultPos = localDefaultPos){

    private var additionalOverlay: OverlayManager
    lateinit var expandedOffset: Pair<Int, Int>
    lateinit var expandedOffsetH: Pair<Int, Int>
    var statePacket: StatePacket

    var minForHorizontal: Float = 0.50f

    init{
        statePacket = StatePacket()
        composable = {MainIcon()}
        floatingView?.layoutDirection = View.LAYOUT_DIRECTION_LTR

        dragPaddingDp = statePacket.iconSizeDp
        setOffset()

        additionalOverlay = OverlayManager(context, lifecycleOwner,
            defaultPos=Pair(
                defaultPos.first + expandedOffset.first,
                defaultPos.second + expandedOffset.second
                ))
        additionalOverlay.composable = {ComposableUnit(statePacket)}
        additionalOverlay.refreshView()
        additionalOverlay.floatingView?.layoutDirection = floatingView?.layoutDirection!!
        additionalOverlay.setVisibility(false)
        additionalOverlay.isDraggable = false
        additionalOverlay.invoke()
        //moveEvent(Pair(0,0))
    }

    fun setOffset(){
        expandedOffsetH = dpToPxPair(context,
            Pair(statePacket.iconSizeDp + statePacket.iconSpacingDp,
                //centralizes in the Y axis considering the difference in size
                statePacket.iconSizeDp*(1-statePacket.subIconScale)/2)
        )
        expandedOffset = dpToPxPair(context,
            Pair(statePacket.iconSizeDp*(1-statePacket.subIconScale)/2,
                statePacket.iconSizeDp + statePacket.iconSpacingDp)
        )
    }
    override fun onDestroy(){
        additionalOverlay.onDestroy()
        super.onDestroy()
    }
    class StatePacket{
        var addIsVisible by mutableStateOf(false)
        var isVertical by mutableStateOf(true)
        var isLtr by mutableStateOf(true)
        var iconSizeDp by mutableFloatStateOf(64f)
        var subIconScale by mutableFloatStateOf(0.75f)
        var iconSpacingDp by mutableFloatStateOf(8f)
    }


    override fun moveEvent(delta: Pair<Int, Int>) {
        if (statePacket.addIsVisible) {
            statePacket.addIsVisible = !statePacket.addIsVisible
            additionalOverlay.setVisibility(statePacket.addIsVisible)
        }
    }

    //Secondary overlay move logic
    override fun upEvent() {
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        statePacket.isLtr = layoutParams.x <= width/2
        statePacket.isVertical = layoutParams.y < height*minForHorizontal
        println(height)
        var invCorrection = 0
        //Offset inverts side
        if(!statePacket.isLtr){
            invCorrection = floatingView!!.width
        }

        if(statePacket.isVertical) {
            additionalOverlay.layoutParams.x = layoutParams.x + expandedOffset.first
            additionalOverlay.layoutParams.y = layoutParams.y + expandedOffset.second
        }else{
            additionalOverlay.layoutParams.x =
                layoutParams.x + (expandedOffsetH.first - invCorrection)
            additionalOverlay.layoutParams.y = layoutParams.y + expandedOffsetH.second
        }

        if(statePacket.isLtr || statePacket.isVertical){
            additionalOverlay.layoutParams.gravity = layoutParams.gravity
        }else{
            //Gravity makes the 0,0 point the Right-Top, necessary for manipulation
            additionalOverlay.layoutParams.gravity = Gravity.END + Gravity.TOP
            //Gravity.END inverts the X axis, mirror geometry needed
            //This subtracts the original position to the midpoint times 2 to get the distance
            //Then subtracts with the mirrored position to align both
            additionalOverlay.layoutParams.x -= (layoutParams.x - width/2) * 2
        }


        additionalOverlay.invoke()
        if (additionalOverlay.floatingView?.isAttachedToWindow == true) {
            try {
                additionalOverlay.winManager?.
                updateViewLayout(additionalOverlay.floatingView, additionalOverlay.layoutParams)
            } catch (e: IllegalArgumentException) {
                // Log it or handle the fact that the view was detached mid-execution
                Log.e("Overlay", "View detached during update: ${e.message}")
            }
        }


    }

    override fun clickEvent() {
        statePacket.addIsVisible = !statePacket.addIsVisible
        additionalOverlay.setVisibility(statePacket.addIsVisible)
    }


    @Composable
    fun IconBox(resource: Int, sizeDp: Float, enabled: Boolean = true, onClick: () -> Unit = {}){
        if(enabled) {
            Box (modifier = Modifier.requiredSize(sizeDp.dp), propagateMinConstraints = true){
                IconButton(onClick = onClick) {
                    Image(
                        modifier = Modifier.size(sizeDp.dp),
                        painter = painterResource(resource),
                        contentDescription = "Icon",
                    )
                }
            }
        } else{
            Image(
                painter = painterResource(resource),
                contentDescription = "Icon",
                modifier = Modifier.size(sizeDp.dp)
            )
        }

    }

    var secondaryButtonSize = statePacket.iconSizeDp * statePacket.subIconScale

    var onCallClick: ((String) -> Unit)? = null

    var additionalButtons: List<@Composable ()->Unit> = listOf(
        {IconBox(R.drawable.health, secondaryButtonSize, onClick = {onCallClick?.invoke("144")})},
        {IconBox(R.drawable.cops, secondaryButtonSize, onClick = {onCallClick?.invoke("1052")})},
        {IconBox(R.drawable.alert, secondaryButtonSize)},
        {IconBox(R.drawable.plt_vigia, secondaryButtonSize)},
        {IconBox(R.drawable.contacts, secondaryButtonSize)},
        {IconBox(R.drawable.insurance, secondaryButtonSize)}
    )

    @Composable
    fun MainIcon(){
        IconBox(R.drawable.main_button, statePacket.iconSizeDp, false)
    }

    @Composable
    fun ComposableUnit(statePacket: StatePacket){
        if (statePacket.isVertical) Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(statePacket.iconSpacingDp.dp)
        ){
            additionalButtons.forEach { it() }
        }
        if (!statePacket.isVertical)
            Row( verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(statePacket.iconSpacingDp.dp)){
                if(statePacket.isLtr) additionalButtons.forEach { it() }
                else additionalButtons.reversed().forEach { it() }
                //additionalButtons.forEach { it() }
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