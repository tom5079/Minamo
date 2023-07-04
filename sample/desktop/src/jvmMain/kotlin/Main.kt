import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import xyz.quaver.subsampledimage.common.App


fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        App()
    }
}
