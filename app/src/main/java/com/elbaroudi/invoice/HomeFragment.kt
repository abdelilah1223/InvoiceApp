package com.elbaroudi.invoice

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.html2pdf.resolver.font.DefaultFontProvider
import com.itextpdf.io.font.PdfEncodings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment<Invoice : Any> : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var emptyText: TextView
    private lateinit var addButton: ImageButton
    private lateinit var mainContainer: LinearLayout

    private lateinit var adapter: InvoiceAdapter
    private var invoices = mutableListOf<AddFragment.Invoice>()
    private var bgColor = "#FFFFFF"
    private var language = "ar"
    private lateinit var prefs: SharedPreferences
    private lateinit var dbHelper: AddFragment.InvoiceDbHelper

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            showStoragePermissionDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHelper = AddFragment.InvoiceDbHelper(requireContext())
        initViews(view)
        initPrefs()
        loadSettings()
        loadInvoices()
        setupRecyclerView()
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        titleText = view.findViewById(R.id.titleText)
        emptyText = view.findViewById(R.id.emptyText)
        addButton = view.findViewById(R.id.addButton)
        mainContainer = view.findViewById(R.id.mainContainer)

        addButton.setOnClickListener {
            val fragment = AddFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun initPrefs() {
        prefs = requireActivity().getSharedPreferences("app_settings", MODE_PRIVATE)
    }

    private fun loadSettings() {
        bgColor = prefs.getString("bgColor", "#FFFFFF") ?: "#FFFFFF"
        language = prefs.getString("language", "ar") ?: "ar"
        updateUI()
    }

    private fun loadInvoices() {
        invoices = dbHelper.getAllInvoices().toMutableList()

        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }

        emptyText.visibility = if (invoices.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateUI() {
        mainContainer.setBackgroundColor(Color.parseColor(bgColor))

        val textColor = getTextColor(bgColor)
        titleText.setTextColor(textColor)
        emptyText.setTextColor(textColor)

        titleText.text = if (language == "ar") "الفواتير" else "Invoices"
        emptyText.text = if (language == "ar") "لا توجد فواتير" else "No invoices found"
    }

    private fun setupRecyclerView() {
        adapter = InvoiceAdapter(invoices) { invoice, action ->
            handleInvoiceAction(invoice, action)
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun handleInvoiceAction(invoice: AddFragment.Invoice, action: String) {
        when (action) {
            "edit" -> editInvoice(invoice)
            "delete" -> deleteInvoice(invoice)
            "export_json" -> exportAsJSON(invoice)
            "export_csv" -> exportAsCSV(invoice)
            "export_text" -> exportAsText(invoice)
            "export_pdf" -> exportAsPDF(invoice)
            "show_qr" -> showQRCode(invoice)
        }
    }

    private fun editInvoice(invoice: AddFragment.Invoice) {
        val fragment = AddFragment().apply {
            arguments = Bundle().apply {
                putString("invoice_id", invoice.id)
            }
        }
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun deleteInvoice(invoice: AddFragment.Invoice) {
        AlertDialog.Builder(requireContext())
            .setTitle(if (language == "ar") "تأكيد الحذف" else "Confirm Delete")
            .setMessage(if (language == "ar") "هل تريد حذف هذه الفاتورة؟" else "Do you want to delete this invoice?")
            .setPositiveButton(if (language == "ar") "حذف" else "Delete") { _, _ ->
                dbHelper.deleteInvoice(invoice.id)
                loadInvoices()
            }
            .setNegativeButton(if (language == "ar") "إلغاء" else "Cancel", null)
            .show()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun showStoragePermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(if (language == "ar") "إذن التخزين" else "Storage Permission")
            .setMessage(
                if (language == "ar")
                    "نحتاج إذن الوصول للملفات لحفظ الفواتير"
                else
                    "We need storage permission to save invoices"
            )
            .setPositiveButton("OK") { _, _ -> requestStoragePermission() }
            .setNegativeButton(if (language == "ar") "إلغاء" else "Cancel", null)
            .show()
    }

    private fun getAppDownloadsDir(): File {
        val downloadsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "InvoiceApp")
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return downloadsDir
    }

    private fun exportAsJSON(invoice: AddFragment.Invoice) {
        if (!checkStoragePermission()) {
            showStoragePermissionDialog()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonDir = File(getAppDownloadsDir(), "JSON")
                if (!jsonDir.exists()) jsonDir.mkdirs()

                val fileName = "${invoice.clientName}_${System.currentTimeMillis()}.json"
                val file = File(jsonDir, fileName)

                val json = Gson().toJson(invoice)
                file.writeText(json)

                withContext(Dispatchers.Main) {
                    showSuccessDialog("JSON", fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    private fun exportAsCSV(invoice: AddFragment.Invoice) {
        if (!checkStoragePermission()) {
            showStoragePermissionDialog()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val csvDir = File(getAppDownloadsDir(), "CSV")
                if (!csvDir.exists()) csvDir.mkdirs()

                val fileName = "${invoice.clientName}_${System.currentTimeMillis()}.csv"
                val file = File(csvDir, fileName)

                val csv = buildString {
                    append("Field,Value\n")
                    append("Client,${invoice.clientName}\n")
                    append("Company,${invoice.companyName}\n")
                    append("Total,${calculateTotal(invoice)}\n")
                    append("Currency,${invoice.currency}\n")
                    append("\nItems:\n")
                    append("Name,Quantity,Price,Total\n")
                    invoice.items.forEach { item ->
                        val itemTotal = item.quantity * item.unitPrice
                        append("${item.name},${item.quantity},${item.unitPrice},$itemTotal\n")
                    }
                }

                file.writeText(csv)

                withContext(Dispatchers.Main) {
                    showSuccessDialog("CSV", fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    private fun calculateTotal(invoice: AddFragment.Invoice): Double {
        var total = 0.0
        invoice.items.forEach { item ->
            total += item.quantity * item.unitPrice
        }
        return total * (1 + invoice.taxRate / 100)
    }

    private fun exportAsText(invoice: AddFragment.Invoice) {
        if (!checkStoragePermission()) {
            showStoragePermissionDialog()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {

                val textDir = File(getAppDownloadsDir(), "Text")
                if (!textDir.exists()) textDir.mkdirs()

                val fileName = "${invoice.clientName}_${System.currentTimeMillis()}.txt"
                val file = File(textDir, fileName)

                val text = buildString {
                    append("${if (language == "ar") "فاتورة" else "Invoice"}: ${invoice.clientName}\n")
                    append("${if (language == "ar") "العميل" else "Client"}: ${invoice.clientName}\n")
                    append("${if (language == "ar") "الشركة" else "Company"}: ${invoice.companyName}\n")
                    append("${if (language == "ar") "المجموع" else "Total"}: ${calculateTotal(invoice)} ${invoice.currency}\n")
                    append("\n${if (language == "ar") "العناصر" else "Items"}:\n")
                    invoice.items.forEach { item ->
                        append("${item.name} - ${item.quantity} x ${item.unitPrice}\n")
                    }
                }

                file.writeText(text)

                withContext(Dispatchers.Main) {
                    showSuccessDialog("Text", fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    // استبدل دالة exportAsPDF بالكامل بهذا الكود:

    private fun exportAsPDF(invoice: AddFragment.Invoice) {
        if (!checkStoragePermission()) {
            showStoragePermissionDialog()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdfDir = File(getAppDownloadsDir(), "PDF")
                if (!pdfDir.exists()) pdfDir.mkdirs()

                val fileName = "${invoice.clientName}_${System.currentTimeMillis()}.pdf"
                val file = File(pdfDir, fileName)

                val html = generatePDFHTML(invoice)

                // إعداد موفر الخطوط المبسط
                val fontProvider = DefaultFontProvider(true, true, true)

                // إضافة الخطوط العربية إذا توفرت
                try {
                    val assetManager = requireContext().assets

                    // محاولة إضافة خط عربي مخصص
                    try {
                        val arabicFontStream = assetManager.open("fonts/NotoSansArabic-Regular.ttf")
                        fontProvider.addFont(arabicFontStream.readBytes(), PdfEncodings.IDENTITY_H)
                        arabicFontStream.close()
                    } catch (e: Exception) {
                        // إذا لم يجد الخط المخصص، استخدم خطوط النظام
                        Log.d("PDF", "Custom Arabic font not found, using system fonts")
                    }

                    // إضافة خطوط النظام التي تدعم العربية
                    fontProvider.addFont("assets/fonts/", PdfEncodings.IDENTITY_H)

                } catch (e: Exception) {
                    Log.e("PDF", "Error loading fonts: ${e.message}")
                }

                // إعداد خصائص التحويل
                val converterProperties = ConverterProperties().apply {
                    charset = "UTF-8"
                    setFontProvider(fontProvider)
                    setBaseUri("file:///android_asset/")
                    // إضافة معالج CSS مخصص للنصوص العربية
                    setCssApplierFactory(com.itextpdf.html2pdf.css.apply.impl.DefaultCssApplierFactory())
                }

                // تحويل HTML إلى PDF
                FileOutputStream(file).use { fos ->
                    HtmlConverter.convertToPdf(html, fos, converterProperties)
                }

                withContext(Dispatchers.Main) {
                    showSuccessDialog("PDF", fileName)
                }

            } catch (e: Exception) {
                Log.e("PDF", "Error generating PDF: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    // استبدل دالة generatePDFHTML بهذه النسخة المحسنة:
    private fun generatePDFHTML(invoice: AddFragment.Invoice): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(invoice.createdAt))
        val isArabic = language == "ar"
        val total = calculateTotal(invoice)

        return """
<!DOCTYPE html>
<html dir="${if (isArabic) "rtl" else "ltr"}">
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <style>
        /* إعداد الخطوط مع fallback */
        body {
            font-family: ${if (isArabic)
            "'Noto Sans Arabic', 'Tahoma', 'Arial Unicode MS', sans-serif"
        else
            "'DejaVu Sans', 'Arial', sans-serif"};
            margin: 0;
            padding: 15px;
            color: #333;
            direction: ${if (isArabic) "rtl" else "ltr"};
            unicode-bidi: embed;
            line-height: 1.6;
        }
        
        /* كلاس خاص للنصوص العربية */
        .arabic {
            font-family: 'Noto Sans Arabic', 'Tahoma', 'Arial Unicode MS', sans-serif;
            direction: rtl;
            text-align: right;
            unicode-bidi: embed;
        }
        
        /* كلاس للأرقام واللغة الإنجليزية */
        .latin {
            font-family: 'DejaVu Sans', 'Arial', sans-serif;
            direction: ltr;
            text-align: left;
            unicode-bidi: embed;
            display: inline-block;
        }
        
        .container {
            max-width: 100%;
            background: white;
            page-break-inside: avoid;
        }
        
        .header {
            background: #2c5aa0;
            color: white;
            padding: 20px;
            text-align: center;
            margin-bottom: 20px;
        }
        
        .header h1 {
            margin: 0;
            font-size: 28px;
            font-weight: bold;
        }
        
        .invoice-info {
            margin: 10px 0;
            font-size: 14px;
        }
        
        .section {
            margin: 15px 0;
            page-break-inside: avoid;
        }
        
        .section-title {
            color: #2c5aa0;
            font-size: 18px;
            font-weight: bold;
            margin-bottom: 10px;
            padding-bottom: 5px;
            border-bottom: 2px solid #2c5aa0;
        }
        
        .info-grid {
            display: table;
            width: 100%;
            margin: 10px 0;
        }
        
        .info-row {
            display: table-row;
        }
        
        .info-cell {
            display: table-cell;
            width: 50%;
            padding: 10px;
            vertical-align: top;
        }
        
        .info-box {
            background: #f8f9fa;
            padding: 15px;
            border: 1px solid #e9ecef;
            border-radius: 5px;
            height: 100%;
            box-sizing: border-box;
        }
        
        .info-box h3 {
            color: #2c5aa0;
            margin: 0 0 10px 0;
            font-size: 16px;
        }
        
        .info-box p {
            margin: 5px 0;
            word-wrap: break-word;
        }
        
        .items-table {
            width: 100%;
            border-collapse: collapse;
            margin: 10px 0;
            border: 1px solid #ddd;
        }
        
        .items-table th {
            background: #2c5aa0;
            color: white;
            padding: 12px 8px;
            text-align: ${if (isArabic) "right" else "left"};
            border: 1px solid #2c5aa0;
            font-weight: bold;
        }
        
        .items-table td {
            padding: 10px 8px;
            border: 1px solid #ddd;
            text-align: ${if (isArabic) "right" else "left"};
            word-wrap: break-word;
        }
        
        .items-table tr:nth-child(even) {
            background: #f8f9fa;
        }
        
        /* خلايا الأرقام */
        .number-cell {
            text-align: right;
            direction: ltr;
            unicode-bidi: embed;
        }
        
        .total-section {
            margin: 20px 0;
            padding: 15px;
            background: #f8f9fa;
            border: 2px solid #2c5aa0;
            border-radius: 5px;
            text-align: ${if (isArabic) "right" else "left"};
        }
        
        .total-amount {
            font-size: 24px;
            font-weight: bold;
            color: #2c5aa0;
            margin-top: 10px;
        }
        
        .footer {
            text-align: center;
            margin-top: 30px;
            padding: 15px;
            border-top: 1px solid #ddd;
            color: #666;
        }
        
        /* تحسينات الطباعة */
        @media print {
            .container { 
                margin: 0; 
                box-shadow: none;
            }
            .header { 
                background: #2c5aa0 !important; 
                color: white !important;
                -webkit-print-color-adjust: exact;
            }
        }
    </style>
</head>
<body class="${if (isArabic) "arabic" else ""}">
    <div class="container">
        <!-- رأس الفاتورة -->
        <div class="header">
            <h1>${if (isArabic) "فاتورة" else "INVOICE"}</h1>
            <div class="invoice-info">
                <span>${if (isArabic) "رقم:" else "No:"} #${invoice.id}</span> |
                <span class="latin">${formattedDate}</span>
            </div>
        </div>
        
        <!-- معلومات الشركة والعميل -->
        <div class="section">
            <div class="info-grid">
                <div class="info-row">
                    <div class="info-cell">
                        <div class="info-box">
                            <h3>${if (isArabic) "معلومات الشركة" else "Company Information"}</h3>
                            <p><strong>${invoice.companyName}</strong></p>
                            <p class="latin">${invoice.companyPhone}</p>
                            <p class="latin">${invoice.companyEmail}</p>
                            <p>${invoice.companyAddress}</p>
                        </div>
                    </div>
                    <div class="info-cell">
                        <div class="info-box">
                            <h3>${if (isArabic) "معلومات العميل" else "Client Information"}</h3>
                            <p><strong>${invoice.clientName}</strong></p>
                            <p class="latin">${invoice.clientPhone}</p>
                            <p class="latin">${invoice.clientEmail}</p>
                            <p>${invoice.clientAddress}</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        
        <!-- جدول العناصر -->
        <div class="section">
            <div class="section-title">${if (isArabic) "تفاصيل الفاتورة" else "Invoice Details"}</div>
            <table class="items-table">
                <thead>
                    <tr>
                        <th>${if (isArabic) "العنصر" else "Item"}</th>
                        <th>${if (isArabic) "الكمية" else "Qty"}</th>
                        <th>${if (isArabic) "السعر" else "Price"}</th>
                        <th>${if (isArabic) "الإجمالي" else "Total"}</th>
                    </tr>
                </thead>
                <tbody>
                    ${invoice.items.joinToString("") { item ->
            val itemTotal = item.quantity * item.unitPrice
            """
                        <tr>
                            <td>${item.name}</td>
                            <td class="number-cell">${item.quantity}</td>
                            <td class="number-cell">${"%.2f".format(item.unitPrice)}</td>
                            <td class="number-cell">${"%.2f".format(itemTotal)}</td>
                        </tr>
                        """
        }}
                </tbody>
            </table>
        </div>
        
    
        <div class="total-section">
            <div>${if (isArabic) "إجمالي المبلغ المستحق" else "Total Amount Due"}</div>
            <div class="total-amount">
                <span class="latin">${"%.2f".format(total)} ${invoice.currency}</span>
            </div>
        </div>
        
        <!-- الخاتمة -->
        <div class="footer">
            <p>${if (isArabic) "شكراً لتعاملكم معنا" else "Thank you for your business!"}</p>
            <p><small>${if (isArabic) "تم إنشاؤها بواسطة تطبيق الفواتير" else "Generated by Invoice App"}</small></p>
        </div>
    </div>
</body>
</html>
""".trimIndent()
    }

    // إضافة هذه الدالة للمساعدة في debugging
    private fun logAvailableFonts() {
        try {
            val fontProvider = DefaultFontProvider(true, true, true)
            val availableFonts = fontProvider.fontSet.fonts
            Log.d("PDF_Fonts", "Available fonts: ${availableFonts.size}")
            availableFonts.forEachIndexed { index, font ->
                Log.d("PDF_Fonts", "Font $index: ${font}")
            }
        } catch (e: Exception) {
            Log.e("PDF_Fonts", "Error checking fonts: ${e.message}")
        }
    }
    private fun showQRCode(invoice: AddFragment.Invoice) {
        val qrData = "${if (language == "ar") "العميل" else "Client"}: ${invoice.clientName}\n" +
                "${if (language == "ar") "الشركة" else "Company"}: ${invoice.companyName}\n" +
                "${if (language == "ar") "المجموع" else "Total"}: ${calculateTotal(invoice)} ${invoice.currency}"

        val qrBitmap = generateQRCode(qrData)

        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_code, null)
        val qrImageView = dialogView.findViewById<ImageView>(R.id.qrImageView)
        val downloadButton = dialogView.findViewById<Button>(R.id.downloadButton)
        val shareButton = dialogView.findViewById<Button>(R.id.shareButton)

        qrImageView.setImageBitmap(qrBitmap)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle(if (language == "ar") "رمز الاستجابة السريعة" else "QR Code")
            .setNegativeButton(if (language == "ar") "إغلاق" else "Close", null)
            .create()

        downloadButton.setOnClickListener {
            saveQRCode(invoice, qrBitmap)
            dialog.dismiss()
        }

        shareButton.setOnClickListener {
            shareQRCode(invoice, qrBitmap)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun generateQRCode(text: String): Bitmap {
        val writer = QRCodeWriter()
        try {
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            return bitmap
        } catch (e: WriterException) {
            e.printStackTrace()
            return Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
        }
    }

    private fun saveQRCode(invoice: AddFragment.Invoice, bitmap: Bitmap) {
        if (!checkStoragePermission()) {
            showStoragePermissionDialog()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val qrDir = File(getAppDownloadsDir(), "QR_Codes")
                if (!qrDir.exists()) qrDir.mkdirs()

                val fileName = "${invoice.clientName}_QR_${System.currentTimeMillis()}.png"
                val file = File(qrDir, fileName)

                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()

                withContext(Dispatchers.Main) {
                    showSuccessDialog("QR Code", fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showErrorDialog()
                }
            }
        }
    }

    private fun shareQRCode(invoice: AddFragment.Invoice, bitmap: Bitmap) {
        try {
            val cachePath = File(requireContext().cacheDir, "images")
            cachePath.mkdirs()
            val file = File(cachePath, "${invoice.clientName}_QR.png")

            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, if (language == "ar") "مشاركة رمز QR" else "Share QR Code"))
        } catch (e: Exception) {
            showErrorDialog()
        }
    }

    private fun showSuccessDialog(type: String, fileName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(if (language == "ar") "تم بنجاح" else "Success")
            .setMessage(
                if (language == "ar")
                    "تم حفظ ملف  في مجلد التحميلات $type بنجاح:\n$fileName"
                else
                    " $type file saved successfully:\n$fileName  in download folder /incoiceApp"
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showErrorDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(if (language == "ar") "خطأ" else "Error")
            .setMessage(if (language == "ar") "فشل في حفظ الملف" else "Failed to save file")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun getTextColor(backgroundColor: String): Int {
        val color = backgroundColor.replace("#", "")
        val r = color.substring(0, 2).toInt(16)
        val g = color.substring(2, 4).toInt(16)
        val b = color.substring(4, 6).toInt(16)
        val brightness = (r * 299 + g * 587 + b * 114) / 1000
        return if (brightness > 128) Color.parseColor("#333333") else Color.WHITE
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        loadInvoices()
    }
}

class InvoiceAdapter(
    private val invoices: List<AddFragment.Invoice>,
    private val onAction: (AddFragment.Invoice, String) -> Unit
) : RecyclerView.Adapter<InvoiceAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView = view.findViewById(R.id.invoiceCard)
        val nameText: TextView = view.findViewById(R.id.invoiceNameText)
        val clientText: TextView = view.findViewById(R.id.clientText)
        val companyText: TextView = view.findViewById(R.id.companyText)
        val totalText: TextView = view.findViewById(R.id.totalText)
        val menuButton: ImageButton = view.findViewById(R.id.menuButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invoice, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val invoice = invoices[position]
        val context = holder.itemView.context
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "ar") ?: "ar"

        holder.nameText.text = invoice.name
        holder.clientText.text = "${if (language == "ar") "العميل" else "Client"}: ${invoice.clientName}"
        holder.companyText.text = "${if (language == "ar") "الشركة" else "Company"}: ${invoice.companyName}"
        holder.totalText.text = "${if (language == "ar") "المجموع" else "Total"}: ${invoice.totalAmount} ${invoice.currency}"

        holder.menuButton.setOnClickListener {
            showInvoiceMenu(holder.itemView.context, invoice)
        }

        holder.card.setOnClickListener {
            // Implement invoice details view if needed
        }
    }

    override fun getItemCount() = invoices.size

    private fun showInvoiceMenu(context: Context, invoice: AddFragment.Invoice) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val language = prefs.getString("language", "ar") ?: "ar"

        val options = if (language == "ar") {
            arrayOf("تعديل", "تصدير JSON", "تصدير CSV", "تصدير نص", "تصدير PDF", "عرض QR", "حذف")
        } else {
            arrayOf("Edit", "Export JSON", "Export CSV", "Export Text", "Export PDF", "Show QR", "Delete")
        }

        val actions = arrayOf("edit", "export_json", "export_csv", "export_text", "export_pdf", "show_qr", "delete")

        AlertDialog.Builder(context)
            .setTitle(if (language == "ar") "خيارات الفاتورة" else "Invoice Options")
            .setItems(options) { _, which ->
                onAction(invoice, actions[which])
            }
            .show()
    }
}