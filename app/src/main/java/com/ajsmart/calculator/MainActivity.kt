package com.ajsmart.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AJSmartCalculatorApp()
        }
    }
}

@Composable
fun AJSmartCalculatorApp() {
    var currentMode by remember { mutableStateOf("CALC") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F5FA))
    ) {
        // TOP HEADER (Gradient)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF5959DD), Color(0xFF8050EE))
                    )
                )
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AJ Smart Calculator",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
            // Simplified Toolbar for Native
            Text("📝", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 20.sp)
            Text("📅", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 20.sp)
            Text("🕘", modifier = Modifier.padding(horizontal = 8.dp), fontSize = 20.sp)
        }

        // MODE SWITCHER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ModeButton(
                text = "CALCULATOR", 
                isActive = currentMode == "CALC", 
                modifier = Modifier.weight(1f)
            ) { currentMode = "CALC" }
            
            ModeButton(
                text = "CR / DR", 
                isActive = currentMode == "CRDR", 
                modifier = Modifier.weight(1f)
            ) { currentMode = "CRDR" }
        }

        // MAIN CONTENT
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
        Text(
            text = text,
            color = if (isActive) Color(0xFF574BD2) else Color(0xFF687386),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun CalculatorView() {
    var v by remember { mutableStateOf("") }
    var l by remember { mutableStateOf("") }
    var operator by remember { mutableStateOf("") }
    var wait by remember { mutableStateOf(false) }

    fun num(n: String) {
        if (wait) { v = ""; wait = false }
        if (n == "00" && v == "") v = "0"
        else if (v == "0") v = if (n == "00") "0" else n
        else v += n
    }

    fun opSet(o: String) {
        if (v.isEmpty() && l.isEmpty()) return
        if (v.isNotEmpty()) {
            l = v
            v = ""
            wait = false
        }
        operator = o
    }

    fun equal() {
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
        // Remove trailing zero if whole number
        v = if (r % 1.0 == 0.0) r.toLong().toString() else r.toString()
        l = ""
        operator = ""
        wait = true
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // DISPLAY AREA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .background(Color(0xFF111827))
                .padding(16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "$l ${if(operator == "*") "×" else if(operator == "/") "÷" else operator}",
                color = Color(0xFFAEB8C8),
                fontSize = 20.sp
            )
            Text(
                text = if (v.isEmpty()) "0" else v,
                color = Color.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black
            )
        }

        // KEYPAD
        val keys = listOf(
            listOf(Triple("AC", "danger", { v = ""; l = ""; operator = ""; wait = false }), Triple("⌫", "soft", { v = v.dropLast(1) }), Triple("%", "soft", { if(v.isNotEmpty()) v = (v.toDouble()/100).toString() }), Triple("÷", "op", { opSet("/") })),
            listOf(Triple("7", "normal", { num("7") }), Triple("8", "normal", { num("8") }), Triple("9", "normal", { num("9") }), Triple("×", "op", { opSet("*") })),
            listOf(Triple("4", "normal", { num("4") }), Triple("5", "normal", { num("5") }), Triple("6", "normal", { num("6") }), Triple("−", "op", { opSet("-") })),
            listOf(Triple("1", "normal", { num("1") }), Triple("2", "normal", { num("2") }), Triple("3", "normal", { num("3") }), Triple("+", "op", { opSet("+") })),
            listOf(Triple("00", "soft", { num("00") }), Triple("0", "normal", { num("0") }), Triple(".", "normal", { if(!v.contains(".")) v += "." }), Triple("=", "eq", { equal() }))
        )

        Column(
            modifier = Modifier.padding(8.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (row in keys) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (key in row) {
                        CalcButton(text = key.first, type = key.second, modifier = Modifier.weight(1f)) {
                            key.third()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CRDRView() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("CR / DR Mode Interface", fontWeight = FontWeight.Bold, color = Color.Gray)
        Text("(Layout active. Logic requires local database integration next.)", fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun CalcButton(text: String, type: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bgColor = when (type) {
        "danger" -> Color(0xFFFFF0F2)
        "soft" -> Color(0xFFEDF1F6)
        "op" -> Color(0xFFEEEBFF)
        "eq" -> Color.Transparent // Handled by gradient
        else -> Color.White
    }
    
    val textColor = when (type) {
        "danger" -> Color(0xFFD9495C)
        "op" -> Color(0xFF574BD3)
        "eq" -> Color.White
        else -> Color(0xFF172033)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .shadow(2.dp, RoundedCornerShape(13.dp))
            .background(
                if (type == "eq") Brush.linearGradient(listOf(Color(0xFF5959DD), Color(0xFF8050EE))) 
                else androidx.compose.ui.graphics.SolidColor(bgColor),
                RoundedCornerShape(13.dp)
            )
            .clip(RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}
