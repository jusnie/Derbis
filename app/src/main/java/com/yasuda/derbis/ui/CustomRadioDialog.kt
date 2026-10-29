package com.yasuda.derbis.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yasuda.derbis.feature.radio.CustomRadioError
import com.yasuda.derbis.feature.radio.RadioStation
import com.yasuda.derbis.ui.i18n.LocalDerbisStrings
import com.yasuda.derbis.ui.i18n.radioLibraryCopy
import com.yasuda.derbis.ui.theme.DerbisSkinManagesLayout

@Composable
internal fun CustomRadioDialog(
    station: RadioStation?, saving: Boolean, error: CustomRadioError?,
    onDismiss: () -> Unit, onChanged: () -> Unit, onSave: (String, String, String) -> Unit
) {
    val strings = LocalDerbisStrings.current
    val copy = radioLibraryCopy(strings.code)
    var name by rememberSaveable(station?.uuid) { mutableStateOf(station?.name.orEmpty()) }
    var stream by rememberSaveable(station?.uuid) { mutableStateOf(station?.streamUrl.orEmpty()) }
    var image by rememberSaveable(station?.uuid) { mutableStateOf(station?.faviconUrl.orEmpty()) }
    val shape = if (DerbisSkinManagesLayout) MaterialTheme.shapes.medium else RoundedCornerShape(28.dp)
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        icon = { Icon(Icons.Rounded.Podcasts, null, Modifier.size(56.dp).background(MaterialTheme.colorScheme.secondaryContainer, shape).padding(12.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer) },
        title = { Text(if (station == null) copy.add else copy.edit) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it.take(160); onChanged() }, label = { Text(copy.name) },
                    enabled = !saving, singleLine = true, isError = error == CustomRadioError.Name, shape = shape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(stream, { stream = it.take(4096); onChanged() }, label = { Text(copy.stream) },
                    enabled = !saving, singleLine = true, isError = error == CustomRadioError.StreamUrl || error == CustomRadioError.Duplicate,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), shape = shape, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(image, { image = it.take(4096); onChanged() }, label = { Text(copy.image) },
                    enabled = !saving, singleLine = true, isError = error == CustomRadioError.ImageUrl,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), shape = shape, modifier = Modifier.fillMaxWidth())
                if (error != null) Text(copy.error(error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        },
        confirmButton = { Button(onClick = { onSave(name, stream, image) }, enabled = !saving) { Text(strings.save) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text(strings.cancel) } }
    )
}
