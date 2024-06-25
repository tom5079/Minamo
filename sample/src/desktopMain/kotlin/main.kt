import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.window.singleWindowApplication
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.aqua.MinamoImagePanel
import xyz.quaver.minamo.loadImageFromFile

fun main() = singleWindowApplication {
    var image: MinamoImage? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        image = loadImageFromFile("/home/tom5079/Downloads/53648696062_3677f341b7_o.jpg")
            .onFailure { it.printStackTrace() }
            .getOrNull()

        onDispose {
            image?.close()
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = {
            MinamoImagePanel().apply {
                onErrorListener = {
                    it.printStackTrace()
                }
                setImage(image)
            }
        }
    )
}