# SubSampledImage

Image Composable that supports Zooming/Panning and Dynamic Image Loading/Unloading with SubSampling  
Built for Jetpack Compose :rocket: with Kotlin and :heart:  

Possible replacement for [davemorrissey/subsampling-scale-image-view](https://github.com/davemorrissey/subsampling-scale-image-view)

# Features

- Dynamic partial image loading/unloading based on the tile visibility (Supports wide/large image)
- Zooming and panning out of the box
- Supports all image types supported by [BigmapRegionDecoder](https://developer.android.com/reference/android/graphics/BitmapRegionDecoder)
- Currently supports ByteArray and InputStream & custom imagesources
- Supports automatic height adjustment based on the content
- Supports animated image zooming/panning

# Documentation

## Getting Started

Add MavenCentral Snapshot repository and add dependency
```kotlin
repositories {
  maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
  implementation("xyz.quaver:subsampledimage:0.0.1-alpha19-SNAPSHOT")
}
...


```

Add following line to your Composable  

```kotlin
SubSampledImage(modifier = modifier, imageSource = imageSource)
```

## ImageSource

Currently supports ByteArray and InputStream

for ByteArray Images:
```kotlin
rememberByteArrayImageSource(image)
```

for InputStream Images:
```kotlin
rememberInputStreamImageSource(inputStream)
```
It is safe to close the stream after calling this function.

... Or Implement your own ImageSource
```kotlin
interface ImageSource {
    val imageSize: Size
    fun decodeRegion(region: Rect, sampleSize: Int): ImageBitmap
}
```

## Manipulating States

To change Reset behavior, Image position, Zoom level or to get information about the state of the composable, use [SubSampledImageState](https://github.com/tom5079/SubSampledImage/blob/master/library/src/main/java/xyz/quaver/graphics/subsampledimage/SubSampledImageState.kt)

```kotlin
val subSampledImageState = rememberSubSampledImageState(scaleType, bound)
SubSampledImage(modifier = modifier, image = image, state = state)
```

### Three Important Rectangles

![](https://github.com/tom5079/SubSampledImage/blob/master/docs/images/state.jpg)
The position of the displayed image is determined based on three rectangles, `canvasSize`, `imageRect`, `imageSize`

Image is projected onto `imageRect`, and the portion that overlaps `canvasSize` gets shown in the Composable  
The size and number of the tiles dynamically change as the zoom level changes  
and the tiles automatically loads and unloads Image based on whether or not it overlaps the canvasSize to save memory use

### ScaleType
```kotlin
typealias ScaleType = (canvasSize: Size, imageSize: Size) -> Rect
```

`ScaleType` determins `imageRect` when the Composable is first initialized or reset.  

Predefined `ScaleType` can be found in object `ScaleTypes`  

Available ScaleTypes:  
|ScaleType|Visualization|Description|
|-|-|-|
|CENTER|![](https://github.com/tom5079/SubSampledImage/blob/master/docs/images/CENTER.jpg?raw=true)|Centers `imageRect` without resizing|
|CENTER_INSIDE|![](https://github.com/tom5079/SubSampledImage/blob/master/docs/images/CENTER_INSIDE_WIDTH.jpg?raw=true) ![](https://github.com/tom5079/SubSampledImage/blob/master/docs/images/CENTER_INSIDE_HEIGHT.jpg?raw=true)|Centers `imageRect` inside the `canvasSize` and resizes until one side of the image touches the border of `canvasSize`|
|FIT_WIDTH|![](https://github.com/tom5079/SubSampledImage/blob/master/docs/images/FIT_WIDTH.jpg?raw=true)|Fits width of `imageRect` to width of `canvasSize`|
|FIT_XY|![](https://github.com/tom5079/SubSampledImage/blob/master/docs/images/FIT_XY.jpg?raw=true)|Fits `imageRect` inside `canvasSize`  (:warning: changes aspect ratio)|

Open Issues/Pull Requests for more ScaleTypes or Implement your own function

### Bound
```kotlin
typealias Bound = (imageRect: Rect, canvasSize: Size) -> Rect
```

`Bound` limits zoom level / position of `imageRect`

Predefined `Bound` can be found in object `Bounds`

Available Bounds:
|Bound|Description|
|-|-|
|NO_BOUND|No Bound|
|FORCE_OVERLAP|Forces `imageRect` to always overlap with `canvasSize`. a.k.a. No Whitespace|
|FORCE_OVERLAP_OR_CENTER|Forces `imageRect` to overlap with `canvasSize`, center if not possible. Do not allow images to go smaller than canvasSize|

## Auto Height Adjustments

SubSampledImage supports automatic height based on loaded image  
call `Modifier.wrapContentHeight(state: SubSampledImageState, defaultHeight: Dp)`  
the height is fixed to supplied `defaultHeight` until the image loads and changes height based on the aspect ratio of the image  
:warning: ***use `ScaleTypes.FIT_WIDTH` as a scaleType***  

Open an issue if you want this feature with the width

## Gestures

### Enable Zooming/Panning
set `state.isGestureEnabled` to true to enable this feature  
default value is `false`

### Double Tap Zoom
call one of the following to enable double tap zoom
```kotlin
Modifier.doubleClickCycleZoom(state, scale)
Modifier.doubleClickContinuousZoom(state, scale)
```

`Modifier.doubleClickCycleZoom(state, scale)` cycles between `scale` and original scale
`Modifier.doubleClickContinuousZoom(state, scale)` continues to zoom in

## Animated Panning/Zooming
call `state.setImageRectWithBound(Rect, animationSpec)` to animate panning and zooming.  
see chapter `Three Important Rectangles` and [double tap zoom source code](https://github.com/tom5079/SubSampledImage/blob/cfa1dcbe476817f31493cc7089825f730a824325/library/src/main/java/xyz/quaver/graphics/subsampledimage/util.kt#L214) to learn more

# Credits

davemorrissey for [screen test card images](https://github.com/davemorrissey/screen-test-card)

# License

```
Copyright 2021 tom5079

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
