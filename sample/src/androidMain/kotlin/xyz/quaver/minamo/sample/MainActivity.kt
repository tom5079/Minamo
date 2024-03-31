package xyz.quaver.minamo.sample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import xyz.quaver.minamo.MinamoImage
import xyz.quaver.minamo.loadImageFromLocalUri

class MainActivity : ComponentActivity() {

    private var image: MinamoImage? by mutableStateOf(null)

    private val requestFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val uri = result.data?.data ?: return@registerForActivityResult

            image = loadImageFromLocalUri(this, uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val a = java.lang.Long.valueOf(3)
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                if (image == null) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                                addCategory(Intent.CATEGORY_OPENABLE)
                                type = "image/*"
                            }

                            requestFileLauncher.launch(intent)
                        }
                    ) {
                        Text(
                            "Choose an image ${a}"
                        )
                    }
                } else {
                    Text("Test")
                }
            }
        }
    }
}