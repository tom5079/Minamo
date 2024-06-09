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
        image =
            loadImageFromFile("/home/tom5079/Downloads/Telegram Desktop/_A_message_from_God_Washington,_D_C_,_March_12_The_Chairman_of_the.tif")

        onDispose {
            image?.close()
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = {
            MinamoImagePanel().apply {
                setImage(image)
            }
        }
    )
}