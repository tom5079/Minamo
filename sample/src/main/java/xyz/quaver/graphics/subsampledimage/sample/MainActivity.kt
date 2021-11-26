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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.kodein.log.LoggerFactory
import org.kodein.log.newLogger
import xyz.quaver.graphics.subsampledimage.ScaleTypes
import xyz.quaver.graphics.subsampledimage.SubSampledImage
import xyz.quaver.graphics.subsampledimage.rememberSubSampledImageState
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

                        val states = listOf(
                            rememberSubSampledImageState(ScaleTypes.FIT_WIDTH),
                            rememberSubSampledImageState(ScaleTypes.FIT_WIDTH),
                            rememberSubSampledImageState(ScaleTypes.FIT_WIDTH),
                            rememberSubSampledImageState(ScaleTypes.FIT_WIDTH),
                            rememberSubSampledImageState(ScaleTypes.FIT_WIDTH),
                        )

                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(states) { state ->
                                val height by produceState<Float?>(null, state.canvasSize, state.imageSize) {
                                    if (value != null) return@produceState

                                    state.canvasSize?.let { canvasSize ->
                                    state.imageSize?.let { imageSize ->
                                        value = imageSize.height * canvasSize.width / imageSize.width
                                    } }
                                }

                                SubSampledImage(modifier = Modifier
                                    .height(height?.let { with (LocalDensity.current) { it.toDp() } } ?: 128.dp)
                                    .fillMaxWidth(), image = it, state = state)
                            }
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