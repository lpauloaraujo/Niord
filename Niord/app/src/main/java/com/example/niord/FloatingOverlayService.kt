package com.example.niord
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.lifecycle.LifecycleService
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import com.example.niord.api.ApiService
import com.example.niord.api.HelpAnswer
import com.example.niord.api.HelpAnswerMulti
import com.example.niord.api.HelpAsk
import com.example.niord.api.HelpReceive
import com.example.niord.api.LocationSchema
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList

class FloatingOverlayService : LifecycleService() {

    val themedContext = ContextThemeWrapper(this, R.style.Theme_Niord)
    private var callMonitor: CallMonitor? = null

    private lateinit var locationManager: LocationManager

    private lateinit var buttonOverlay: MainOverlayButton

    val permission = PermissionChecker(this)

    private lateinit var apiService: ApiService

    private val receivedIds = CopyOnWriteArrayList<Int>()

    private var alertState = false

    inner class LocalBinder : Binder() {
        fun getService(): FloatingOverlayService = this@FloatingOverlayService
    }
    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        locationManager = LocationManager(this)
        apiService = ApiService(this)

        startForeground(1, createNotification())

        val buttonPos = UserFlowPreferences.getOverlayPos(this)

        buttonOverlay = MainOverlayButton(context = this, buttonPos)
        buttonOverlayInit()
        initOverwatch()

        buttonOverlay.invoke()
    }

    override fun onDestroy() {

        buttonOverlay.onDestroy()
        super.onDestroy()
    }

    private fun buttonOverlayInit() {
        buttonOverlay.isDraggable = !UserFlowPreferences.isOverlayLocked(this)
        buttonOverlay.statePacket.vigiaActive = VigiaService.isRunning
        buttonOverlay.setVisibility(true)

        buttonOverlay.onCallClick = { number ->
            if(permission.isCallPermitted()) {
                showCallDialog(number)
            }
        }

        buttonOverlay.onVigiaClick = { isActive ->
            if(permission.isVigiaPermitted()) {
                showVigiaDialog(isActive)
            }
        }

        buttonOverlay.onLocationClick = {
            if(permission.isSmsPermitted() and permission.isLocationPermitted()) {
                showSendLocationThroughSMSDialog()
            }
        }

        buttonOverlay.onAlertClick ={
            showAlertDialog()
        }
    }

    fun refresh(){
        buttonOverlay.applyStatePacketPreferences()
    }

    fun initOverwatch(){
        if(permission.isLocationPermitted()) {
            lifecycleScope.launch {

                apiService.connectWsOverwatch(
                    { frame ->
                        val data = Json.decodeFromString<HelpReceive>(frame.readText())
                        when(data.type) {
                            "accept" -> receiveHelpAnswer(data)
                            "deny" -> receiveHelpAnswer(data)
                        }

                    },
                    suspend {
                        var sendFrame: Frame? = null
                        val loc: Location? =
                            locationManager.fetchLocationRet(Priority.PRIORITY_LOW_POWER)
                        if(loc != null) {
                            val data = LocationSchema(
                                loc.latitude,
                                loc.longitude
                            )
                            sendFrame = Frame.Text(Json.encodeToString(data))
                        }
                        sendFrame
                    }
                )
            }
        }
    }

    private fun receiveHelpAnswer(data: HelpReceive){
        if(!alertState) return

        if(receivedIds.contains(data.userId)){
            if(data.type == "deny") {
                receivedIds.remove(data.userId)
            }else{
                receivedIds.clear()
                sendAnswerRequest(type = "acknowledge", targetId = data.userId)
                alertState = false
                showAlertHelpArrived()
            }
        }else if(data.type == "accept"){
            sendAnswerRequest(type = "acknowledge", targetId = data.userId)
            receivedIds.add(data.userId)
        }
    }
    private fun sendHelpRequest(type: String){
        locationManager.getUserLocation { loc ->
            if (loc != null) {
                val data = HelpAsk(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    type = type
                )
                lifecycleScope.launch {
                    val response = apiService.askHelp(data)
                    if((response.status.value == 200) and (type == "accident")){
                        alertState = true
                    }
                }
            }

        }
    }

    private fun sendAnswerRequest(type: String, targetId: Int){
        locationManager.getUserLocation { loc ->
            if (loc != null) {
                val data = HelpAnswer(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    type = type,
                    targetId = targetId
                )
                lifecycleScope.launch {
                    apiService.answerHelp(data)
                }
            }

        }
    }

    private fun alertCancel(){
        alertState = false
        if(receivedIds.isNotEmpty()){
            locationManager.getUserLocation { loc ->
                if (loc != null) {
                    val data = HelpAnswerMulti(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        type = "cancel",
                        targetIds = receivedIds.toList()
                    )
                    lifecycleScope.launch {
                        apiService.answerHelpMulti(data)
                    }
                }

            }
        }
    }


    private fun showAlertHelpArrived(){
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Um motorista chegou ao seu local")
            .setMessage(
                "Um motorista que estava a seu caminho chegou."
            )
            .setPositiveButton("Ok") { _, _ -> {}}
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showAlertDialog(){
        if(alertState){
            showAlertDialogTrack()
        }else{
            showAlertDialogAsk()
        }
    }

    private fun showAlertDialogTrack(){
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Cancelar alerta?")
            .setMessage(
                "Há ${receivedIds.size} motorista(s) a seu caminho.\n" +
                        "Motoristas a caminho serão notificados."
            )
            .setPositiveButton("Cancelar Alerta") { _, _ -> alertCancel() }
            .setNegativeButton("Continuar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showAlertDialogAsk(){
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Alertar usuários próximos?")
            .setMessage(
                "Motoristas próximos serão notificados com o seu pedido de alerta."
            )
            .setPositiveButton("Enviar Alerta") { _, _ -> showAlertDialogChoice() }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showAlertDialogChoice(){
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Qual o tipo de alerta?")
            .setMessage(
                "O tipo de alerta será mostrado para os motoristas próximos."
            )
            .setPositiveButton("Assalto") { _, _ -> sendHelpRequest("robbery") }
            .setNegativeButton("Acidente") { _, _ -> sendHelpRequest("accident") }
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showVigiaDialog(isActive: Boolean) {
        if (isActive) {
            showVigiaDeactivateDialog()
        } else {
            showVigiaActivateDialog()
        }
    }

    private fun showVigiaActivateDialog() {
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Ativar Niord Vigia?")
            .setMessage(
                "O app vai monitorar o áudio do seu aparelho em segundo plano para identificar " +
                        "ameaças, brigas ou comportamentos perigosos."
            )
            .setPositiveButton("Ativar Proteção") { _, _ -> startVigia() }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showVigiaDeactivateDialog() {
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Desativar Niord Vigia?")
            .setMessage("O monitoramento de áudio em segundo plano será encerrado.")
            .setPositiveButton("Desativar") { _, _ -> stopVigia() }
            .setNegativeButton("Cancelar", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showVigiaActivatedDialog() {
        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Niord Vigia Ativado")
            .setMessage("O monitoramento de áudio está rodando em segundo plano.")
            .setPositiveButton("Entendi", null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun startVigia() {
        startVigiaService()
    }

    private fun startVigiaService() {
        VigiaService.start(this)
        UserFlowPreferences.setVigiaActive(this, true)
        buttonOverlay.statePacket.vigiaActive = true
        showVigiaActivatedDialog()
    }

    private fun stopVigia() {
        VigiaService.stop(this)
        UserFlowPreferences.setVigiaActive(this, false)
        buttonOverlay.statePacket.vigiaActive = false
    }

    private fun showCallDialog(number: String) {

        val title: String
        val message: String
        val positiveText: String
        val negativeText: String

        when (number) {

            "144" -> {
                title = "Ligar para Emergência?"
                message = "Você será direcionado para a chamada telefônica. Confirme para discar imediatamente."
                positiveText = "Ligar Agora"
                negativeText = "Cancelar"
            }

            "1052" -> {
                title = "Ligar para a Polícia?"
                message = "Você será direcionado para a chamada telefônica. Confirme para discar imediatamente."
                positiveText = "Ligar Agora"
                negativeText = "Cancelar"
            }

            else -> {
                title = "Chamada"
                message = "Deseja realmente ligar para $number?"
                positiveText = "Confirmar"
                negativeText = "Cancelar"
            }
        }

        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ ->

                // 🔹 cria o monitor
                callMonitor = CallMonitor(
                    context = themedContext,
                    onCallStarted = {
                    },
                    onCallEnded = {
                            callMonitor?.stop()
                            callMonitor = null
                            if (number == "144") {
                                    val intent = Intent(this, PosEmergenciaActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    startActivity(intent)
                            } else if (number == "1052") {
                                    val intent = Intent(this, PosPoliciaActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    startActivity(intent)
                            }
                    }
                )

                callMonitor?.start()
                CallManager().toCall(this, number)

            }
            .setNegativeButton(negativeText, null)
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }


    fun sendUserLocationToContacts() {
        if (!permission.isSmsPermitted()) {
          Log.d("SMS", "Permissão negada")
          return
        }



        val contatos =
            ContatosEmergenciaManager.getNumerosContatosSelecionados(this)

        if (contatos.isEmpty()) {

            Log.d("SMS", "Nenhum contato selecionado")
            return
        }

        locationManager.getUserLocation { location ->

            if (location != null) {

                val mapsLink =
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"

                val smsManager =
                    SmsManager.getDefault()

                contatos.forEach { (telefone, nome) ->

                    val numeroLimpo =
                        telefone.replace(Regex("[^0-9+]"), "")

                    val mensagem =
                        "Olá $nome! Minha localização atual: $mapsLink"

                    try {

                        smsManager.sendTextMessage(
                            numeroLimpo,
                            null,
                            mensagem,
                            null,
                            null
                        )

                        Log.d(
                            "SMS",
                            "Mensagem enviada para $numeroLimpo"
                        )

                    } catch (e: Exception) {

                        Log.e(
                            "SMS",
                            "Erro ao enviar SMS para $numeroLimpo",
                            e
                        )
                    }
                }

            } else {

                Log.d("LOCATION", "Sem localização")
            }
        }
    }

    fun showSendLocationThroughSMSDialog() {

        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Compartilhar Localização via SMS?")
            .setMessage(
                "Todos os seus contatos de emergência receberão um link " +
                        "com sua localização atual."
            )
            .setPositiveButton("Compartilhar Agora") { _, _ ->

                sendUserLocationToContacts()
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.window?.setType(
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        )

        dialog.show()
    }

    private fun createNotification(): Notification {
        val channelId = "overlay_channel"
        val channel = NotificationChannel(
            channelId,
            "Floating Widget",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(themedContext, channelId)
            .setContentTitle("Widget Active")
            .setContentText("Your floating widget is running on screen.")
            .setSmallIcon(R.drawable.main_button)
            .build()
    }

    fun setVisibility(visible: Boolean) {
        buttonOverlay.setVisibility(visible)
    }

    fun fixOverlay(locked: Boolean) {
       buttonOverlay.isDraggable = !locked
    }
}