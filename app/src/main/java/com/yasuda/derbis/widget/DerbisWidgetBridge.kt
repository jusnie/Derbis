package com.yasuda.derbis.widget

object DerbisWidgetBridge {
    @Volatile
    var onToggle: (() -> Unit)? = null

    @Volatile
    var onNext: (() -> Unit)? = null

    @Volatile
    var onPrevious: (() -> Unit)? = null

    fun clear() {
        onToggle = null
        onNext = null
        onPrevious = null
    }
}
