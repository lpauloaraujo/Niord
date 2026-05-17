package com.example.niord

import android.Manifest
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.text.Normalizer
import kotlin.math.abs
import android.util.Log

@RequiresApi(Build.VERSION_CODES.O)
class ContatosEmergenciaActivity : ComponentActivity() {
    private var permission = Permission(this)
    private lateinit var searchContatos: EditText
    private lateinit var contatosList: LinearLayout
    private lateinit var contatosSelecionadosList: LinearLayout
    private lateinit var emptyStateContatos: TextView
    private lateinit var emptyStateSelecionados: TextView
    private lateinit var permissionDialogOverlay: FrameLayout
    private lateinit var btnBuscarContatos: ImageButton
    private lateinit var btnLimparBusca: ImageButton
    private lateinit var btnAction: ImageButton
    private lateinit var headerTitle: TextView
    private lateinit var selectionScreenContainer: LinearLayout
    private lateinit var selectedScreenContainer: LinearLayout

    private val allContatos = mutableListOf<ContatoEmergencia>()
    private val filteredContatos = mutableListOf<ContatoEmergencia>()
    private val selecionados = mutableSetOf<String>()
    private val selecionadosContatos = mutableListOf<ContatoEmergencia>()
    private var selectionMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.contatos_emergencia)
        findViewById<View>(R.id.selectionScreenContainer).applyStatusBarPadding()

        searchContatos = findViewById(R.id.searchContatos)
        contatosList = findViewById(R.id.contatosList)
        contatosSelecionadosList = findViewById(R.id.contatosSelecionadosList)
        emptyStateContatos = findViewById(R.id.emptyStateContatos)
        emptyStateSelecionados = findViewById(R.id.emptyStateSelecionados)
        permissionDialogOverlay = findViewById(R.id.permissionDialogOverlay)
        btnBuscarContatos = findViewById(R.id.btnBuscarContatos)
        btnLimparBusca = findViewById(R.id.btnLimparBusca)
        btnAction = findViewById(R.id.btnAction)
        headerTitle = findViewById(R.id.headerTitle)
        selectionScreenContainer = findViewById(R.id.selectionScreenContainer)
        selectedScreenContainer = findViewById(R.id.selectedScreenContainer)

        findViewById<ImageButton>(R.id.btnVoltar).setOnClickListener { finish() }
        btnAction.setOnClickListener {
            if (selectionMode) {
                confirmSelection()
            } else {
                enterSelectionMode()
            }
        }

        setupSearch()
        setupPermissionDialog()

        // Load saved contact ids and names from previous sessions
        selecionados.addAll(ContatosEmergenciaPreferences.getContatosSelecionados(this))

        // Check permission
        if (permission.isContactsPermitted(this)) {
            loadContatos()
            if (selecionados.isEmpty()) {
                enterSelectionMode()
            } else {
                populateSelecionadosContatos()
                enterSelectedMode()
            }
        } else {
            showPermissionRequest()
        }
    }

    private fun setupSearch() {
        searchContatos.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString().orEmpty().lowercase()
                btnLimparBusca.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterContatos(query)
            }
        })

        searchContatos.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                executeSearch()
                true
            } else {
                false
            }
        }

        btnBuscarContatos.setOnClickListener {
            executeSearch()
        }

        btnLimparBusca.setOnClickListener {
            searchContatos.text.clear()
        }
    }

    private fun executeSearch() {
        filterContatos(searchContatos.text.toString())
        searchContatos.clearFocus()
    }

    private fun setupPermissionDialog() {
        findViewById<android.widget.Button>(R.id.btnPermitirPermissao).setOnClickListener {
            permission.requestContactsPermission { granted ->
                permissionDialogOverlay.visibility = View.GONE
                if (granted) {
                    loadContatos()
                    if (selecionados.isEmpty()) {
                        enterSelectionMode()
                    } else {
                        populateSelecionadosContatos()
                        enterSelectedMode()
                    }
                } else {
                    showPermissionDeniedMessage()
                }
            }
        }

        findViewById<android.widget.Button>(R.id.btnNegarPermissao).setOnClickListener {
            permissionDialogOverlay.visibility = View.GONE
            showPermissionDeniedMessage()
        }
    }

    private fun showPermissionRequest() {
        permissionDialogOverlay.visibility = View.VISIBLE
    }

    private fun showPermissionDeniedMessage() {
        Toast.makeText(
            this,
            "É necessário permitir o acesso para escolher os contatos",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    private fun loadContatos() {
        allContatos.clear()
        allContatos.addAll(queryDeviceContacts())
        Log.d("ContatosEmergencia", "loadContatos: loaded ${allContatos.size} contacts")
        allContatos.forEachIndexed { i, c -> 
            Log.d("ContatosEmergencia", "  [$i] id=${c.id}, nome=${c.nome}, tel=${c.telefone}") 
        }

        filterContatos("")
        updateViews()
    }

    private fun populateSelecionadosContatos() {
        selecionadosContatos.clear()
        selecionadosContatos.addAll(
            allContatos.filter { selecionados.contains(it.id) }
                .sortedBy { it.nome }
        )
    }

    fun getNumerosContatosSelecionados(): List<Pair<String, String>> {
        if (selecionadosContatos.isEmpty() && selecionados.isNotEmpty()) {
            populateSelecionadosContatos()
        }

        return selecionadosContatos
            .filter { selecionados.contains(it.id) }
            .map { contato -> contato.telefone to contato.nome }
    }

    private fun queryDeviceContacts(): List<ContatoEmergencia> {
        val result = mutableListOf<ContatoEmergencia>()
        val seenNumbers = mutableSetOf<String>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone._ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

        val cursor: Cursor? = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val id = it.getString(idIndex)
                val name = it.getString(nameIndex) ?: ""
                val numberRaw = it.getString(numberIndex) ?: ""
                val phone = normalizePhoneNumber(numberRaw)
                if (phone.isEmpty() || seenNumbers.contains(phone)) continue
                seenNumbers.add(phone)

                result.add(
                    ContatoEmergencia(
                        id = id,
                        nome = if (name.isBlank()) phone else name,
                        telefone = formatPhoneNumber(numberRaw)
                    )
                )
            }
        }

        return result
    }

    private fun normalizePhoneNumber(number: String): String {
        return number.replace(Regex("[^0-9]"), "")
    }

    private fun formatPhoneNumber(number: String): String {
        return number.trim()
    }

    private fun filterContatos(query: String) {
        filteredContatos.clear()
        val normalizedQuery = normalizeSearchText(query)
        val numericQuery = query.replace(Regex("[^0-9]"), "")
        Log.d("ContatosEmergencia", "filterContatos CALLED: query='$normalizedQuery', allContatos.size=${allContatos.size}")

        filteredContatos.addAll(
            allContatos.filter { contato ->
                val nomeMatch = normalizeSearchText(contato.nome).contains(normalizedQuery)
                val phoneMatch = numericQuery.isNotEmpty() &&
                    contato.telefone.replace(Regex("[^0-9]"), "").contains(numericQuery)
                val matches = nomeMatch || phoneMatch
                Log.d("ContatosEmergencia", "  contato: nome='${contato.nome}', nomeMatch=$nomeMatch, phoneMatch=$phoneMatch, final=$matches")
                matches
            }
        )

        Log.d("ContatosEmergencia", "filterContatos DONE: filtered=${filteredContatos.size} contatos")
        filteredContatos.forEachIndexed { i, c ->
            Log.d("ContatosEmergencia", "    [$i] ${c.nome}")
        }
        updateContatosListView()
    }

    private fun normalizeSearchText(text: String): String {
        return Normalizer.normalize(text.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
    }

    private fun updateViews() {
        if (selectionMode) {
            updateContatosListView()
        } else {
            updateSelecionadosView()
        }
    }

    private fun enterSelectionMode() {
        selectionMode = true
        headerTitle.text = "Selecionar contatos"
        btnAction.setImageResource(R.drawable.ic_check)
        searchContatos.visibility = View.VISIBLE
        contatosList.visibility = View.VISIBLE
        btnLimparBusca.visibility = if (searchContatos.text.isNotEmpty()) View.VISIBLE else View.GONE
        selectionScreenContainer.visibility = View.VISIBLE
        selectedScreenContainer.visibility = View.GONE
        filterContatos(searchContatos.text.toString().lowercase())
    }

    private fun enterSelectedMode() {
        selectionMode = false
        headerTitle.text = "Contatos de Emergência"
        btnAction.setImageResource(R.drawable.ic_add_button)
        searchContatos.visibility = View.GONE
        btnLimparBusca.visibility = View.GONE
        contatosList.visibility = View.GONE
        selectionScreenContainer.visibility = View.GONE
        selectedScreenContainer.visibility = View.VISIBLE
        updateSelecionadosView()
    }

    private fun confirmSelection() {
        if (selecionados.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos um contato", Toast.LENGTH_SHORT).show()
            return
        }

        populateSelecionadosContatos()
        ContatosEmergenciaPreferences.salvarContatosSelecionados(this, selecionados.toList())
        enterSelectedMode()
    }

    private fun updateSelecionadosView() {
        contatosSelecionadosList.removeAllViews()

        if (selecionadosContatos.isEmpty()) {
            emptyStateSelecionados.visibility = View.VISIBLE
        } else {
            emptyStateSelecionados.visibility = View.GONE
            selecionadosContatos.forEach { contato ->
                val itemView = createSelecionadoNomeView(contato)
                contatosSelecionadosList.addView(itemView)
            }
        }
    }

    private fun updateContatosListView() {
        contatosList.removeAllViews()

        if (filteredContatos.isEmpty()) {
            emptyStateContatos.visibility = View.VISIBLE
        } else {
            emptyStateContatos.visibility = View.GONE
            filteredContatos.forEach { contato ->
                val itemView = createContatoItemView(contato)
                contatosList.addView(itemView)
            }
        }
    }

    private fun createSelecionadoItemView(contato: ContatoEmergencia): View {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.item_contato_emergencia, contatosSelecionadosList, false)

        itemView.findViewById<TextView>(R.id.contatoNome).text = contato.nome
        itemView.findViewById<TextView>(R.id.contatoTelefone).text = contato.telefone
        itemView.findViewById<CheckBox>(R.id.contatoCheckbox).visibility = View.GONE

        val removeBtn = itemView.findViewById<ImageButton>(R.id.contatoRemoveBtn)
        removeBtn.visibility = View.GONE
        itemView.setOnTouchListener(createSwipeToDeleteTouchListener(contato))

        return itemView
    }

    private fun createSelecionadoNomeView(contato: ContatoEmergencia): View {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.item_contato_emergencia, contatosSelecionadosList, false)

        itemView.findViewById<TextView>(R.id.contatoNome).text = contato.nome
        itemView.findViewById<TextView>(R.id.contatoTelefone).visibility = View.GONE
        itemView.findViewById<CheckBox>(R.id.contatoCheckbox).visibility = View.INVISIBLE
        itemView.findViewById<ImageButton>(R.id.contatoRemoveBtn).visibility = View.GONE

        itemView.setOnTouchListener(createSwipeToDeleteTouchListener(contato) {
            dialContato(contato)
        })

        return itemView
    }

    private fun dialContato(contato: ContatoEmergencia) {
        val phone = contato.telefone.replace(Regex("[^0-9+]"), "")
        if (phone.isBlank()) {
            Toast.makeText(this, "Número inválido para ligação", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "Não foi possível iniciar a chamada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createContatoItemView(contato: ContatoEmergencia): View {
        val inflater = LayoutInflater.from(this)
        val itemView = inflater.inflate(R.layout.item_contato_emergencia, contatosList, false)

        itemView.findViewById<TextView>(R.id.contatoNome).text = contato.nome
        itemView.findViewById<TextView>(R.id.contatoTelefone).text = contato.telefone

        val checkbox = itemView.findViewById<CheckBox>(R.id.contatoCheckbox)
        checkbox.isChecked = selecionados.contains(contato.id)

        checkbox.setOnClickListener {
            if (checkbox.isChecked) {
                if (selecionados.size < 5) {
                    selecionados.add(contato.id)
                } else {
                    checkbox.isChecked = false
                    Toast.makeText(
                        this,
                        "Máximo de 5 contatos atingido",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                selecionados.remove(contato.id)
            }
        }

        itemView.setOnClickListener {
            checkbox.isChecked = !checkbox.isChecked
        }

        return itemView
    }

    private fun createSwipeToDeleteTouchListener(
        contato: ContatoEmergencia,
        onTap: (() -> Unit)? = null
    ): View.OnTouchListener {
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onTap?.invoke()
                return onTap != null
            }

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (deltaX < -120 && abs(velocityX) > abs(velocityY) && abs(velocityX) > 150) {
                    removerContato(contato)
                    return true
                }
                return false
            }
        })

        return View.OnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
        }
    }

    private fun adicionarContato(contato: ContatoEmergencia) {
        if (!selecionados.contains(contato.id) && selecionados.size < 5) {
            selecionados.add(contato.id)
            ContatosEmergenciaPreferences.adicionarContato(this, contato.id)
            android.util.Log.d("ContatosEmergencia", "adicionarContato called for id=${contato.id}")
            android.util.Log.d("ContatosEmergencia", "selecionados after add: $selecionados")
            Toast.makeText(this, "Contato adicionado aos favoritos", Toast.LENGTH_SHORT).show()
            // After adding, switch to the selected contacts screen to reflect the change
            enterSelectedMode()
        }
    }

    private fun removerContato(contato: ContatoEmergencia) {
        selecionados.remove(contato.id)
        selecionadosContatos.removeAll { it.id == contato.id }
        ContatosEmergenciaPreferences.removerContato(this, contato.id)
        android.util.Log.d("ContatosEmergencia", "removerContato called for id=${contato.id}")
        android.util.Log.d("ContatosEmergencia", "selecionados after remove: $selecionados")
        Toast.makeText(this, "Contato removido", Toast.LENGTH_SHORT).show()
        updateViews()
    }
}
