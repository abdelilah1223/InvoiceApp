package com.elbaroudi.invoice

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

class AddFragment : Fragment() {

    companion object {
        private const val PREF_BG_COLOR = "bg_color"
        private const val PREF_LANGUAGE = "language"
        private const val DEFAULT_BG_COLOR = "#FFFFFF"
        private const val DEFAULT_LANGUAGE = "en"

        fun newInstance(invoiceId: String? = null): AddFragment {
            val fragment = AddFragment()
            invoiceId?.let {
                val args = Bundle()
                args.putString("invoice_id", it)
                fragment.arguments = args
            }
            return fragment
        }
    }

    // Database helper
    private lateinit var dbHelper: InvoiceDbHelper

    // UI state
    private var bgColor = DEFAULT_BG_COLOR
    private var language = DEFAULT_LANGUAGE
    private lateinit var sharedPref: SharedPreferences
    private val collapsedSections = mutableMapOf(
        "company" to false,
        "client" to false,
        "items" to false,
        "additional" to false
    )

    // Views
    private lateinit var mainLayout: ScrollView
    private lateinit var companyHeader: LinearLayout
    private lateinit var companyContent: LinearLayout
    private lateinit var clientHeader: LinearLayout
    private lateinit var clientContent: LinearLayout
    private lateinit var itemsHeader: LinearLayout
    private lateinit var itemsContent: LinearLayout
    private lateinit var additionalHeader: LinearLayout
    private lateinit var additionalContent: LinearLayout

    // Input fields
    private lateinit var companyName: EditText
    private lateinit var companyPhone: EditText
    private lateinit var companyEmail: EditText
    private lateinit var companyAddress: EditText
    private lateinit var clientName: EditText
    private lateinit var clientPhone: EditText
    private lateinit var clientEmail: EditText
    private lateinit var clientAddress: EditText
    private lateinit var itemName: EditText
    private lateinit var quantity: EditText
    private lateinit var unitPrice: EditText
    private lateinit var itemsContainer: LinearLayout
    private lateinit var addItemBtn: Button
    private lateinit var currency: EditText
    private lateinit var tax: EditText
    private lateinit var subtotalValue: TextView
    private lateinit var taxValue: TextView
    private lateinit var totalValue: TextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    // Data
    private val invoiceItems = mutableListOf<InvoiceItem>()
    private var invoiceId: String = ""
    private var editId: String? = null
    private var invoiceName: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_invoice, container, false)

        // Initialize database helper
        dbHelper = InvoiceDbHelper(requireContext())

        sharedPref =  requireActivity().getSharedPreferences("app_settings", MODE_PRIVATE)
        loadUserPreferences()
        initializeViews(view)
        editId = arguments?.getString("invoice_id")
        setupInvoiceDetails()
        setupUI()

        return view
    }

    private fun loadUserPreferences() {
        bgColor = sharedPref.getString(PREF_BG_COLOR, DEFAULT_BG_COLOR) ?: DEFAULT_BG_COLOR
        language = sharedPref.getString(PREF_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

        // Ensure text color is readable on the selected background
        val textColor = getContrastColor(bgColor)
        sharedPref.edit().putString("text_color", textColor).apply()
    }

    private fun getContrastColor(hexColor: String): String {
        val hex = hexColor.replace("#", "")
        val r = hex.substring(0, 2).toInt(16)
        val g = hex.substring(2, 4).toInt(16)
        val b = hex.substring(4, 6).toInt(16)
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        return if (brightness > 128) "#000000" else "#FFFFFF"
    }

    private fun initializeViews(view: View) {
        mainLayout = view.findViewById(R.id.mainLayout)

        // Section headers and content
        companyHeader = view.findViewById(R.id.companyHeader)
        companyContent = view.findViewById(R.id.companyContent)
        clientHeader = view.findViewById(R.id.clientHeader)
        clientContent = view.findViewById(R.id.clientContent)
        itemsHeader = view.findViewById(R.id.itemsHeader)
        itemsContent = view.findViewById(R.id.itemsContent)
        additionalHeader = view.findViewById(R.id.additionalHeader)
        additionalContent = view.findViewById(R.id.additionalContent)

        // Company section
        companyName = view.findViewById(R.id.companyName)
        companyPhone = view.findViewById(R.id.companyPhone)
        companyEmail = view.findViewById(R.id.companyEmail)
        companyAddress = view.findViewById(R.id.companyAddress)

        // Client section
        clientName = view.findViewById(R.id.clientName)
        clientPhone = view.findViewById(R.id.clientPhone)
        clientEmail = view.findViewById(R.id.clientEmail)
        clientAddress = view.findViewById(R.id.clientAddress)

        // Items section
        itemsContainer = view.findViewById(R.id.itemsContainer)
        addItemBtn = view.findViewById(R.id.addItemBtn)
        itemName = view.findViewById(R.id.itemName)
        quantity = view.findViewById(R.id.quantity)
        unitPrice = view.findViewById(R.id.unitPrice)

        currency = view.findViewById(R.id.currency)
        tax = view.findViewById(R.id.tax)

        // Totals
        subtotalValue = view.findViewById(R.id.subtotalValue)
        taxValue = view.findViewById(R.id.taxValue)
        totalValue = view.findViewById(R.id.totalValue)

        // Buttons
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
    }

    private fun setupInvoiceDetails() {
        invoiceId = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        editId?.let { loadInvoice(it) }
    }

    private fun loadInvoice(id: String) {
        val invoice = dbHelper.getInvoiceById(id)
        if (invoice != null) {
            // Set invoice name
            invoiceName = invoice.name

            // Fill company information
            companyName.setText(invoice.companyName)
            companyPhone.setText(invoice.companyPhone)
            companyEmail.setText(invoice.companyEmail)
            companyAddress.setText(invoice.companyAddress)

            // Fill client information
            clientName.setText(invoice.clientName)
            clientPhone.setText(invoice.clientPhone)
            clientEmail.setText(invoice.clientEmail)
            clientAddress.setText(invoice.clientAddress)

            // Clear existing items and add loaded items
            invoiceItems.clear()
            invoiceItems.addAll(invoice.items)
            updateItemsList()

            // Fill additional information
            currency.setText(invoice.currency)
            tax.setText(invoice.taxRate.toString())

            subtotalValue.text = "%.2f %s".format(invoice.subtotal, invoice.currency)
            taxValue.text = "%.2f %s".format(invoice.taxAmount, invoice.currency)
            totalValue.text = "%.2f %s".format(invoice.totalAmount, invoice.currency)
            showMessage(getString(R.string.invoice_loaded))
        } else {
            showError(getString(R.string.invoice_not_found))
        }
    }

    private fun setupUI() {
        applyBackgroundSettings()
        updateTextForLanguage()

        // Section toggles
        companyHeader.setOnClickListener { toggleSection("company", companyContent) }
        clientHeader.setOnClickListener { toggleSection("client", clientContent) }
        itemsHeader.setOnClickListener { toggleSection("items", itemsContent) }
        additionalHeader.setOnClickListener { toggleSection("additional", additionalContent) }

        // Item management
        addItemBtn.setOnClickListener { addItem() }
        itemName.addTextChangedListener(calculateTotalsWatcher)
        quantity.addTextChangedListener(calculateTotalsWatcher)
        unitPrice.addTextChangedListener(calculateTotalsWatcher)
        tax.addTextChangedListener(calculateTotalsWatcher)

        // Action buttons
        saveButton.setOnClickListener { showSaveDialog() }
        cancelButton.setOnClickListener { requireActivity().onBackPressed() }
    }

    private val calculateTotalsWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) { calculateTotals() }
    }

    private fun toggleSection(sectionKey: String, sectionView: View) {
        collapsedSections[sectionKey] = !collapsedSections[sectionKey]!!
        sectionView.visibility = if (collapsedSections[sectionKey]!!) View.GONE else View.VISIBLE

        // Update arrow icon
        val arrowIcon = when (sectionKey) {
            "company" -> companyHeader.findViewById<ImageView>(R.id.companyArrow)
            "client" -> clientHeader.findViewById<ImageView>(R.id.clientArrow)
            "items" -> itemsHeader.findViewById<ImageView>(R.id.itemsArrow)
            "additional" -> additionalHeader.findViewById<ImageView>(R.id.additionalArrow)
            else -> null
        }

        arrowIcon?.setImageResource(
            if (collapsedSections[sectionKey]!!)
                R.drawable.ic_arrow_down
            else
                R.drawable.ic_arrow_up
        )
    }

    private fun addItem() {
        val name = itemName.text.toString()
        val qty = quantity.text.toString()
        val price = unitPrice.text.toString()

        if (name.isEmpty() || qty.isEmpty() || price.isEmpty()) {
            showError(getString(R.string.fill_all_fields))
            return
        }

        try {
            val item = InvoiceItem(
                name = name,
                quantity = qty.toInt(),
                unitPrice = price.toDouble()
            )
            invoiceItems.add(item)
            updateItemsList()
            clearItemFields()
            calculateTotals()
        } catch (e: NumberFormatException) {
            showError(getString(R.string.invalid_numbers))
        }
    }

    private fun updateItemsList() {
        itemsContainer.removeAllViews()

        invoiceItems.forEachIndexed { index, item ->
            val itemView = LayoutInflater.from(context).inflate(R.layout.item_invoice_row, itemsContainer, false)

            itemView.findViewById<TextView>(R.id.itemName).text = item.name
            itemView.findViewById<TextView>(R.id.itemQuantity).text = item.quantity.toString()
            itemView.findViewById<TextView>(R.id.itemPrice).text = "%.2f".format(item.unitPrice)
            itemView.findViewById<TextView>(R.id.itemTotal).text = "%.2f".format(item.quantity * item.unitPrice)

            // Edit button
            itemView.findViewById<ImageView>(R.id.editItemBtn).setOnClickListener {
                editItem(index)
            }

            // Delete button
            itemView.findViewById<ImageView>(R.id.deleteItemBtn).setOnClickListener {
                deleteItem(index)
            }

            itemsContainer.addView(itemView)
        }
    }

    private fun editItem(index: Int) {
        if (index in 0 until invoiceItems.size) {
            val item = invoiceItems[index]
            itemName.setText(item.name)
            quantity.setText(item.quantity.toString())
            unitPrice.setText(item.unitPrice.toString())
            invoiceItems.removeAt(index)
            updateItemsList()
            calculateTotals()
        }
    }

    private fun deleteItem(index: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_item))
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                if (index in 0 until invoiceItems.size) {
                    invoiceItems.removeAt(index)
                    updateItemsList()
                    calculateTotals()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun clearItemFields() {
        itemName.text.clear()
        quantity.text.clear()
        unitPrice.text.clear()
    }

    private fun calculateTotals(): Triple<Double, Double, Double> {
        val subtotal = invoiceItems.sumOf { it.quantity * it.unitPrice }
        val taxRate = try {
            tax.text.toString().toDouble()
        } catch (e: NumberFormatException) {
            0.0
        }
        val taxAmount = subtotal * (taxRate / 100)
        val total = subtotal + taxAmount

        val curr = currency.text.toString().ifEmpty { "DH" }

        subtotalValue.text = "%.2f %s".format(subtotal, curr)
        taxValue.text = "%.2f %s".format(taxAmount, curr)
        totalValue.text = "%.2f %s".format(total, curr)

        return Triple(subtotal, taxAmount, total)
    }

    private fun showSaveDialog() {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_save_invoice, null)
        val invoiceNameInput = view.findViewById<EditText>(R.id.invoiceNameInput)

        // If editing, pre-fill the existing name
        editId?.let {
            invoiceNameInput.setText(invoiceName)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.save_invoice))
            .setView(view)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val name = invoiceNameInput.text.toString()
                if (name.isNotEmpty()) {
                    invoiceName = name
                    saveInvoice()
                } else {
                    showError(getString(R.string.enter_name))
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun saveInvoice() {
        val (subtotal, taxAmount, total) = calculateTotals()

        val invoice = Invoice(
            id = editId ?: invoiceId,
            name = invoiceName,
            companyName = companyName.text.toString(),
            companyPhone = companyPhone.text.toString(),
            companyEmail = companyEmail.text.toString(),
            companyAddress = companyAddress.text.toString(),
            clientName = clientName.text.toString(),
            clientPhone = clientPhone.text.toString(),
            clientEmail = clientEmail.text.toString(),
            clientAddress = clientAddress.text.toString(),
            items = invoiceItems.toList(),
            currency = currency.text.toString(),
            taxRate = tax.text.toString().toDoubleOrNull() ?: 0.0,
            createdAt = System.currentTimeMillis(),
            subtotal = subtotal,
            taxAmount = taxAmount,
            totalAmount = total
        )

        val result = if (editId == null) {
            dbHelper.addInvoice(invoice)
        } else {
            dbHelper.updateInvoice(invoice).toLong()
        }

        if (result != -1L) {
            showMessage(getString(R.string.invoice_saved))
            if (editId == null) {
                clearForm()
            }
        } else {
            showError(getString(R.string.save_failed))
        }
    }

    private fun clearForm() {
        invoiceName = ""
        companyName.text.clear()
        companyPhone.text.clear()
        companyEmail.text.clear()
        companyAddress.text.clear()

        clientName.text.clear()
        clientPhone.text.clear()
        clientEmail.text.clear()
        clientAddress.text.clear()

        invoiceItems.clear()
        updateItemsList()
        clearItemFields()

        currency.text.clear()
        tax.setText("0")

        calculateTotals()
    }

    private fun applyBackgroundSettings() {
        mainLayout.setBackgroundColor(Color.parseColor(bgColor))

        val textColor = getContrastColor(bgColor)
        val borderColor = if (bgColor == DEFAULT_BG_COLOR) "#E0E0E0" else "rgba(255,255,255,0.2)"
        val placeholderTextColor = if (textColor == "#000000") "#888888" else "rgba(255,255,255,0.7)"

        // Apply colors to all text views and inputs
        val textViews = listOf<TextView>(
            companyName, companyPhone, companyEmail, companyAddress,
            clientName, clientPhone, clientEmail, clientAddress,
            itemName, quantity, unitPrice, currency, tax,
            subtotalValue, taxValue, totalValue
        )

        textViews.forEach { tv ->
            tv.setTextColor(Color.parseColor(textColor))
            tv.setHintTextColor(Color.parseColor(placeholderTextColor))
        }

        // Apply colors to buttons
        val buttons = listOf<Button>(addItemBtn, saveButton, cancelButton)
        buttons.forEach { btn ->
            btn.setTextColor(Color.parseColor(textColor))
            btn.background.setTint(Color.parseColor(borderColor))
        }
    }

    private fun updateTextForLanguage() {
        // Update all text elements based on language
        val isArabic = language == "ar"

        companyName.hint = getString(if (isArabic) R.string.company_name_ar else R.string.company_name)
        companyPhone.hint = getString(if (isArabic) R.string.company_phone_ar else R.string.company_phone)
        companyEmail.hint = getString(if (isArabic) R.string.email_ar else R.string.email)
        companyAddress.hint = getString(if (isArabic) R.string.address_ar else R.string.address)

        clientName.hint = getString(if (isArabic) R.string.client_name_ar else R.string.client_name)
        clientPhone.hint = getString(if (isArabic) R.string.client_phone_ar else R.string.client_phone)
        clientEmail.hint = getString(if (isArabic) R.string.email_ar else R.string.email)
        clientAddress.hint = getString(if (isArabic) R.string.address_ar else R.string.address)

        itemName.hint = getString(if (isArabic) R.string.item_name_ar else R.string.item_name)
        quantity.hint = getString(if (isArabic) R.string.quantity_ar else R.string.quantity)
        unitPrice.hint = getString(if (isArabic) R.string.price_ar else R.string.price)
        currency.hint = getString(if (isArabic) R.string.currency_placeholder_ar else R.string.currency_placeholder)
        tax.hint = getString(if (isArabic) R.string.tax_rate_placeholder_ar else R.string.tax_rate_placeholder)

        saveButton.text = getString(if (isArabic) R.string.save_invoice_ar else R.string.save_invoice)
        cancelButton.text = getString(if (isArabic) R.string.cancel_ar else R.string.cancel)
    }

    private fun showMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    data class InvoiceItem(
        val name: String,
        val quantity: Int,
        val unitPrice: Double
    ) : java.io.Serializable

    data class Invoice(
        val id: String,
        val name: String,
        val companyName: String,
        val companyPhone: String,
        val companyEmail: String,
        val companyAddress: String,
        val clientName: String,
        val clientPhone: String,
        val clientEmail: String,
        val clientAddress: String,
        val items: List<InvoiceItem>,
        val currency: String,
        val taxRate: Double,
        val createdAt: Long,
        val subtotal: Double,
        val taxAmount: Double,
        val totalAmount: Double
    ) : Serializable

    class InvoiceDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
        companion object {
            private const val DATABASE_NAME = "Invoices.db"
            private const val DATABASE_VERSION = 4

            const val TABLE_INVOICES = "invoices"
            const val COLUMN_ID = "id"
            const val COLUMN_NAME = "name"
            const val COLUMN_COMPANY_NAME = "company_name"
            const val COLUMN_COMPANY_PHONE = "company_phone"
            const val COLUMN_COMPANY_EMAIL = "company_email"
            const val COLUMN_COMPANY_ADDRESS = "company_address"
            const val COLUMN_CLIENT_NAME = "client_name"
            const val COLUMN_CLIENT_PHONE = "client_phone"
            const val COLUMN_CLIENT_EMAIL = "client_email"
            const val COLUMN_CLIENT_ADDRESS = "client_address"
            const val COLUMN_ITEMS_JSON = "items_json"
            const val COLUMN_CURRENCY = "currency"
            const val COLUMN_TAX_RATE = "tax_rate"
            const val COLUMN_CREATED_AT = "created_at"
            const val COLUMN_SUBTOTAL = "subtotal"
            const val COLUMN_TAX_AMOUNT = "tax_amount"
            const val COLUMN_TOTAL_AMOUNT = "total_amount"
        }

        override fun onCreate(db: SQLiteDatabase) {
            val createTable = """
                CREATE TABLE $TABLE_INVOICES (
                    $COLUMN_ID TEXT PRIMARY KEY,
                    $COLUMN_NAME TEXT,
                    $COLUMN_COMPANY_NAME TEXT,
                    $COLUMN_COMPANY_PHONE TEXT,
                    $COLUMN_COMPANY_EMAIL TEXT,
                    $COLUMN_COMPANY_ADDRESS TEXT,
                    $COLUMN_CLIENT_NAME TEXT,
                    $COLUMN_CLIENT_PHONE TEXT,
                    $COLUMN_CLIENT_EMAIL TEXT,
                    $COLUMN_CLIENT_ADDRESS TEXT,
                    $COLUMN_ITEMS_JSON TEXT,
                    $COLUMN_CURRENCY TEXT,
                    $COLUMN_TAX_RATE REAL,
                    $COLUMN_CREATED_AT INTEGER,
                    $COLUMN_SUBTOTAL REAL,
                    $COLUMN_TAX_AMOUNT REAL,
                    $COLUMN_TOTAL_AMOUNT REAL
                )
            """.trimIndent()
            db.execSQL(createTable)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (oldVersion < 4) {
                db.execSQL("DROP TABLE IF EXISTS $TABLE_INVOICES")
                onCreate(db)
            }
        }

        fun getAllInvoices(): List<Invoice> {
            val invoices = mutableListOf<Invoice>()
            val db = readableDatabase
            val cursor = db.query(
                TABLE_INVOICES,
                null,
                null,
                null,
                null,
                null,
                "$COLUMN_CREATED_AT DESC"
            )

            while (cursor.moveToNext()) {
                val invoice = Invoice(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    companyName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_NAME)),
                    companyPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_PHONE)),
                    companyEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_EMAIL)),
                    companyAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_ADDRESS)),
                    clientName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_NAME)),
                    clientPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_PHONE)),
                    clientEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_EMAIL)),
                    clientAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_ADDRESS)),
                    items = Gson().fromJson(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_JSON)),
                        Array<InvoiceItem>::class.java
                    ).toList(),
                    currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    taxRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_RATE)),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SUBTOTAL)),
                    taxAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_AMOUNT)),
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT))
                )
                invoices.add(invoice)
            }
            cursor.close()
            return invoices
        }

        fun addInvoice(invoice: Invoice): Long {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_ID, invoice.id)
                put(COLUMN_NAME, invoice.name)
                put(COLUMN_COMPANY_NAME, invoice.companyName)
                put(COLUMN_COMPANY_PHONE, invoice.companyPhone)
                put(COLUMN_COMPANY_EMAIL, invoice.companyEmail)
                put(COLUMN_COMPANY_ADDRESS, invoice.companyAddress)
                put(COLUMN_CLIENT_NAME, invoice.clientName)
                put(COLUMN_CLIENT_PHONE, invoice.clientPhone)
                put(COLUMN_CLIENT_EMAIL, invoice.clientEmail)
                put(COLUMN_CLIENT_ADDRESS, invoice.clientAddress)
                put(COLUMN_ITEMS_JSON, Gson().toJson(invoice.items))
                put(COLUMN_CURRENCY, invoice.currency)
                put(COLUMN_TAX_RATE, invoice.taxRate)
                put(COLUMN_CREATED_AT, invoice.createdAt)
                put(COLUMN_SUBTOTAL, invoice.subtotal)
                put(COLUMN_TAX_AMOUNT, invoice.taxAmount)
                put(COLUMN_TOTAL_AMOUNT, invoice.totalAmount)
            }
            return db.insert(TABLE_INVOICES, null, values)
        }

        fun deleteInvoice(invoiceId: String): Int {
            val db = writableDatabase
            return db.delete(
                TABLE_INVOICES,
                "$COLUMN_ID = ?",
                arrayOf(invoiceId)
            )
        }

        fun updateInvoice(invoice: Invoice): Int {
            val db = writableDatabase
            val values = ContentValues().apply {
                put(COLUMN_NAME, invoice.name)
                put(COLUMN_COMPANY_NAME, invoice.companyName)
                put(COLUMN_COMPANY_PHONE, invoice.companyPhone)
                put(COLUMN_COMPANY_EMAIL, invoice.companyEmail)
                put(COLUMN_COMPANY_ADDRESS, invoice.companyAddress)
                put(COLUMN_CLIENT_NAME, invoice.clientName)
                put(COLUMN_CLIENT_PHONE, invoice.clientPhone)
                put(COLUMN_CLIENT_EMAIL, invoice.clientEmail)
                put(COLUMN_CLIENT_ADDRESS, invoice.clientAddress)
                put(COLUMN_ITEMS_JSON, Gson().toJson(invoice.items))
                put(COLUMN_CURRENCY, invoice.currency)
                put(COLUMN_TAX_RATE, invoice.taxRate)
                put(COLUMN_SUBTOTAL, invoice.subtotal)
                put(COLUMN_TAX_AMOUNT, invoice.taxAmount)
                put(COLUMN_TOTAL_AMOUNT, invoice.totalAmount)
            }

            return db.update(
                TABLE_INVOICES,
                values,
                "$COLUMN_ID = ?",
                arrayOf(invoice.id)
            )
        }

        fun getInvoiceById(id: String): Invoice? {
            val db = readableDatabase
            val cursor = db.query(
                TABLE_INVOICES,
                null,
                "$COLUMN_ID = ?",
                arrayOf(id),
                null, null, null
            )

            return if (cursor.moveToFirst()) {
                Invoice(
                    id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ID)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)),
                    companyName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_NAME)),
                    companyPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_PHONE)),
                    companyEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_EMAIL)),
                    companyAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMPANY_ADDRESS)),
                    clientName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_NAME)),
                    clientPhone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_PHONE)),
                    clientEmail = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_EMAIL)),
                    clientAddress = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLIENT_ADDRESS)),
                    items = Gson().fromJson(
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ITEMS_JSON)),
                        Array<InvoiceItem>::class.java
                    ).toList(),
                    currency = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CURRENCY)),
                    taxRate = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_RATE)),
                    createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT)),
                    subtotal = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_SUBTOTAL)),
                    taxAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TAX_AMOUNT)),
                    totalAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_TOTAL_AMOUNT))
                )
            } else {
                null
            }.also { cursor.close() }
        }
    }
}