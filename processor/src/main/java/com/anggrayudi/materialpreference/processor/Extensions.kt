package com.anggrayudi.materialpreference.processor

import javax.annotation.processing.Messager

inline fun Messager.errorMessage(message: () -> String) {
    this.printMessage(javax.tools.Diagnostic.Kind.ERROR, message())
}

inline fun Messager.noteMessage(message: () -> String) {
    this.printMessage(javax.tools.Diagnostic.Kind.NOTE, message())
}

inline fun Messager.warningMessage(message: () -> String) {
    this.printMessage(javax.tools.Diagnostic.Kind.WARNING, message())
}
