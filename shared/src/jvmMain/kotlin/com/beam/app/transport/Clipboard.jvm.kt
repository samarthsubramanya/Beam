package com.beam.app.transport

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

actual fun setClipboardText(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
