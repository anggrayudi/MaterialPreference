package com.anggrayudi.materialpreference.processor

data class PreferenceSpec(
        var key: String = "",
        var constantName: String = "",
        var methodName: String? = null,
        var dataType: PreferenceDataType = PreferenceDataType.NOTHING,
        var defaultValue: Any? = null
)