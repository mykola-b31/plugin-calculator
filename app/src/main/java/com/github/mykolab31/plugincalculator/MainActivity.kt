package com.github.mykolab31.plugincalculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SmokeTestScreen()
                }
            }
        }
    }
}

/**
 * Тимчасовий екран для перевірки, що Compose та LuaJ
 * коректно підключені та працюють разом.
 *
 * Виконує простий Lua-вираз "2 + 2" та відображає результат.
 * Якщо на екрані з'явиться "Lua result: 4" — все працює.
 */
@Composable
fun SmokeTestScreen() {
    // Виконуємо тестовий Lua-скрипт один раз при першому рендері
    val luaResult = remember {
        try {
            runLuaSmokeTest()
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Calculator Plugin Engine")
        Text(text = "Lua smoke test:")
        Text(text = luaResult)
    }
}

/**
 * Виконує найпростіший Lua-вираз через LuaJ та повертає
 * результат у вигляді рядка для відображення в UI.
 */
private fun runLuaSmokeTest(): String {
    val globals = JsePlatform.standardGlobals()
    val chunk = globals.load("return 2 + 2")
    val result: LuaValue = chunk.call()
    return "Lua result: ${result.toString()}"
}

@Preview(showBackground = true)
@Composable
fun SmokeTestScreenPreview() {
    MaterialTheme {
        Surface {
            SmokeTestScreen()
        }
    }
}