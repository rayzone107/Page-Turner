package com.pageturner.core.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** PageTurner typography tokens. Use these everywhere — never hardcode text styles. */
object PageTurnerType {
    val AppTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold
    )
    val CardTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
    )
    val DetailTitle = TextStyle(
        fontFamily = FontFamily.Serif,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold
    )
    val Body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 22.sp
    )
    val BodySmall = TextStyle(
        fontSize = 12.sp,
        lineHeight = 18.sp
    )
    /** Italic style used for all AI-generated content. */
    val AiBrief = TextStyle(
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontStyle = FontStyle.Italic
    )
    val Label = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp
    )
    val Chip = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
}
