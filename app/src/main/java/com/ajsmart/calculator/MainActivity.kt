package com.ajsmart.calculator

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AJSmartCalculatorApp()
        }
    }
}

// --- DATA CLASSES ---
data class LedgerEntry(val date: String, val details: String, val crAmount: Double, val drAmount: Double)
data class CalcRow(val date: String, val sign: String, val amount: String, val detail: String)
data class HistoryItem(val id: Long, val expr: String, val result: String, val date: String, val rows: List<CalcRow>)

// --- LOCAL STORAGE MANAGER ---
object StorageManager {
    fun saveCRDR(context: Context, entries: List<LedgerEntry>) {
        val prefs = context.getSharedPreferences("AJ_PREFS", Context.MODE_PRIVATE)
        val data = entries.joinToString("|||") { "${it.date}::${it.details}::${it.crAmount}::${it.drAmount}" }
        prefs.edit().putString("CRDR_DATA", data).apply()
    }

    fun loadCRDR(context: Context): List<LedgerEntry> {
        val prefs = context.getSharedPreferences("AJ_PREFS", Context.MODE_PRIVATE)
        val data = prefs.getString("CRDR_DATA", "") ?: ""
        if (data.isEmpty()) return emptyList()
        return data.split("|||").mapNotNull {
            val parts = it.split("::")
            if (parts.size == 4) LedgerEntry(parts[0], parts[1], parts[2].toDoubleOrNull() ?: 0.0, parts[3].toDoubleOrNull() ?: 0.0) else null
        }
    }
}

// --- NATIVE TABLE PDF ENGINE ---
fun generateTablePdf(context: Context, uri: Uri, title: String, headers: List<String>, colX: FloatArray, tableData: List<List<String>>, footer: String) {
    try {
        val pdf = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        var page = pdf.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 22f; isFakeBoldText = true; color = android.graphics.Color.BLACK }
        val headerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.WHITE }
        val headerBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#5959DD") }
        val textPaint = Paint().apply { textSize = 12f; color = android.graphics.Color.DKGRAY }
        val linePaint = Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f }
        val footerPaint = Paint().apply { textSize = 14f; isFakeBoldText = true; color = android.graphics.Color.BLACK }

        var y = 60f
        canvas.drawText(title, 50f, y, titlePaint)
        y += 30f

        canvas.drawRect(45f, y - 18f, 550f, y + 8f, headerBgPaint)
        for (i in headers.indices) {
            canvas.drawText(headers[i], colX[i], y, headerPaint)
        }
        y += 25f

        for (row in tableData) {
            if (y > 780f) {
                pdf.finishPage(page)
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, 2).create()
                page = pdf.startPage(pageInfo)
                canvas = page.canvas
                y = 60f
            }
            
            for (i in row.indices) {
                val text = if (i == 1 && row[i].length > 30) row[i].take(27) + "..." else row[i]
                canvas.drawText(text, colX[i], y, textPaint)
            }
            y += 8f
            canvas.drawLine(50f, y, 545f, y, linePaint)
            y += 17f
        }

        y += 15f
        canvas.drawText(footer, 50f, y, footerPaint)

        pdf.finishPage(page)
        context.contentResolver.openOutputStream(uri)?.use { pdf.writeTo(it) }
        pdf.close()
        Toast.makeText(context, "PDF Saved Successfully!", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
    }
}

// --- MAIN APP UI ---
@Composable
fun AJSmartCalculatorApp() {
    var currentMode by remember { mutableStateOf("CALC") }
    var showDetails by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var dateOn by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val calcHistory = remember { mutableStateListOf<HistoryItem>() }
    val calcRows = remember { mutableStateListOf<CalcRow>() }
    val crdrEntries = remember { mutableStateListOf<LedgerEntry>().apply { addAll(StorageManager.loadCRDR(context)) } }

    var showPdfDialog by remember { mutableStateOf(false) }
    var pdfTitleInput by remember { mutableStateOf("") }
    var finalPdfTitle by remember { mutableStateOf("") }

    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val safeFileName = finalPdfTitle.ifBlank { "AJ_Report" }.replace(Regex("[^A-Za-z0-9 ]"), "").trim().replace(" ", "_") + ".pdf"
            
            if (currentMode == "CRDR") {
                val headers = listOf("Date", "Details", "CR", "DR")
                val colX = floatArrayOf(50f, 180f, 380f, 460f)
                val sortedEntries = crdrEntries.sortedBy { it.date }
                val tableData = sortedEntries.map { 
                    listOf(
                        it.date, 
                        it.details.ifEmpty { "-" }, 
                        if (it.crAmount > 0.0) "${it.crAmount}" else "", 
                        if (it.drAmount > 0.0) "${it.drAmount}" else ""
                    ) 
                }
                
                val totalCr = crdrEntries.sumOf { it.crAmount }
                val totalDr = crdrEntries.sumOf { it.drAmount }
                val bal = Math.abs(totalCr - totalDr)
                val type = if (totalCr >= totalDr) "CR" else "DR"
                val footer = "Total CR: Rs.$totalCr | Total DR: Rs.$totalDr | Balance: Rs.$bal $type"
                
                generateTablePdf(context, uri, finalPdfTitle, headers, colX, tableData, footer)
            } else {
                val headers = if (dateOn) listOf("Date", "Details", "Sign", "Amount") else listOf("Details", "Sign", "Amount")
                val colX = if (dateOn) floatArrayOf(50f, 140f, 380f, 450f) else floatArrayOf(50f, 350f, 420f)
                val sortedRows = calcRows.sortedBy { it.date }
                val tableData = sortedRows.map { 
                    if (dateOn) listOf(it.date, it.detail.ifEmpty { "-" }, it.sign, "${it.amount}") 
                    else listOf(it.detail.ifEmpty { "-" }, it.sign, "${it.amount}")
                }
                
                var total = 0.0
                calcRows.forEach { 
                    val amt = it.amount.toDoubleOrNull() ?: 0.0
                    if (it.sign == "-") total -= amt else total += amt 
                }
                val footer = "Final Total: Rs. $total"
                
                generateTablePdf(context, uri, finalPdfTitle, headers, colX, tableData, footer)
            }
        }
    }

    if (showPdfDialog) {
        AlertDialog(
            onDismissRequest = { showPdfDialog = false },
            title = { Text("PDF Heading & File Name", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("This name becomes the PDF heading and file name.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pdfTitleInput,
                        onValueChange = { pdfTitleInput = it },
                        placeholder = { Text("Example: Raj Account") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        finalPdfTitle = if (pdfTitleInput.isNotBlank()) pdfTitleInput else "AJ Smart Calculator"
                        pdfLauncher.launch("report.pdf")
                        showPdfDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5959DD))
                ) { Text("Save PDF") }
            },
            dismissButton = {
                TextButton(onClick = { showPdfDialog = false }) { Text("Cancel", color = Color.Gray) }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F5FA))) {
        Row(
            modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF5959DD), Color(0xFF8050EE)))).padding(12.dp, 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AJ Smart Calculator", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("📋", modifier = Modifier.padding(horizontal = 8.dp).clickable { showDetails = !showDetails; showHistory = false }, fontSize = 20.sp)
            Text("📅", modifier = Modifier.padding(horizontal = 8.dp).clickable { dateOn = !dateOn }, fontSize = 20.sp, color = if(dateOn) Color.White else Color(0x77FFFFFF))
            Text("📄", modifier = Modifier.padding(horizontal = 8.dp).clickable { 
                pdfTitleInput = ""
                showPdfDialog = true 
            }, fontSize = 20.sp)
            Text("🕘", modifier = Modifier.padding(horizontal = 8.dp).clickable { showHistory = !showHistory; showDetails = false }, fontSize = 20.sp)
        }

        Row(modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModeButton("CALCULATOR", currentMode == "CALC", Modifier.weight(1f)) { currentMode = "CALC"; showHistory = false }
            ModeButton("CR / DR", currentMode == "CRDR", Modifier.weight(1f)) { currentMode = "CRDR"; showHistory = false }
        }

        if (showHistory) {
            HistoryView(calcHistory) { showHistory = false }
        } else if (currentMode == "CALC") {
            CalculatorView(showDetails, dateOn, calcRows, calcHistory)
        } else {
            CRDRView(crdrEntries) { updatedList ->
                crdrEntries.clear()
                crdrEntries.addAll(updatedList)
                StorageManager.saveCRDR(context, crdrEntries)
            }
        }
    }
}

@Composable
fun ModeButton(text: String, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.height(44.dp).clip(RoundedCornerShape(9.dp)).background(if (isActive) Color(0xFFE9E8FF) else Color.Transparent).clickable { onClick() }, contentAlignment = Alignment.Center) {
        Text(text, color = if (isActive) Color(0xFF574BD2) else Color(0xFF687386), fontWeight = FontWeight.Bold)
    }
}

// --- CR/DR LOGIC ---
@Composable
fun CRDRView(entries: List<LedgerEntry>, onUpdate: (List<LedgerEntry>) -> Unit) {
    val context = LocalContext.current
    var amountInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CR") }
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%02d", dayOfMonth, month + 1, year % 100)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    val totalCr = entries.sumOf { it.crAmount }
    val totalDr = entries.sumOf { it.drAmount }
    val balance = Math.abs(totalCr - totalDr)
    val balanceType = if (totalCr >= totalDr) "CR" else "DR"

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BalanceCard("TOTAL CR", "₹$totalCr", Modifier.weight(1f))
            BalanceCard("TOTAL DR", "₹$totalDr", Modifier.weight(1f))
            BalanceCard("BALANCE", "₹$balance $balanceType", Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = amountInput, 
                    onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } }, 
                    label = { Text("Amount (Numbers only)") }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { selectedType = "CR" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "CR") Color(0xFF078D62) else Color.LightGray), modifier = Modifier.weight(1f)) { Text("CR") }
                    Button(onClick = { selectedType = "DR" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "DR") Color(0xFFD9495C) else Color.LightGray), modifier = Modifier.weight(1f)) { Text("DR") }
                }
                
                Box(modifier = Modifier.fillMaxWidth().clickable { datePickerDialog.show() }) {
                    OutlinedTextField(
                        value = selectedDate, onValueChange = {}, label = { Text("Date") }, enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.LightGray, disabledLabelColor = Color.Gray)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = detailInput, onValueChange = { detailInput = it }, label = { Text("Message / Details") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull()
                        if (amt != null) {
                            val cr = if (selectedType == "CR") amt else 0.0
                            val dr = if (selectedType == "DR") amt else 0.0
                            val newList = entries.toMutableList()
                            newList.add(LedgerEntry(selectedDate, detailInput, cr, dr))
                            onUpdate(newList)
                            amountInput = ""; detailInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5959DD))
                ) { Text("＋ Add Entry") }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries.sortedBy { it.date }.reversed()) { entry ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp).background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text(entry.date, fontSize = 10.sp, color = Color.Gray)
                        Text(entry.details.ifEmpty { "Entry" }, fontWeight = FontWeight.Bold)
                    }
                    Text(if (entry.crAmount > 0) "CR: ₹${entry.crAmount}" else "", fontWeight = FontWeight.Bold, color = Color(0xFF078D62), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    Text(if (entry.drAmount > 0) "DR: ₹${entry.drAmount}" else "", fontWeight = FontWeight.Bold, color = Color(0xFFD9495C), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }
        }
    }
}

@Composable
fun BalanceCard(title: String, amount: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 10.sp, color = Color.Gray)
        Text(amount, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

// --- CALCULATOR LOGIC ---
@Composable
fun CalculatorView(showDetails: Boolean, dateOn: Boolean, calcRows: MutableList<CalcRow>, calcHistory: MutableList<HistoryItem>) {
    val context = LocalContext.current
    var v by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var wait by remember { mutableStateOf(false) }
    var rowDetail by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%02d", dayOfMonth, month + 1, year % 100)
        },
        calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun addRow(sign: String, amount: String) {
        if (amount.isNotEmpty()) {
            calcRows.add(CalcRow(selectedDate, sign, amount, rowDetail))
        }
        rowDetail = ""
    }

    fun evaluate() {
        if (l.isEmpty() || operator.isEmpty() || v.isEmpty()) return
        if (showDetails) addRow(operator, v)
        
        val a = l.toDoubleOrNull() ?: 0.0
        val b = v.toDoubleOrNull() ?: 0.0
        var r = 0.0
        when (operator) {
            "+" -> r = a + b
            "-" -> r = a - b
            "*" -> r = a * b
            "/" -> if (b != 0.0) r = a / b
        }
        
        val resultStr = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
        val expr = "$l $operator $v"
        val currentDate = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date())
        
        calcHistory.add(0, HistoryItem(System.currentTimeMillis(), expr, resultStr, currentDate, calcRows.toList()))
        
        v = resultStr
        l = ""
        operator = ""
        wait = true
    }

    fun num(n: String) {
        if (wait) { v = ""; wait = false }
        if (n == "00" && v == "") v = "0"
        else if (v == "0") v = if (n == "00") "0" else n
        else v += n
    }

    fun opSet(o: String) {
        if (v.isEmpty() && l.isEmpty()) return
        if (l.isNotEmpty() && v.isNotEmpty() && operator.isNotEmpty()) {
            evaluate()
            l = v
            v = ""
            operator = o
            wait = false
            return
        }
        if (v.isNotEmpty()) {
            if (showDetails && l.isEmpty()) addRow(if(o=="-") "-" else "+", v)
            l = v
            v = ""
            wait = false
        }
        operator = o
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (showDetails) {
                Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(8.dp)) {
                    Text("Calculation Summary (${calcRows.size} entries)", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(calcRows.sortedBy { it.date }) { row ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(2f)) {
                                    if (dateOn) Text(row.date, fontSize = 10.sp, color = Color.Gray)
                                    Text(row.detail.ifEmpty { "Entry" }, color = Color.Gray)
                                }
                                Text("${row.sign} ${row.amount}", fontWeight = FontWeight.Bold, color = if (row.sign == "-") Color.Red else Color(0xFF078D62), modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (dateOn) {
                            Box(modifier = Modifier.weight(1f).clickable { datePickerDialog.show() }) {
                                OutlinedTextField(
                                    value = selectedDate, onValueChange = {}, label = { Text("Date") }, enabled = false,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(disabledTextColor = Color.Black, disabledBorderColor = Color.LightGray, disabledLabelColor = Color.Gray)
                                )
                            }
                        }
                        OutlinedTextField(
                            value = rowDetail, onValueChange = { rowDetail = it }, label = { Text("Details (Tea, milk...)") },
                            modifier = Modifier.weight(if (dateOn) 1.5f else 1f), singleLine = true
                        )
                    }
                }
            }
        }
        
        Column(modifier = Modifier.fillMaxWidth().height(110.dp).background(Color(0xFF111827)).padding(16.dp), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End) {
            Text("$l ${if(operator == "*") "×" else if(operator == "/") "÷" else operator}", color = Color(0xFFAEB8C8), fontSize = 20.sp)
            Text(if (v.isEmpty()) "0" else v, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
        }
        
        val keys = listOf(
            listOf(Triple("AC", "danger", { v = ""; l = ""; operator = ""; wait = false; calcRows.clear() }), Triple("⌫", "soft", { v = v.dropLast(1) }), Triple("%", "soft", { if(v.isNotEmpty()) v = (v.toDouble()/100).toString() }), Triple("÷", "op", { opSet("/") })),
            listOf(Triple("7", "normal", { num("7") }), Triple("8", "normal", { num("8") }), Triple("9", "normal", { num("9") }), Triple("×", "op", { opSet("*") })),
            listOf(Triple("4", "normal", { num("4") }), Triple("5", "normal", { num("5") }), Triple("6", "normal", { num("6") }), Triple("−", "op", { opSet("-") })),
            listOf(Triple("1", "normal", { num("1") }), Triple("2", "normal", { num("2") }), Triple("3", "normal", { num("3") }), Triple("+", "op", { opSet("+") })),
            listOf(Triple("00", "soft", { num("00") }), Triple("0", "normal", { num("0") }), Triple(".", "normal", { if(!v.contains(".")) v += "." }), Triple("=", "eq", { evaluate() }))
        )
        
        Column(modifier = Modifier.fillMaxWidth().height(380.dp).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in keys) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (key in row) { CalcButton(text = key.first, type = key.second, modifier = Modifier.weight(1f)) { key.third() } }
                }
            }
        }
    }
}

@Composable
fun HistoryView(history: List<HistoryItem>, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = onClose) { Text("Close") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyColumn {
            items(history) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(item.expr, color = Color.Gray)
                        Text("= ${item.result}", fontWeight = FontWeight.Black, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CalcButton(text: String, type: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = when (type) { "danger" -> Color(0xFFFFF0F2); "soft" -> Color(0xFFEDF1F6); "op" -> Color(0xFFEEEBFF); "eq" -> Color.Transparent; else -> Color.White }
    val textColor = when (type) { "danger" -> Color(0xFFD9495C); "op" -> Color(0xFF574BD3); "eq" -> Color.White; else -> Color(0xFF172033) }
    Box(
        modifier = modifier.fillMaxHeight().shadow(2.dp, RoundedCornerShape(13.dp))
            .background(if (type == "eq") Brush.linearGradient(listOf(Color(0xFF5959DD), Color(0xFF8050EE))) else androidx.compose.ui.graphics.SolidColor(bgColor), RoundedCornerShape(13.dp))
            .clip(RoundedCornerShape(13.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) { Text(text, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
}
