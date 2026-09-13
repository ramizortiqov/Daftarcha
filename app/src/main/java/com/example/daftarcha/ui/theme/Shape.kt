package com.example.daftarcha.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// The Modernist system draws everything square — ruled ledger rows, flat
// bordered buttons, no radius anywhere. Zero-corner shapes let every
// Material3 component (Card, Button, TextField, Dialog, NavigationBar…)
// inherit the flat look automatically through MaterialTheme.shapes.
val DaftarchaShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)
