package com.ajsmart.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
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

// Data Classes
data class LedgerEntry(val amount: Double, val type: String, val details: String, val date: String)
data class HistoryItem(val expression: String, val result: String, val date: String)

@Composable
fun AJSmartCalculatorApp() {
    var currentMode by remember { mutableStateOf("CALC") }
    var showHistory by remember { mutableStateOf(false) }
    
    val calcHistory = remember { mutableStateListOf<HistoryItem>() }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF3F5FA))) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF5959DD), Color(0xFF8050EE))))
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("AJ Smart Calculator", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
            Text("📄", modifier = Modifier.padding(horizontal = 8.dp).clickable { /* PDF implementation next */ }, fontSize = 20.sp)
            Text("🕘", modifier = Modifier.padding(horizontal = 8.dp).clickable { showHistory = !showHistory }, fontSize = 20.sp)
        }

        // MODE SWITCHER
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeButton("CALCULATOR", currentMode == "CALC", Modifier.weight(1f)) { currentMode = "CALC"; showHistory = false }
            ModeButton("CR / DR", currentMode == "CRDR", Modifier.weight(1f)) { currentMode = "CRDR"; showHistory = false }
        }

        // VIEW ROUTING
        if (showHistory) {
            HistoryView(calcHistory) { showHistory = false }
        } else if (currentMode == "CALC") {
            CalculatorView(calcHistory)
        } else {
            CRDRView()
        }
    }
}

@Composable
fun ModeButton(text: String, isActive: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (isActive) Color(0xFFE9E8FF) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = if (isActive) Color(0xFF574BD2) else Color(0xFF687386), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun HistoryView(history: List<HistoryItem>, onClose: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Calculation History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray)) { Text("Close", color = Color.Black) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (history.isEmpty()) {
            Text("No history.", color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history) { item ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(item.expression, color = Color.Gray, fontSize = 16.sp)
                            Text("= ${item.result}", fontWeight = FontWeight.Black, fontSize = 28.sp, color = Color(0xFF172033))
                            Text(item.date, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorView(calcHistory: MutableList<HistoryItem>) {
    var expression by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf("") }

    // Native Math Parser (BODMAS Order of Operations)
    fun calculate(expr: String): String {
        try {
            val text = expr.replace("×", "*").replace("÷", "/").replace("−", "-").replace(" ", "")
            if (text.isEmpty()) return ""
            
            val regex = Regex("(?<=[-+*/])|(?=[-+*/])")
            val tokens = text.split(regex).filter { it.isNotBlank() }.toMutableList()
            
            // Handle negative numbers
            var i = 0
            while (i < tokens.size) {
                if ((i == 0 || tokens[i-1] in listOf("+","-","*","/")) && tokens[i] == "-") {
                    if (i + 1 < tokens.size) {
                        tokens[i] = "-" + tokens[i+1]
                        tokens.removeAt(i+1)
                    }
                }
                i++
            }

            // Multiply & Divide
            var j = 0
            while (j < tokens.size) {
                if (tokens[j] == "*" || tokens[j] == "/") {
                    if (j + 1 >= tokens.size) return "" // Incomplete expression
                    val a = tokens[j-1].toDouble()
                    val b = tokens[j+1].toDouble()
                    val res = if (tokens[j] == "*") a * b else a / b
                    tokens[j-1] = res.toString()
                    tokens.removeAt(j)
                    tokens.removeAt(j)
                    j--
                }
                j++
            }
            
            // Add & Subtract
            var k = 0
            while (k < tokens.size) {
                if (tokens[k] == "+" || tokens[k] == "-") {
                    if (k + 1 >= tokens.size) return ""
                    val a = tokens[k-1].toDouble()
                    val b = tokens[k+1].toDouble()
                    val res = if (tokens[k] == "+") a + b else a - b
                    tokens[k-1] = res.toString()
                    tokens.removeAt(k)
                    tokens.removeAt(k)
                    k--
                }
                k++
            }
            
            val finalRes = tokens[0].toDouble()
            return if (finalRes % 1.0 == 0.0) finalRes.toLong().toString() else finalRes.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    fun updatePreview() {
        preview = calculate(expression)
    }

    fun append(str: String) {
        val operators = listOf(" + ", " − ", " × ", " ÷ ")
        if (str in operators) {
            if (expression.isNotEmpty() && !expression.endsWith(" ")) {
                expression += str
            }
        } else {
            expression += str
        }
        updatePreview()
    }

    fun onEqual() {
        if (preview.isNotEmpty()) {
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
            calcHistory.add(0, HistoryItem(expression, preview, date))
            expression = preview
            preview = ""
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        
        // Display Area (Android Calculator Style)
        Column(
            modifier = Modifier.fillMaxWidth().height(160.dp).background(Color(0xFF111827)).padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = expression, 
                color = Color.White, 
                fontSize = if (expression.length > 15) 32.sp else 48.sp, 
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.End,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (preview.isNotEmpty()) "= $preview" else "", 
                color = Color(0xFFAEB8C8), 
                fontSize = 24.sp
            )
        }

        val keys = listOf(
            listOf(Triple("AC", "danger", { expression = ""; preview = "" }), Triple("⌫", "soft", { if(expression.isNotEmpty()) { expression = expression.dropLast(if (expression.endsWith(" ")) 3 else 1); updatePreview() } }), Triple("%", "soft", { /* Percentage logic requires advanced parsing */ }), Triple("÷", "op", { append(" ÷ ") })),
            listOf(Triple("7", "normal", { append("7") }), Triple("8", "normal", { append("8") }), Triple("9", "normal", { append("9") }), Triple("×", "op", { append(" × ") })),
            listOf(Triple("4", "normal", { append("4") }), Triple("5", "normal", { append("5") }), Triple("6", "normal", { append("6") }), Triple("−", "op", { append(" − ") })),
            listOf(Triple("1", "normal", { append("1") }), Triple("2", "normal", { append("2") }), Triple("3", "normal", { append("3") }), Triple("+", "op", { append(" + ") })),
            listOf(Triple("00", "soft", { append("00") }), Triple("0", "normal", { append("0") }), Triple(".", "normal", { append(".") }), Triple("=", "eq", { onEqual() }))
        )

        Column(modifier = Modifier.padding(8.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (row in keys) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (key in row) {
                        CalcButton(text = key.first, type = key.second, modifier = Modifier.weight(1f)) { key.third() }
                    }
                }
            }
        }
    }
}

@Composable
fun CRDRView() {
    var amountInput by remember { mutableStateOf("") }
    var detailInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("CR") }
    val entries = remember { mutableStateListOf<LedgerEntry>() }

    val totalCr = entries.filter { it.type == "CR" }.sumOf { it.amount }
    val totalDr = entries.filter { it.type == "DR" }.sumOf { it.amount }
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
                OutlinedTextField(value = amountInput, onValueChange = { amountInput = it }, label = { Text("Amount") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { selectedType = "CR" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "CR") Color(0xFF078D62) else Color.LightGray), modifier = Modifier.weight(1f)) { Text("CR") }
                    Button(onClick = { selectedType = "DR" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "DR") Color(0xFFD9495C) else Color.LightGray), modifier = Modifier.weight(1f)) { Text("DR") }
                }
                OutlinedTextField(value = detailInput, onValueChange = { detailInput = it }, label = { Text("Message / Details") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = {
                        amountInput.toDoubleOrNull()?.let { amt ->
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            entries.add(LedgerEntry(amt, selectedType, detailInput, date))
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
            items(entries) { entry ->
                Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp).padding(bottom = 4.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.date, fontSize = 10.sp, color = Color.Gray)
                        Text(entry.details, fontWeight = FontWeight.Bold)
                    }
                    Text("₹${entry.amount} ${entry.type}", fontWeight = FontWeight.Bold, color = if (entry.type == "CR") Color(0xFF078D62) else Color(0xFFD9495C))
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
