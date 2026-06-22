package com.example.niord
import android.R.attr.label
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
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
import com.example.niord.api.User
import kotlinx.coroutines.Dispatchers
import com.example.niord.api.HelpAnswer
import com.example.niord.api.HelpAnswerMulti
import com.example.niord.api.HelpAsk
import com.example.niord.api.HelpReceive
import com.example.niord.api.LocationSchema
import com.google.android.gms.location.Priority
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.serialization.json.Json
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.seconds
import androidx.core.net.toUri

class FloatingOverlayService : LifecycleService() {

    val themedContext = ContextThemeWrapper(this, R.style.Theme_Niord)
    private var callMonitor: CallMonitor? = null

    private lateinit var locationManager: LocationManager

    private lateinit var buttonOverlay: MainOverlayButton

    val permission = PermissionChecker(this)

    private lateinit var apiService: ApiService

    private val receivedIds = CopyOnWriteArrayList<Int>()

    private var alertState = MutableStateFlow(false)
    private var helpingState = MutableStateFlow(false)

    private var helpingStateWait = MutableStateFlow(false)
    private var helpingStateId = MutableStateFlow(-1)

    val locationChannel = Channel<Frame>(Channel.BUFFERED)

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
        stopVigia()
        alertCancel()
        endHelping(true, null)
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
                            "robbery" -> showHelpRequest(data)
                            "accident" -> showHelpRequest(data)
                            "acknowledge" -> startHelping(data)
                            "cancel" -> endHelping(true, data.userId)
                        }

                    },
                    locationChannel
                )
            }

            lifecycleScope.launch {
                while(isActive) {
                    val loc: Location? =
                        locationManager.fetchLocationRet(Priority.PRIORITY_LOW_POWER)
                    if (loc != null) {
                        val data = LocationSchema(
                            loc.latitude,
                            loc.longitude
                        )
                        locationChannel.send(Frame.Text(Json.encodeToString(data)))
                    }
                    delay(5.seconds)
                }
             }
        }
    }

    private fun receiveHelpAnswer(data: HelpReceive){
        if(!alertState.value) return //Ignore if not in alert

        if(receivedIds.contains(data.userId)){
            if(data.type == "deny") {
                receivedIds.remove(data.userId)
            }else{
                sendAnswerRequest(type = "acknowledge", targetId = data.userId)
                alertState.value = false
                showAlertHelpArrived()
                alertCancel()
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
                receivedIds.clear()
                lifecycleScope.launch {
                    val response = apiService.askHelp(data)
                    if((response.status.value == 200) and (type == "accident")){
                        alertState.value = true
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
        alertState.value = false
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

    private fun startHelping(data: HelpReceive){
        if(!helpingStateWait.value) return
        if(helpingState.value and (data.userId == helpingStateId.value)){
            //Confirmation of arrival
            endHelping(false, data.userId)
        }else {
            helpingState.value = true
            helpingStateId.value = data.userId
            openMap(
            Location("manual").apply{
                latitude = data.latitude
                longitude = data.longitude
            }
            )
        }
    }

    private fun endHelping(isCancel: Boolean, targetId: Int?){
        helpingState.value = false
        helpingStateWait.value = false
        if(isCancel and (targetId == helpingStateId.value)) {
            val dialog = MaterialAlertDialogBuilder(
                themedContext,
                R.style.CustomAlertDialog
            )
                .setTitle("Apoio cancelado")
                .setMessage(
                    "O motorista cancelou o pedido ou já está sendo ajudado."
                )
                .setPositiveButton("Ok") { _, _ -> {}}
                .create()
            dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            dialog.show()
        }
    }

    private fun answerHelpRequest(targetId: Int){
        if(helpingState.value) return //Ignore if helping someone else
        sendAnswerRequest(type = "accept", targetId = targetId)
        helpingStateWait.value = true
    }

    private fun denyHelpRequest(){
        sendAnswerRequest("deny", helpingStateId.value)
        endHelping(false, 0)
    }

    private fun openMap(location: Location, notId: Int = 67,
                        title: String = "Acompanhar pedido de ajuda",
                        subTitle: String = "Clique para abrir o mapa na posição de ajuda"){
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "map_navigation_channel"

        val channel = NotificationChannel(
            channelId,
            "Map Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications to open map coordinates"
        }
        notificationManager.createNotificationChannel(channel)


        val latitude = location.latitude
        val longitude = location.longitude
        val uriString = "geo:$latitude,$longitude?q=$latitude,$longitude($label)"
        val mapIntent = Intent(Intent.ACTION_VIEW, uriString.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setContentTitle(title)
            .setContentText(subTitle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notId, notification)
    }

    private fun showHelpRequest(data: HelpReceive){
        if(alertState.value) return //Ignore if user is currently asking for help
        if(helpingState.value and (data.type == "accident")) return
        val targetLoc = Location("manual").apply {
            latitude = data.latitude
            longitude = data.longitude
        }
        locationManager.getUserLocation { location ->
            val distanceMeters = location?.distanceTo(targetLoc)?.toInt()
            when (data.type) {
                "robbery" -> {
                    val dialog = MaterialAlertDialogBuilder(
                        themedContext,
                        R.style.CustomAlertDialog
                    )
                        .setTitle("Um motorista disparou uma alerta de assalto")
                        .setMessage(
                            "O alerta foi disparado a ${distanceMeters}m de distância"
                        )
                        .setPositiveButton("Ver no mapa") { _, _ ->
                            openMap(targetLoc,
                                notId = 42,
                                title = "Alerta de assalto",
                                subTitle = "Clique para abrir na " +
                                    "localização do alerta de assalto")}
                        .setNegativeButton("Ok") { _, _ -> {} }
                        .create()

                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    dialog.show()
                }

                "accident" -> {
                    val dialog = MaterialAlertDialogBuilder(
                        themedContext,
                        R.style.CustomAlertDialog
                    )
                        .setTitle("Um motorista disparou uma alerta de acidente")
                        .setMessage(
                            "O alerta foi disparado a ${distanceMeters}m de distância. \nVocê pode ajudar?"
                        )
                        .setPositiveButton("Ajudar") { _, _ -> answerHelpRequest(data.userId) }
                        .setNegativeButton("Não posso") { _, _ -> {} }
                        .create()
                    dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
                    dialog.show()
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
        if(!apiService.isWebsocketConnected.value){
            initOverwatch()
        }
        if(alertState.value){
            showAlertDialogTrack()
        }else if(helpingState.value){
            showAlertDialogHelping()
        } else{
            showAlertDialogAsk()
        }
    }

    private fun showAlertDialogHelping(){

        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Menu de acompanhamento?")
            .setMessage(
                "O motorista será notificado.\n" +
                        "Clique fora da caixa para cancelar a ação"
            )
            .setPositiveButton("Cancelar Ajuda") { _, _ -> denyHelpRequest() }
            .setNegativeButton("Cheguei ao local") { _, _, ->
                sendAnswerRequest("accept", helpingStateId.value)
                endHelping(false, null)
            }
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
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

    private fun showLocationSentDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Localização Compartilhada")
            .setMessage("Sua localização foi enviada para os contatos de emergência.")
            .setPositiveButton("OK", null)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showLocationErrorDialog(message: String) {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Falha ao Compartilhar")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .create()

        dialog.window?.setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        dialog.show()
    }

    private fun showLocationPartialDialog(enviados: Int, total: Int) {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Envio Parcial")
            .setMessage(
                "A localização foi enviada para $enviados de $total contatos de emergência."
            )
            .setPositiveButton("OK", null)
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
            showLocationErrorDialog("Nenhum contato de emergência foi configurado.")
            return
        }

        Log.d(
            "LOCATION",
            "Permissão: ${permission.isLocationPermitted()}"
        )

        locationManager.getUserLocation { location ->

            if (location != null) {

                val totalContatos = contatos.size
                val sucessos = AtomicInteger(0)
                val finalizados = AtomicInteger(0)

                val mapsLink =
                    "https://maps.google.com/?q=${location.latitude},${location.longitude}"

                contatos.forEach { (telefone, nome) ->

                    val numeroLimpo =
                        telefone.replace(Regex("""\D"""), "")

                    val mensagem =
                        "Olá $nome! Minha localização atual: $mapsLink"

                    lifecycleScope.launch(Dispatchers.IO) {

                        try {

                            val response = apiService.sendWhatsappMessage(
                                numeroLimpo,
                                mensagem
                            )

                            if (response.status.value in 200..299) {
                                Log.d("WAHA", "Mensagem enviada para $numeroLimpo")
                                sucessos.incrementAndGet()
                            } else {
                                Log.e(
                                    "WAHA",
                                    "Falha ao enviar para $numeroLimpo: ${response.status}"
                                )
                            }

                        } catch (e: Exception) {

                            Log.e("WAHA", "Erro ao enviar para $numeroLimpo", e)

                        } finally {

                            if (finalizados.incrementAndGet() == totalContatos) {

                                launch(Dispatchers.Main) {

                                    when (val enviados = sucessos.get()) {
                                        totalContatos -> {
                                            showLocationSentDialog()
                                        }
                                        0 -> {
                                            showLocationErrorDialog(
                                                "Não foi possível enviar a localização para nenhum contato."
                                            )
                                        }
                                        else -> {
                                            showLocationPartialDialog(enviados, totalContatos)
                                        }
                                    }
                                }
                            }
                        }
                    }.start()
                }
            } else {
                Log.d("LOCATION", "Sem localização")
                    showLocationErrorDialog("Não foi possível obter sua localização atual. Verifique se a localização do dispositivo está ativada.")
            }
        }
    }

    fun showSendLocationThroughSMSDialog() {

        val dialog = MaterialAlertDialogBuilder(
            themedContext,
            R.style.CustomAlertDialog
        )
            .setTitle("Compartilhar Localização via Whatsapp?")
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
            .setContentTitle("Overlay Niord")
            .setContentText("O overlay Niord está ativo na tela.")
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
