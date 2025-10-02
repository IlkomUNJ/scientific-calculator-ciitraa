package com.example.calculatorcitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.calculatorcitra.ui.theme.CalculatorCitraTheme
import com.example.calculatorcitra.ui.theme.Red40
import com.example.calculatorcitra.ui.theme.Orange40
import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import kotlin.math.*

object Screen {
    const val STANDARD = "standard_calculator"
    const val SCIENTIFIC = "scientific_calculator"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalculatorCitraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ){
                    CalculatorApp()
                }
            }
        }
    }
}

@Composable
fun CalculatorApp() {
    val navController = rememberNavController()
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var currentScreenRoute by remember { mutableStateOf(Screen.STANDARD) }
    
    val sin = object : Function("sin", 1) {
        override fun apply(vararg args: Double): Double = kotlin.math.sin(Math.toRadians(args[0]))
    }
    val cos = object : Function("cos", 1) {
        override fun apply(vararg args: Double): Double = kotlin.math.cos(Math.toRadians(args[0]))
    }
    val tan = object : Function("tan", 1) {
        override fun apply(vararg args: Double): Double = kotlin.math.tan(Math.toRadians(args[0]))
    }

    val asin = object : Function("asin", 1) {
        override fun apply(vararg args: Double): Double = Math.toDegrees(kotlin.math.asin(args[0]))
    }
    val acos = object : Function("acos", 1) {
        override fun apply(vararg args: Double): Double = Math.toDegrees(kotlin.math.acos(args[0]))
    }
    val atan = object : Function("atan", 1) {
        override fun apply(vararg args: Double): Double = Math.toDegrees(kotlin.math.atan(args[0]))
    }

    val factorial = object : Function("factorial", 1) {
        override fun apply(vararg args: Double): Double {
            val num = args[0].toInt()
            if (num < 0) throw IllegalArgumentException("Faktorial tidak terdefinisi untuk angka negatif.")
            if (num.toDouble() != args[0]) throw IllegalArgumentException("Faktorial hanya untuk bilangan bulat.")
            var res = 1.0
            for (i in 1..num) res *= i.toDouble()
            return res
        }
    }

    val handleButtonClick = { buttonValue: String ->
        when (buttonValue) {
            "sin", "cos", "tan", "log", "ln", "sqrt", "sin⁻¹", "cos⁻¹", "tan⁻¹" -> {
                if(result.isNotEmpty()) {
                    expression = ""
                    result = ""
                }
                expression += "$buttonValue("
            }
            in "0".."9", ".", "(", ")" -> {
                if(result.isNotEmpty()) {
                    expression = ""
                    result = ""
                }
                expression += buttonValue
            }
            "C" -> {
                expression = ""
                result = ""
            }
            "⌫" -> {
                if (result.isNotEmpty()){
                    expression = ""
                    result = ""
                } else if (expression.isNotEmpty()) {
                    expression = expression.dropLast(1)
                }
            }
            "+", "−", "x", "÷", "%", "^" -> {
                if(result.isNotEmpty()) {
                    expression = result
                    result = ""
                }
                if (expression.isNotEmpty() || buttonValue == "−") {
                    expression += buttonValue
                }
            }
            "x!" -> {
                if(result.isNotEmpty()) { expression = result; result = "" }
                expression += "!"
            }
            "1/x" -> {
                if(result.isNotEmpty()) { expression = ""; result = "" }
                expression += "1/"
            }
            "=" -> {
                if (expression.isNotEmpty()) {
                    try {
                        var sanitizedExpression = expression
                        val openParenCount = sanitizedExpression.count { it == '(' }
                        val closeParenCount = sanitizedExpression.count { it == ')' }
                        if (openParenCount > closeParenCount) {
                            sanitizedExpression += ")".repeat(openParenCount - closeParenCount)
                        }

                        sanitizedExpression = sanitizedExpression
                            .replace('x', '*')
                            .replace('÷', '/')
                            .replace('−', '-')
                            .replace("%", "/100")
                            .replace("sin⁻¹", "asin")
                            .replace("cos⁻¹", "acos")
                            .replace("tan⁻¹", "atan")

                        val factorialRegex = "(\\d+)!".toRegex()
                        sanitizedExpression = sanitizedExpression.replace(factorialRegex) { matchResult ->
                            "factorial(${matchResult.groupValues[1]})"
                        }
                        println("Ekspresi yang akan dihitung: $sanitizedExpression")

                        val expressionBuilder = ExpressionBuilder(sanitizedExpression)
                            .function(factorial)
                            .function(sin)
                            .function(cos)
                            .function(tan)
                            .function(asin)
                            .function(acos)
                            .function(atan)
                            .build()

                        val calculationResult = expressionBuilder.evaluate()

                        result = if (calculationResult == calculationResult.toLong().toDouble()) {
                            calculationResult.toLong().toString()
                        } else {
                            String.format("%.10f", calculationResult).trimEnd('0').trimEnd('.')
                        }
                    } catch (e: Exception) {
                        result = "Error"
                    }
                }
            }
            "⇄" -> {
                if (currentScreenRoute == Screen.STANDARD) {
                    navController.navigate(Screen.SCIENTIFIC)
                    currentScreenRoute = Screen.SCIENTIFIC
                } else {
                    navController.navigate(Screen.STANDARD)
                    currentScreenRoute = Screen.STANDARD
                }
            }
        }
    }

    NavHost(navController = navController, startDestination = Screen.STANDARD) {
        composable(Screen.STANDARD) {
            currentScreenRoute = Screen.STANDARD
            CalcStandardCitra(
                expressionText = expression,
                resultText = result,
                onButtonClick = handleButtonClick
            )
        }
        composable(Screen.SCIENTIFIC) {
            currentScreenRoute = Screen.SCIENTIFIC
            CalcSciCitra(
                expressionText = expression,
                resultText = result,
                onButtonClick = handleButtonClick
            )
        }
    }
}

@Composable
fun KalkButton(
    modifier: Modifier = Modifier,
    num: String,
    click: (String) -> Unit,
    fontSize: TextUnit = 30.sp,
    shape : Shape = RoundedCornerShape(10.dp)
) {
    val isOperator = num in listOf("=", "+", "−", "x", "÷", "⇄")

    val containerColor = when {
        isOperator -> Orange40
        num == "C" -> Red40    
        else -> MaterialTheme.colorScheme.surfaceVariant 
    }

    val contentColor = when {
        isOperator || num == "C" -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Button(
        onClick = { click(num) },
        modifier = modifier
            .padding(2.dp)
            .height(75.dp),
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Text(text = num, fontSize = fontSize, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CalcStandardCitra(
    expressionText: String,
    resultText: String,
    onButtonClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expressionText,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
                Text(
                    text = if (resultText.isNotEmpty()) resultText else "",
                    fontSize = 60.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        val buttons = listOf(
            listOf("C", "⌫", "%", "÷"),
            listOf("7", "8", "9", "x"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("⇄", "0", ".", "=")
        )

        buttons.forEach { rowButtons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                rowButtons.forEach { buttonText ->
                    val weight = 1f
                    val fontSize = if (buttonText in listOf("⌫", "%", "÷", "x", "−", "+", "=", "⇄")) 24.sp else 30.sp

                    KalkButton(
                        modifier = Modifier.weight(weight),
                        num = buttonText,
                        click = onButtonClick,
                        fontSize = fontSize
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Composable
fun CalcSciCitra(
    expressionText: String,
    resultText: String,
    onButtonClick: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expressionText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    maxLines = 2
                )
                Text(
                    text = if (resultText.isNotEmpty()) resultText else "",
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }

        val scientificButtons = listOf(
            listOf("sin", "cos", "tan", "(", ")"),
            listOf("sin⁻¹", "cos⁻¹", "tan⁻¹", "log", "ln"),
            listOf("C", "⌫", "%", "÷", "sqrt"),
            listOf("7", "8", "9", "x", "^"),
            listOf("4", "5", "6", "−", "x!"),
            listOf("1", "2", "3", "+", "1/x"),
            listOf("⇄", "0", ".", "=")
        )

        scientificButtons.forEach { rowButtons ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowButtons.forEach { buttonText ->
                    val weight = if (buttonText == "=") 2f else 1f
                    val fontSize = when(buttonText) {
                        "sin⁻¹", "cos⁻¹", "tan⁻¹" -> 18.sp
                        else -> 20.sp
                    }
                    if (buttonText.isNotEmpty()){
                        KalkButton(
                            modifier = Modifier.weight(weight),
                            num = buttonText,
                            click = onButtonClick,
                            fontSize = fontSize
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StandardPreview() {
    CalculatorCitraTheme {
        CalcStandardCitra(
            expressionText = "",
            resultText = "0",
            onButtonClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ScientificPreview() {
    CalculatorCitraTheme {
        CalcSciCitra(
            expressionText = "",
            resultText = "0",
            onButtonClick = {}
        )
    }
}