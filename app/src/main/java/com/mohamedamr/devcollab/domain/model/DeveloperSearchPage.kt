package com.mohamedamr.devcollab.domain.model

data class DeveloperSearchPage(
    val developers: List<DeveloperSummary>,
    val totalCount: Int,
    val isIncomplete: Boolean,
)
