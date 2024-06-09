package xyz.quaver.minamo.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.aqua.MinamoImageView
import xyz.quaver.minamo.loadImageFromLocalUri

class MainActivity : ComponentActivity() {
    private var image: MinamoImage? by mutableStateOf(null)

    private val requestFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@registerForActivityResult

            image?.close()
            image = loadImageFromLocalUri(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            if (image != null) {
                BackHandler {
                    image?.close()
                    image = null
                }
            }

            MaterialTheme {
                if (image == null) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "*/*"
                            }

                            requestFileLauncher.launch(intent)
                        }
                    ) {
                    }
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            MinamoImageView(context)
                        },
                        update = { view ->
                            image?.let { view.setImage(it) }
                        }
                    )
                }
            }
        }
    }
}