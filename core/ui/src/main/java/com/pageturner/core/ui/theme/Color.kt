package com.pageturner.core.ui.theme

import androidx.compose.ui.graphics.Color

/** PageTurner design system colour tokens. Dark theme only. Never use raw hex values in UI code. */
object PageTurnerColors {
    val Background     = Color(0xFF0F0E17)   // near-black, warm undertone
    val Surface        = Color(0xFF1A1928)   // slightly lifted surface
    val SurfaceVariant = Color(0xFF252438)   // cards, elevated surfaces
    val OnBackground   = Color(0xFFF5F0E8)   // warm white
    val OnSurface      = Color(0xFFF5F0E8)
    val OnSurfaceMuted = Color(0x73F5F0E8)   // 45% opacity warm white
    val Accent         = Color(0xFFE8A020)   // amber — primary action colour
    val OnAccent       = Color(0xFF0F0E17)
    val Teal           = Color(0xFF5DCAA5)   // wildcard badge, AI indicators
    val Error          = Color(0xFFFF4444)
    val SkipRed        = Color(0x29FF3C3C)   // swipe-left overlay tint (16% opacity)
    val SaveGreen      = Color(0x295DCAA5)   // swipe-right overlay tint (16% opacity)
    val CardBorder     = Color(0x1AF5F0E8)   // subtle 10% white border on cards
}
