package xyz.azraellab.shared.data

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UiStateTest {

    @Test
    fun readyMapsDataAndGetOrNull() {
        val ready: UiState<Int> = UiState.Ready(7)
        assertEquals(8, ready.map { it + 1 }.getOrNull())
        assertEquals(7, ready.getOrNull())
    }

    @Test
    fun loadingStaysLoadingAndErrorPassesThrough() {
        val loading: UiState<Int> = UiState.Loading
        assertTrue(loading.map { it + 1 } is UiState.Loading)
        assertNull(loading.getOrNull())

        val err: UiState<Int> = UiState.Error("boom")
        val mapped = err.map { it + 1 }
        assertTrue(mapped is UiState.Error)
        assertEquals("boom", mapped.message)
        assertNull(mapped.getOrNull())
    }

    @Test
    fun runStateIntCapturesFailure() = runBlocking {
        assertEquals("broken", (runStateInt { throw IllegalStateException("broken") } as UiState.Error).message)
        assertEquals(3, runStateInt { 3 }.getOrNull())
    }

    @Test
    fun errorKeepsKindThroughMapAndReadyIsFinal() {
        val err: UiState<Int> = UiState.Error("x")
        assertTrue(err.map { it + 1 } is UiState.Error)
        val ready: UiState<Int> = UiState.Ready(1)
        assertTrue(ready.map { it * 2 } is UiState.Ready)
    }
}