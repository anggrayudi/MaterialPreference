package com.anggrayudi.materialpreference.util

typealias IntSummaryFormatter = (value: Int) -> String?

typealias StringSummaryFormatter = (value: String?) -> String?

typealias EntrySummaryFormatter = (entry: CharSequence?, value: String?) -> String?

typealias ArraySummaryFormatter = (value: Array<String>) -> String?