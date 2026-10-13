package com.bbetterdialer.ui.design

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bbetterdialer.R
import com.bbetterdialer.ui.theme.Surface2
import com.bbetterdialer.ui.theme.TextPrimary
import com.bbetterdialer.ui.theme.TextTertiary

/**
 * Lapos keresősáv – a jobb szélén kis kereső ikonnal, ami jelzi, hogy ez egy
 * keresőmező. Amint van beírt szöveg, az ikon fehérre vált.
 */
@Composable
fun FlatSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Keresés",
    showIcon: Boolean = true,
) {
    Row(
        modifier = modifier
            .flatSurface(RoundedCornerShape(14.dp), Surface2)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
            cursorBrush = SolidColor(TextPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextTertiary,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            }
        )

        Spacer(Modifier.width(10.dp))

        if (showIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_search),
                contentDescription = "Keresés",
                tint = if (value.isEmpty()) TextTertiary else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
