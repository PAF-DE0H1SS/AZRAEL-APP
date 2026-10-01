package xyz.azraellab.shared.data.vm

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import xyz.azraellab.shared.data.UiState
import kotlin.test.Test
import kotlin.test.assertEquals

private class ProbeViewModel(scope: CoroutineScope) : AppViewModel(scope) {
    val s = MutableStateFlow<UiState<String>>(UiState.Loading)
    fun load(loader: suspend () -> UiState<String>) = loadInto(s, loader)
}

class AppViewModelTest {

    @Test
    fun loadGoesLoadingThenReady() = runBlocking {
        val gate = CompletableDeferred<UiState<String>>()
        val vm = ProbeViewModel(CoroutineScope(Dispatchers.Default))
        vm.load { gate.await() }
        yield()
        // Двери ещё не отпущены — поток обязан быть в Loading.
        val loading = withTimeout(2000) { vm.s.first { it is UiState.Loading } }
        assertEquals(UiState.Loading, loading)

        gate.complete(UiState.Ready("ok"))
        val ready = withTimeout(2000) { vm.s.first { it is UiState.Ready } }
        assertEquals("ok", (ready as UiState.Ready).data)
        vm.close()
    }

    @Test
    fun loaderErrorBecomesError() = runBlocking {
        val vm = ProbeViewModel(CoroutineScope(Dispatchers.Default))
        vm.load { UiState.Error("boom") }
        val err = withTimeout(2000) { vm.s.first { it is UiState.Error } }
        assertEquals("boom", (err as UiState.Error).message)
        vm.close()
    }

    @Test
    fun closeCancelsPendingLoad() = runBlocking {
        val gate = CompletableDeferred<UiState<String>>()
        val started = CompletableDeferred<Unit>()
        val vm = ProbeViewModel(CoroutineScope(Dispatchers.Default))
        vm.load {
            started.complete(Unit)
            gate.await()
        }
        // Ждём, пока загрузка действительно встанет на `gate.await()` — иначе
        // проверка зависела от того, успел ли планировщик запустить корутину, и
        // падала при случайной задержке вместо честной гонки.
        withTimeout(2000) { started.await() }
        vm.close()
        gate.complete(UiState.Ready("late"))
        // После close никаких новых значений: поток остаётся в Loading.
        yield()
        val still = vm.s.value
        assertEquals(UiState.Loading, still)
    }
}