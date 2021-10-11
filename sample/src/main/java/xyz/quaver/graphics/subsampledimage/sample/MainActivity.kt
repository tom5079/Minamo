package xyz.quaver.graphics.subsampledimage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import org.kodein.log.LoggerFactory
import org.kodein.log.newLogger
import xyz.quaver.graphics.subsampledimage.SubsampledImage
import xyz.quaver.graphics.subsampledimage.sample.ui.theme.SubSamplingImageViewTheme

class MainActivity : ComponentActivity() {

    val logger = newLogger(LoggerFactory.default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SubSamplingImageViewTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colors.background) {
                    resources.assets.open("card.png").use {
                        it.readBytes()
                    }.let {
                        logger.debug {
                            "Image ByteArray ${it.size} bytes"
                        }
                        SubsampledImage(modifier = Modifier.fillMaxSize(), image = it)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SubSamplingImageViewTheme {
        Greeting("Android")
    }
}