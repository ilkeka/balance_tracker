package me.ilker.accountlink.scanner

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureMediaTypeVideo
import platform.AVFoundation.AVCaptureMetadataOutput
import platform.AVFoundation.AVCaptureMetadataOutputObjectsDelegateProtocol
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetHigh
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMetadataMachineReadableCodeObject
import platform.AVFoundation.AVMetadataObjectTypeQRCode
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun QrScanner(onScanned: (String) -> Unit) {
    val currentOnScanned = rememberUpdatedState(onScanned)
    val scannerDelegate = remember {
        QrScannerDelegate { value ->
            currentOnScanned.value(value)
        }
    }

    DisposableEffect(Unit) {
        scannerDelegate.start()
        onDispose { scannerDelegate.stop() }
    }

    UIKitView(
        modifier = Modifier.fillMaxSize(),
        factory = { scannerDelegate.createView() },
        update = { view -> scannerDelegate.layout(view) }
    )
}

@OptIn(ExperimentalForeignApi::class)
private class QrScannerDelegate(
    private val onScanned: (String) -> Unit
) : NSObject(), AVCaptureMetadataOutputObjectsDelegateProtocol {

    private val session = AVCaptureSession()
    private val previewLayer = AVCaptureVideoPreviewLayer(session = session)
    private val metadataOutput = AVCaptureMetadataOutput()
    private var didDetect = false

    fun createView(): UIView {
        val view = UIView(frame = CGRectZero)
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
        view.layer.addSublayer(previewLayer)
        layout(view)
        return view
    }

    fun layout(view: UIView) {
        previewLayer.frame = view.bounds
    }

    fun start() {
        val device = AVCaptureDevice.defaultDeviceWithMediaType(AVCaptureMediaTypeVideo) ?: return
        AVCaptureDevice.requestAccessForMediaType(AVCaptureMediaTypeVideo) { granted ->
            if (granted) {
                val input = AVCaptureDeviceInput(device, error = null) ?: return@requestAccessForMediaType
                session.beginConfiguration()
                session.sessionPreset = AVCaptureSessionPresetHigh
                if (session.canAddInput(input)) session.addInput(input)
                if (session.canAddOutput(metadataOutput)) {
                    session.addOutput(metadataOutput)
                    metadataOutput.setMetadataObjectsDelegate(this, dispatchQueue = null)
                    metadataOutput.metadataObjectTypes = listOf(AVMetadataObjectTypeQRCode)
                }
                session.commitConfiguration()
                session.startRunning()
            }
        }
    }

    fun stop() {
        if (session.running) {
            session.stopRunning()
        }
    }

    override fun captureOutput(
        output: AVCaptureOutput,
        didOutputMetadataObjects: List<*>,
        fromConnection: AVCaptureConnection
    ) {
        if (didDetect) return

        val code = didOutputMetadataObjects
            .mapNotNull { it as? AVMetadataMachineReadableCodeObject }
            .firstOrNull { it.type == AVMetadataObjectTypeQRCode && it.stringValue != null }

        code?.stringValue?.let { value ->
            didDetect = true
            onScanned(value)
        }
    }
}
