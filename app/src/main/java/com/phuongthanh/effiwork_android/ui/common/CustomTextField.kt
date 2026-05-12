package com.phuongthanh.effiwork_android.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.phuongthanh.effiwork_android.ui.theme.Blue500

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    leadingIcon: Int? = null,
    trailingIcon: Int? = null,
    isPassword: Boolean = false,
    isEmail: Boolean = false,
    isPhone: Boolean = false,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    imeAction: ImeAction = ImeAction.Next
) {
    val visualTransformation = when {
        isPassword -> PasswordVisualTransformation()
        else -> VisualTransformation.None
    }

    val keyboardType = when {
        isEmail -> KeyboardType.Email
        isPhone -> KeyboardType.Phone
        else -> KeyboardType.Text
    }

    Column(
        modifier = Modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF9E9E9E)),
                )
            },
            leadingIcon = leadingIcon?.let {
                { Icon(painter = painterResource(it), contentDescription = null) }
            },
            trailingIcon = trailingIcon?.let {
                { Icon(painter = painterResource(it), contentDescription = null) }
            },
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Blue500,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = Blue500
            ),
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CustomTextFieldPreview() {
    CustomTextField(
        value = "Sample text",
        onValueChange = {},
        label = "Sample Label",
        placeholder = "Enter text here..."
    )
}