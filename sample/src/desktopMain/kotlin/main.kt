import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.window.singleWindowApplication
import kotlinx.coroutines.delay
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.aqua.MinamoImagePanel
import xyz.quaver.minamo.loadImageFromFile

fun main() = singleWindowApplication {
    var image: MinamoImage? by remember { mutableStateOf(null) }
    var scale by remember { mutableStateOf(1.0f) }

    DisposableEffect(Unit) {
        image =
            loadImageFromFile("/home/tom5079/Downloads/53648696062_3677f341b7_o.jpg")

        onDispose {
            image?.close()
        }
    }

    LaunchedEffect(scale) {
        scale = if (scale == 1.0f) 0.1f else 1.0f
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = {
            MinamoImagePanel().apply {
                setImage(image)
            }
        },
        update = {
            it.scale = scale
        }
    )
}