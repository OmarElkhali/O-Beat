package com.zionhuang.innertube

import com.zionhuang.innertube.models.SongItem
import com.zionhuang.innertube.models.response.SearchResponse
import com.zionhuang.innertube.pages.SearchSummaryPage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalSerializationApi::class)
class SearchSummaryTest {
    @Test
    fun `summary retains headerless cards and songs wrapped in item sections`() {
        // Reduced anonymous live response for "Daft punk", captured on 2026-10-08.
        val response = javaClass.getResource("/search-summary-item-sections.json")!!.readText()
        val parsed = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }.decodeFromString(SearchResponse.serializer(), response)
        val page = SearchSummaryPage.fromResponse(parsed)
        assertEquals("Daft Punk", page.summaries.first().title)
        assertEquals(
            listOf("Instant Crush (feat. Julian Casablancas)", "Voyager"),
            page.summaries.flatMap { it.items }.filterIsInstance<SongItem>().map { it.title },
        )
        val songs = page.summaries.flatMap { it.items }.filterIsInstance<SongItem>()
        assertTrue(songs.first().artists.isEmpty())
        assertEquals(338, songs.first().duration)
        assertEquals("Daft Punk", songs.last().artists.single().name)
        assertEquals("UCRr1xG_2WIDs18a6cIiCxeA", songs.last().artists.single().id)
    }
}
