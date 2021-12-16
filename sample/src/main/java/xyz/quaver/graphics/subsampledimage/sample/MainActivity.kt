/*
 * Copyright 2021 tom5079
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.quaver.graphics.subsampledimage.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.kodein.log.LoggerFactory
import org.kodein.log.newLogger
import xyz.quaver.graphics.subsampledimage.*
import xyz.quaver.graphics.subsampledimage.sample.ui.theme.SubSamplingImageViewTheme

class MainActivity : ComponentActivity() {

    val logger = newLogger(LoggerFactory.default)

    @ExperimentalMaterialApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val imageSource = resources.assets.open("card.png").use { rememberInputStreamImageSource(it) }

            val states = remember {
                List(5) { _ ->
                    SubSampledImageState(ScaleTypes.FIT_WIDTH, Bounds.FORCE_OVERLAP_OR_CENTER)
                }
            }

            var isGestureEnabled by remember { mutableStateOf(false) }

            LaunchedEffect(isGestureEnabled) {
                states.forEach {
                    it.isGestureEnabled = isGestureEnabled
                }
            }

            SubSamplingImageViewTheme {
                BottomSheetScaffold(
                    sheetContent = {
                        Card(Modifier.fillMaxWidth(), elevation = 4.dp) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(32.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Enable Gestures")
                                    Switch(isGestureEnabled, onCheckedChange = {
                                        isGestureEnabled = it
                                    })
                                }
                            }
                        }
                    }, sheetPeekHeight = 32.dp
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(states) { state ->
                            val height by produceState<Float?>(null, state.canvasSize, state.imageSize) {
                                if (value != null) return@produceState

                                state.canvasSize?.let { canvasSize ->
                                    state.imageSize?.let { imageSize ->
                                        value = imageSize.height * canvasSize.width / imageSize.width
                                    } }
                            }

                            SubSampledImage(
                                modifier = Modifier
                                    .height(height?.let { with(LocalDensity.current) { it.toDp() } }
                                        ?: 128.dp)
                                    .fillMaxWidth(),
                                imageSource = imageSource,
                                state = state)
                        }
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