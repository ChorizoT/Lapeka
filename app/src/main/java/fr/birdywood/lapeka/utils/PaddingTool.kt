package fr.birdywood.lapeka.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

fun PaddingValues.update(
    start: Dp = this.calculateStartPadding(LayoutDirection.Ltr),
    top: Dp = this.calculateTopPadding(),
    end: Dp = this.calculateEndPadding(LayoutDirection.Ltr),
    bottom: Dp = this.calculateBottomPadding()
): PaddingValues =
    PaddingValues(
        start = start,
        top = top,
        end = end,
        bottom = bottom
    )

fun PaddingValues.add(
    start: Dp = 0.dp,
    top: Dp = 0.dp,
    end: Dp = 0.dp,
    bottom: Dp = 0.dp
) =
    PaddingValues(
        start = this.calculateStartPadding(LayoutDirection.Ltr) + start,
        top = this.calculateTopPadding() + top,
        end = this.calculateEndPadding(LayoutDirection.Ltr) + end,
        bottom = this.calculateBottomPadding() + bottom
    )