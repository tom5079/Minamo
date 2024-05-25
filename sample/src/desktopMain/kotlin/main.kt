import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.aqua.MinamoImagePanel
import xyz.quaver.minamo.loadImageFromFile

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        var image: MinamoImage? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            image = loadImageFromFile("/home/tom5079/Downloads/red-room.jxl")

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
            },
            update = { panel ->
                println("update")
                panel.revalidate()
                panel.setImage(image)
            }
        )
    }
}
