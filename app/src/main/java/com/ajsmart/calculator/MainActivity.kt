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

// Data Class for CR/DR
data class LedgerEntry(val amount: Double, val type: String, val details: String, val date: String)

@Composable
fun AJSmartCalculatorApp() {
    var currentMode by remember { mutableStateOf("CALC") }

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
        }

        // MODE SWITCHER
        Row(
            modifier = Modifier.fillMaxWidth().background(Color.White).padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeButton("CALCULATOR", currentMode == "CALC", Modifier.weight(1f)) { currentMode = "CALC" }
            ModeButton("CR / DR", currentMode == "CRDR", Modifier.weight(1f)) { currentMode = "CRDR" }
        }

        if (currentMode == "CALC") {
            CalculatorView()
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
        // Balances
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BalanceCard("TOTAL CR", "₹$totalCr", Modifier.weight(1f))
            BalanceCard("TOTAL DR", "₹$totalDr", Modifier.weight(1f))
            BalanceCard("BALANCE", "₹$balance $balanceType", Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Form
        Card(
            modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = amountInput, 
                    onValueChange = { amountInput = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(onClick = { selectedType = "CR" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "CR") Color(0xFF078D62) else Color.LightGray), modifier = Modifier.weight(1f)) { Text("CR") }
                    Button(onClick = { selectedType = "DR" }, colors = ButtonDefaults.buttonColors(containerColor = if (selectedType == "DR") Color(0xFFD9495C) else Color.LightGray), modifier = Modifier.weight(1f)) { Text("DR") }
                }
                OutlinedTextField(
                    value = detailInput, 
                    onValueChange = { detailInput = it },
                    label = { Text("Message / Details") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull()
                        if (amt != null) {
                            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            entries.add(LedgerEntry(amt, selectedType, detailInput, date))
                            amountInput = ""
                            detailInput = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5959DD))
                ) { Text("＋ Add Entry") }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(entries) { entry ->
                Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(12.dp).padding(bottom = 4.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.date, fontSize = 10.sp, color = Color.Gray)
                        Text(entry.details, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "₹${entry.amount} ${entry.type}",
                        fontWeight = FontWeight.Bold,
                        color = if (entry.type == "CR") Color(0xFF078D62) else Color(0xFFD9495C)
                    )
                }
            }
        }
    }
}

@Composable
fun BalanceCard(title: String, amount: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color.White, RoundedCornerShape(8.dp)).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 10.sp, color = Color.Gray)
        Text(amount, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun CalculatorView() {
    var v by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var wait by remember { mutableStateOf(false) }

    fun evaluate() {
        if (l.isEmpty() || operator.isEmpty() || v.isEmpty()) return
        val a = l.toDoubleOrNull() ?: 0.0
        val b = v.toDoubleOrNull() ?: 0.0
        var r = 0.0
        when (operator) {
            "+" -> r = a + b
            "-" -> r = a - b
            "*" -> r = a * b
            "/" -> if (b != 0.0) r = a / b
        }
        v = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
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
        
        // This fixes the chaining bug (e.g. 100 + 20 + 30)
        if (l.isNotEmpty() && v.isNotEmpty() && operator.isNotEmpty()) {
            evaluate()
            l = v
            v = ""
            operator = o
            wait = false
            return
        }
        
        if (v.isNotEmpty()) {
            l = v
            v = ""
            wait = false
        }
        operator = o
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxWidth().height(130.dp).background(Color(0xFF111827)).padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text("$l ${if(operator == "*") "×" else if(operator == "/") "÷" else operator}", color = Color(0xFFAEB8C8), fontSize = 20.sp)
            Text(if (v.isEmpty()) "0" else v, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Black)
        }

        val keys = listOf(
            listOf(Triple("AC", "danger", { v = ""; l = ""; operator = ""; wait = false }), Triple("⌫", "soft", { v = v.dropLast(1) }), Triple("%", "soft", { if(v.isNotEmpty()) v = (v.toDouble()/100).toString() }), Triple("÷", "op", { opSet("/") })),
            listOf(Triple("7", "normal", { num("7") }), Triple("8", "normal", { num("8") }), Triple("9", "normal", { num("9") }), Triple("×", "op", { opSet("*") })),
            listOf(Triple("4", "normal", { num("4") }), Triple("5", "normal", { num("5") }), Triple("6", "normal", { num("6") }), Triple("−", "op", { opSet("-") })),
            listOf(Triple("1", "normal", { num("1") }), Triple("2", "normal", { num("2") }), Triple("3", "normal", { num("3") }), Triple("+", "op", { opSet("+") })),
            listOf(Triple("00", "soft", { num("00") }), Triple("0", "normal", { num("0") }), Triple(".", "normal", { if(!v.contains(".")) v += "." }), Triple("=", "eq", { evaluate() }))
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
