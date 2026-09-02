package com.univ.learningapp.data.model

enum class MaterialType {
    PDF,
    VIDEO
}

data class PdfMaterial(
    val id: String,
    val title: String,
    val subject: String,
    val pagesCount: Int = 0,
    val fileSizeMb: String = "2.5 MB",
    val pdfUrl: String = "",
    val videoUrl: String? = null,
    val videoDuration: String? = null,
    val description: String,
    val category: String = "Notes",
    val materialType: MaterialType = MaterialType.PDF,
    val isLocked: Boolean = false,
    val priceInInr: Int = 0
)
