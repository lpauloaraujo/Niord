package com.example.niord
import kotlinx.coroutines.*

class Debouncer(private val delayMs: Long) {
    private var debounceJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    private var able = true

    fun process(action: suspend () -> Unit) {

        if(able) {
            debounceJob = coroutineScope.launch {
                action()
                able = false
                delay(delayMs)
                able = true
            }
        }
    }

    fun tearDown() {
        coroutineScope.cancel()
    }
}