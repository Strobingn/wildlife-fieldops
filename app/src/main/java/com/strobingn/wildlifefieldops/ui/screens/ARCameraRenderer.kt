package com.strobingn.wildlifefieldops.ui.screens

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import com.google.ar.core.DepthPoint
import com.google.ar.core.Plane
import com.google.ar.core.Session
import com.google.ar.core.TrackingState
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.SessionPausedException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

internal data class ARHitPoint(
    val fractionX: Float,
    val fractionY: Float,
    val worldX: Float,
    val worldY: Float,
    val worldZ: Float
)

internal class ARCameraRenderer(
    private val session: Session,
    private val displayRotation: () -> Int,
    private val onFrameReady: () -> Unit,
    private val onHit: (ARHitPoint) -> Unit,
    private val onNoSurface: () -> Unit,
    private val onError: (String) -> Unit
) : GLSurfaceView.Renderer {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingTap = AtomicReference<Pair<Float, Float>?>(null)
    private val sessionResumed = AtomicBoolean(false)
    private val surfaceReady = AtomicBoolean(false)
    private val fatalErrorDelivered = AtomicBoolean(false)

    private var textureId = 0
    private var program = 0
    private var positionHandle = 0
    private var texCoordHandle = 0
    private var textureHandle = 0
    private var viewportWidth = 1
    private var viewportHeight = 1
    private var lastRotation = -1
    private var firstFrameDelivered = false

    private val quadVertices: FloatBuffer = floatBufferOf(
        -1f, -1f,
         1f, -1f,
        -1f,  1f,
         1f,  1f
    )

    private val cameraUv = FloatArray(8)
    private val cameraUvBuffer: FloatBuffer = floatBufferOf(
        0f, 1f,
        1f, 1f,
        0f, 0f,
        1f, 0f
    )

    fun queueTap(x: Float, y: Float) {
        pendingTap.set(x to y)
    }

    fun setSessionResumed(resumed: Boolean) {
        sessionResumed.set(resumed)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            textureId = createExternalTexture()
            check(textureId != 0) { "OpenGL could not create the AR camera texture" }

            session.setCameraTextureName(textureId)
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
            texCoordHandle = GLES20.glGetAttribLocation(program, "a_TexCoord")
            textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")

            check(positionHandle >= 0 && texCoordHandle >= 0 && textureHandle >= 0) {
                "AR camera shader attributes could not be initialized"
            }

            GLES20.glClearColor(0f, 0f, 0f, 1f)
            surfaceReady.set(true)
        } catch (error: Throwable) {
            deliverFatalError("AR renderer initialization failed: ${error.message ?: error.javaClass.simpleName}")
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        viewportWidth = width.coerceAtLeast(1)
        viewportHeight = height.coerceAtLeast(1)
        GLES20.glViewport(0, 0, viewportWidth, viewportHeight)
        updateDisplayGeometry(force = true)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        if (!surfaceReady.get() || !sessionResumed.get() || fatalErrorDelivered.get()) return

        try {
            updateDisplayGeometry(force = false)
            val frame = session.update()

            if (frame.hasDisplayGeometryChanged() || !firstFrameDelivered) {
                updateCameraUv(frame)
            }

            drawCameraBackground()

            if (!firstFrameDelivered && frame.timestamp != 0L) {
                firstFrameDelivered = true
                mainHandler.post(onFrameReady)
            }

            pendingTap.getAndSet(null)?.let { (x, y) ->
                if (frame.camera.trackingState != TrackingState.TRACKING) {
                    mainHandler.post(onNoSurface)
                    return@let
                }

                val hit = frame.hitTest(x, y).firstOrNull { result ->
                    when (val trackable = result.trackable) {
                        is Plane ->
                            trackable.trackingState == TrackingState.TRACKING &&
                                trackable.isPoseInPolygon(result.hitPose)
                        is DepthPoint -> trackable.trackingState == TrackingState.TRACKING
                        else -> false
                    }
                }

                if (hit == null) {
                    mainHandler.post(onNoSurface)
                } else {
                    val pose = hit.hitPose
                    val point = ARHitPoint(
                        fractionX = (x / viewportWidth).coerceIn(0f, 1f),
                        fractionY = (y / viewportHeight).coerceIn(0f, 1f),
                        worldX = pose.tx(),
                        worldY = pose.ty(),
                        worldZ = pose.tz()
                    )
                    mainHandler.post { onHit(point) }
                }
            }
        } catch (_: SessionPausedException) {
            // Normal while Compose/Activity lifecycle transitions are settling.
        } catch (error: CameraNotAvailableException) {
            sessionResumed.set(false)
            deliverFatalError("Camera is unavailable. Close other camera apps and tap Retry.")
        } catch (error: Throwable) {
            deliverFatalError(error.message ?: error.javaClass.simpleName)
        }
    }

    private fun updateDisplayGeometry(force: Boolean) {
        val rotation = displayRotation()
        if (force || rotation != lastRotation) {
            lastRotation = rotation
            session.setDisplayGeometry(rotation, viewportWidth, viewportHeight)
        }
    }

    private fun updateCameraUv(frame: com.google.ar.core.Frame) {
        val input = floatBufferOf(
            0f, 1f,
            1f, 1f,
            0f, 0f,
            1f, 0f
        )
        val output = ByteBuffer.allocateDirect(8 * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        frame.transformDisplayUvCoords(input, output)
        output.position(0)
        output.get(cameraUv)
        cameraUvBuffer.position(0)
        cameraUvBuffer.put(cameraUv)
        cameraUvBuffer.position(0)
    }

    private fun drawCameraBackground() {
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthMask(false)
        GLES20.glUseProgram(program)

        quadVertices.position(0)
        GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 0, quadVertices)
        GLES20.glEnableVertexAttribArray(positionHandle)

        cameraUvBuffer.position(0)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, cameraUvBuffer)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(textureHandle, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
        GLES20.glDepthMask(true)
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        return textures[0]
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        return GLES20.glCreateProgram().also { programId ->
            GLES20.glAttachShader(programId, vertexShader)
            GLES20.glAttachShader(programId, fragmentShader)
            GLES20.glLinkProgram(programId)
            val status = IntArray(1)
            GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                "AR camera shader link failed: ${GLES20.glGetProgramInfoLog(programId)}"
            }
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
        }
    }

    private fun compileShader(type: Int, source: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, source)
            GLES20.glCompileShader(shader)
            val status = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
            check(status[0] == GLES20.GL_TRUE) {
                "AR camera shader compile failed: ${GLES20.glGetShaderInfoLog(shader)}"
            }
        }
    }

    private fun deliverFatalError(message: String) {
        if (fatalErrorDelivered.compareAndSet(false, true)) {
            mainHandler.post { onError(message) }
        }
    }

    private fun floatBufferOf(vararg values: Float): FloatBuffer {
        return ByteBuffer.allocateDirect(values.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .apply {
                put(values)
                position(0)
            }
    }

    companion object {
        private const val VERTEX_SHADER = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES u_Texture;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
    }
}
