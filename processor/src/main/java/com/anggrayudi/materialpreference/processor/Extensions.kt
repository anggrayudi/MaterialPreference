package com.anggrayudi.materialpreference.processor

import javax.annotation.processing.Messager

fun Messager.errorMessage(message : () -> String){
    this.printMessage(javax.tools.Diagnostic.Kind.ERROR, message())
}

fun Messager.noteMessage(message : () -> String){
    this.printMessage(javax.tools.Diagnostic.Kind.NOTE, message())
}

fun Messager.warningMessage(message : () -> String){
    this.printMessage(javax.tools.Diagnostic.Kind.WARNING, message())
}