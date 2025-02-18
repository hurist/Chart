package com.hvi2ist.chart

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hvi2ist.chart.databinding.ActivityMainBinding
import com.hvi2ist.chart.ui.theme.ChartTheme
import com.hvi2ist.chartlib.BarChart.Companion.BarData
import com.hvi2ist.chartlib.HealthRangeView
import com.hvi2ist.chartlib.LineChart
import com.hvi2ist.chartlib.SimpleLineChart
import com.hvi2ist.chartlib.TimeLineData
import java.text.SimpleDateFormat
import java.util.Date
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
                SimpleLineChart.ChartData("11", SimpleLineChart.ChartData.NO_VALUE),
                SimpleLineChart.ChartData("12", SimpleLineChart.ChartData.NO_VALUE),
                SimpleLineChart.ChartData("13", SimpleLineChart.ChartData.NO_VALUE),
            ),
            1
        )

        val data = mutableListOf<BarData>()
        repeat(31) {
            BarData("0$it", Random.nextInt(0, 1000).toFloat(), Random.nextInt(0, 1000).toFloat()).also { data.add(it) }
        }
        // 如果data的大小超过7，label要控制间隔，间隔中间的label不显示

        data.add(BarData("0", 100f, 1f))
        data.add(BarData("0", 10f, 10f))
        binding.barChart.setData(
            data,
            1000f
        )
        binding.barChart.setOnTouchBarListener { infoView, barData, index ->
            /*infoView.findViewById<TextView>(com.hvi2ist.chartlib.R.id.root).text = """
                total: ${barData.value + barData.value2}
                value: ${barData.value}
                value2: ${barData.value2}
            """.trimIndent()*/
        }

        val lineData = mutableListOf<LineChart.LineData>()

        /*repeat(10) {
            LineChart.LineData("0$it", Random.nextInt(0, 100).toFloat()).also { lineData.add(it) }
        }*/
        lineData.add(LineChart.LineData("0", LineChart.NO_VALUE))
        lineData.add(LineChart.LineData("1", LineChart.NO_VALUE))
        lineData.add(LineChart.LineData("2", 20f))
        lineData.add(LineChart.LineData("3", 30f))
        lineData.add(LineChart.LineData("4", 0f))
        lineData.add(LineChart.LineData("5", 75f))
        lineData.add(LineChart.LineData("6", LineChart.NO_VALUE))
        lineData.add(LineChart.LineData("7", 110f))
        lineData.add(LineChart.LineData("8", 33.6f))
        lineData.add(LineChart.LineData("9", LineChart.NO_VALUE))
        lineData.add(LineChart.LineData("10", LineChart.NO_VALUE))

        binding.lineChart.setData(
            lineData,
            listOf(
                LineChart.RangeColor(0f to 25f, Color.RED),
                LineChart.RangeColor(25f to 50f, Color.YELLOW),
                LineChart.RangeColor(50f to 75f, Color.BLACK),
                LineChart.RangeColor(75f to 100f, Color.BLUE)
            )
        )
        binding.lineChart.setOnTouchBarListener { infoView, barData, index ->
            infoView.findViewById<TextView>(com.hvi2ist.chartlib.R.id.root).text = barData.value.toString()
        }

        val sleepData = listOf(
            TimeLineData("2025-01-01", "三", "2024-12-31 23:45:12", "2025-01-01 08:15:45"),
            TimeLineData("2025-01-02", "四", "2025-01-01 22:30:35", "2025-01-02 07:50:20"),
            TimeLineData("2025-01-03", "五", "2025-01-02 00:00:05", "2025-01-02 12:00:10"),
            TimeLineData("2025-01-04", "六", "2025-01-03 23:10:18", "2025-01-04 08:05:33"),
            TimeLineData("2025-01-05", "日", "2025-01-04 22:20:42", "2025-01-05 07:40:55"),
            TimeLineData("2025-01-06", "一", "2025-01-05 23:25:50", "2025-01-06 08:10:14"),
            TimeLineData("2025-01-07", "二", "2025-01-07 10:40:28", "2025-01-07 22:20:03")
        )
        binding.timeLineChart.setData(sleepData)
        binding.timeLineChart.setOnTouchBarListener { infoView, barData, index, isStart ->
            binding.timeLineChart.post {
                infoView.findViewById<TextView>(com.hvi2ist.chartlib.R.id.root).text = barData.toString()
            }
        }
        val sf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val str = sf.format(Date())
        Log.d(TAG, "onCreate: $str")

        binding.healthRangeView.setData(
            HealthRangeView.Data(
                40..59,
                60..69,
                70..77,
                78..89,
                30
            )
        )
    }

    companion object {
        const val TAG = "MainActivity"
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