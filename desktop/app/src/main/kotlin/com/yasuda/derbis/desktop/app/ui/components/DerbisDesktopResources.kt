package com.yasuda.derbis.desktop.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.yasuda.derbis.desktop.app.generated.resources.Res
import com.yasuda.derbis.desktop.app.generated.resources.derbis
import org.jetbrains.compose.resources.painterResource

@Composable
fun derbisIconPainter(): Painter = painterResource(Res.drawable.derbis)
