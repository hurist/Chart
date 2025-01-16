package com.hvi2ist.chart

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
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
import com.hvi2ist.chartlib.BarChart
import com.hvi2ist.chartlib.BarChart.Companion.BarData
import com.hvi2ist.chartlib.SimpleLineChart
import com.hvi2ist.chartlib.SimpleLineChart.Companion.ChartData
import kotlin.random.Random

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

        val data = mutableListOf<BarData>()
        repeat(31) {
            BarData("0$it", Random.nextInt(0, 2000)).also { data.add(it) }
        }
        // 如果data的大小超过7，label要控制间隔，间隔中间的label不显示


        binding.barChart.setData(
            data,
            2000,
            1000
        )
        binding.barChart.setOnTouchBarListener { infoView, barData ->
            infoView.findViewById<TextView>(com.hvi2ist.chartlib.R.id.root).text = barData.value.toString()
        }

        //binding.barChart.childInfoView = LayoutInflater.from(this).inflate(com.hvi2ist.chartlib.R.layout.layout_test, null, false)

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