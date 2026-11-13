package com.example.myapplication

data class EmailCheckResponse(
    val success: Boolean, // 중복 체크 성공 여부
    val message: String,   // 사용 가능/불가 여부 메시지
    val available: Boolean
)
