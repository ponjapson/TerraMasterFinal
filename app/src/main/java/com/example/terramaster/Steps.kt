package com.example.terramaster

data class Guide(
    val knowledgeGuideId: String = "",
    val title: String = "",
    val steps: MutableList<Step> = mutableListOf(),
    val guideType: String = "",
    val userType: String = ""
)
data class Step(
    val title: String = "",
    val description: String = ""
)
