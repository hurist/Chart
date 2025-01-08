package com.hvi2ist.chart

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hvi2ist.chart.databinding.ActivityMainBinding
import com.hvi2ist.chart.ui.theme.ChartTheme
import com.hvi2ist.chartlib.SimpleLineChart
import com.hvi2ist.chartlib.SimpleLineChart.Companion.ChartData

class MainActivity : ComponentActivity() {

    private lateinit var binding : ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.simpleLineChart.setData(
            listOf(
                ChartData("11", 10),
                ChartData("12", ChartData.NO_VALUE),
                ChartData("13", 10),
            ),
            1
        )
        /*setContent {
            ChartTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }*/
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ChartTheme {
        Greeting("Android")
    }
}