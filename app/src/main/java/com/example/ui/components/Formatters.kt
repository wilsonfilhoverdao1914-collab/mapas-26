package com.example.ui.components

import java.text.NumberFormat
import java.util.Locale

private val ptBrFormat: NumberFormat by lazy {
  NumberFormat.getIntegerInstance(Locale("pt", "BR"))
}

fun Int.formatBr(): String = ptBrFormat.format(this)
