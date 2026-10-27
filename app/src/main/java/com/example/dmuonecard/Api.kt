package com.example.dmuonecard

data class ScanRequest(
    val student_id: String
)

data class StudentInfo(
    val student_id: String? = null,
    val full_name: String? = null,
    val department: String? = null,
    val photo_url: String? = null
)

data class ScanResponse(
    val status: String? = null,
    val message: String? = null,
    val student: StudentInfo? = null,
    val meal: String? = null,
    val service_date: String? = null,
    val served_at: String? = null,
    val server_time: String? = null
)
