package ru.alexbur.backend.profile.data

internal enum class ProfileType(val value: Int) {
    CLIENT(0),
    COACH(1),
    UNKNOWN(-1);

    companion object {
        fun fromInt(value: Int) = entries.find { it.value == value } ?: UNKNOWN
    }
}