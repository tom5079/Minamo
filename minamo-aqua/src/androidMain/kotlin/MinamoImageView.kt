import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceView

class MinamoImageView(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : SurfaceView(context, attrs, defStyleAttr, defStyleRes), Runnable {

    val thread: Thread? = null
    private val holder = getHolder()

    init {
        System.loadLibrary("minamo-aqua")
    }

    override fun run() {

    }

}