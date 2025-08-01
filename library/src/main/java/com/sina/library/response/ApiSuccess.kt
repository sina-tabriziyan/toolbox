package com.sina.library.response

data class ApiSuccess<T>(
    val code: Int,
    val body: T
)
