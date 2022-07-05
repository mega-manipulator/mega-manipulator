package com.github.jensim.megamanipulator.settings.types

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.github.jensim.megamanipulator.settings.types.codehost.CodeHostSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettings
import com.github.jensim.megamanipulator.settings.types.searchhost.SearchHostSettingsGroup
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.io.File

data class MegaManipulatorSettings(
    @Min(1)
    @Max(100)
    @JsonPropertyDescription(
        """
When applying changes using the scripted method,
number of parallel executing changes
"""
    )
    val concurrency: Int = 5,
    @JsonPropertyDescription(
        """
Override the default strict https validation
May be set less strict on searchHost or codeHost level as well
"""
    )
    val defaultHttpsOverride: HttpsOverride? = null,
    @JsonPropertyDescription("Search host definitions")
    val searchHostSettings: Map<String, SearchHostSettingsGroup>,
    @JsonProperty(value = "\$schema")
    val schema: String = "mega-manipulator-schema.json",
) {
    init {
        require(searchHostSettings.isNotEmpty()) {
            """
            |Please add one or search host settings.
            |Available types are [HOUND, SOURCEGRAPH] 
            |""".trimMargin()
        }
    }

    fun resolveHttpsOverride(searchHostName: String): HttpsOverride? = searchHostSettings[searchHostName]
        ?.value()?.httpsOverride ?: defaultHttpsOverride

    fun resolveHttpsOverride(searchHostName: String, codeHostName: String): HttpsOverride? = searchHostSettings[searchHostName]
        ?.value()?.codeHostSettings?.get(codeHostName)?.value()?.httpsOverride ?: defaultHttpsOverride

    fun resolveSettings(repoDir: File): Pair<SearchHostSettings, CodeHostSettings>? {
        val codeHostDir: String = repoDir.parentFile.parentFile.name
        val searchHostDir: String = repoDir.parentFile.parentFile.parentFile.name
        return resolveSettings(searchHostDir, codeHostDir)
    }

    fun resolveSettings(searchHostName: String, codeHostName: String): Pair<SearchHostSettings, CodeHostSettings>? {
        return searchHostSettings[searchHostName]?.value()?.let { first ->
            searchHostSettings[searchHostName]?.value()?.codeHostSettings?.get(codeHostName)?.value()?.let { second ->
                Pair(first, second)
            }
        }
    }
}
