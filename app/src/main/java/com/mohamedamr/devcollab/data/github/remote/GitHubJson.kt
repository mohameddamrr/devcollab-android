package com.mohamedamr.devcollab.data.github.remote

import kotlinx.serialization.json.Json

internal val githubJson = Json {
    ignoreUnknownKeys = true
}
