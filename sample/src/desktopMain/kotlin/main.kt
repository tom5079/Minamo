import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.loadImageFromFile

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var image: MinamoImage? by remember { mutableStateOf(null) }

        LaunchedEffect(Unit) {
            image = loadImageFromFile("/home/tom5079/Downloads/boiler-portrait-posterised-interlaced-256w.png.jxl")
        }

        image?.let {
            Box(Modifier.fillMaxSize().background(Color.Gray)) {
                Text("TestOK")
            }
        }
    }
}
