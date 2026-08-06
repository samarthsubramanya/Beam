package com.beam.app.transport

import platform.UIKit.UIPasteboard

actual fun setClipboardText(text: String) {
    UIPasteboard.generalPasteboard.string = text
}
