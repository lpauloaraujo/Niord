package com.example.niord
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.WindowManager
import androidx.lifecycle.LifecycleService
import androidx.core.app.NotificationCompat

class FloatingOverlayService : LifecycleService() {

    val themedContext = ContextThemeWrapper(this, R.style.Theme_Niord)
    private var callMonitor: CallMonitor? = null

    private lateinit var locationManager: LocationManager

    private lateinit var buttonOverlay: MainOverlayButton

    val permission = PermissionChecker(this)

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

        startForeground(1, createNotification())

        buttonOverlay = MainOverlayButton(context = this)
        buttonOverlayInit()

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
    }

    fun refresh(){
        buttonOverlay.applyStatePacketPreferences()
    }


    private fun showVigiaDialog(isActive: Boolean) {
        if (isActive) {
            showVigiaDeactivateDialog()
        } else {
            showVigiaActivateDialog()
        }
    }

    private fun showVigiaActivateDialog() {
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
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
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
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
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
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

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
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
                    android.telephony.SmsManager.getDefault()

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

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(
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