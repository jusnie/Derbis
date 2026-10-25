package com.luc4n3x.levyra.feature.radio

/** Reasons a user-created radio station can be rejected or fail to save. */
internal enum class CustomRadioError {
    Name,
    StreamUrl,
    ImageUrl,
    Duplicate,
    Storage,
    Limit
}
