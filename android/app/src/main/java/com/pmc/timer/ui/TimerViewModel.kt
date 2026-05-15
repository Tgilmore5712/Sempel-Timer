package com.pmc.timer.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.pmc.timer.models.BoilItem
import com.pmc.timer.audio.AlarmSoundPlayer
import com.pmc.timer.worker.AlertWorker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID
import java.util.concurrent.TimeUnit

data class TimerState(
    val items: List<BoilItem> = emptyList(),
    val running: Boolean = false,
    val elapsed: Int = 0,
    val alertMessage: String = "",
    val firedDropTimes: Set<String> = emptySet()
)

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(TimerState(items = createDefaultItems()))
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var startTime: Long = 0
    private val workManager = WorkManager.getInstance(application)

    private fun createDefaultItems() = listOf(
        BoilItem(name = "Potatoes", minutes = 20.0),
        BoilItem(name = "Sausage", minutes = 12.0),
        BoilItem(name = "Lobster tails", minutes = 9.0),
        BoilItem(name = "Corn", minutes = 8.0),
        BoilItem(name = "Crab legs", minutes = 6.0),
        BoilItem(name = "Mussels", minutes = 5.0),
        BoilItem(name = "Crawfish", minutes = 5.0),
        BoilItem(name = "Clams", minutes = 4.0),
        BoilItem(name = "Shrimp", minutes = 3.0)
    ).sortedByDescending { it.minutes }

    fun addItem() {
        _state.update { it.copy(items = it.items + BoilItem(name = "New item", minutes = 5.0)) }
    }

    fun removeItem(id: String) {
        _state.update { it.copy(items = it.items.filter { item -> item.id != id }) }
    }

    fun updateItem(id: String, name: String, minutes: Double) {
        _state.update { it.copy(items = it.items.map { item ->
            if (item.id == id) item.copy(name = name, minutes = minutes) else item
        }) }
    }

    fun resetDefaults() {
        stopTimer()
        _state.value = TimerState(items = createDefaultItems())
    }

    fun toggleTimer() {
        if (_state.value.running) {
            stopTimer()
        } else {
            startTimer()
        }
    }

    private fun startTimer() {
        if (_state.value.items.isEmpty()) return
        
        val longestSeconds = (_state.value.items.maxOf { it.minutes } * 60).toInt()
        if (_state.value.elapsed >= longestSeconds) {
            _state.update { it.copy(elapsed = 0, firedDropTimes = emptySet(), alertMessage = "") }
        }

        startTime = System.currentTimeMillis() - (_state.value.elapsed * 1000L)
        _state.update { it.copy(running = true) }

        scheduleBackgroundAlerts()

        timerJob = viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val currentElapsed = ((now - startTime) / 1000).toInt()
                val totalSeconds = (_state.value.items.maxOf { it.minutes } * 60).toInt()

                if (currentElapsed >= totalSeconds) {
                    _state.update { it.copy(
                        elapsed = totalSeconds,
                        running = false,
                        alertMessage = "Done! Pull everything out."
                    ) }
                    stopTimer()
                    break
                }

                checkAlerts(currentElapsed, totalSeconds)
                _state.update { it.copy(elapsed = currentElapsed) }
                delay(1000)
            }
        }
    }

    private fun scheduleBackgroundAlerts() {
        workManager.cancelAllWork()
        val currentState = _state.value
        val longestSeconds = (currentState.items.maxOf { it.minutes } * 60).toInt()
        val currentElapsed = currentState.elapsed

        currentState.items.forEach { item ->
            val dropAt = (longestSeconds - (item.minutes * 60)).toInt()
            val delaySeconds = dropAt - currentElapsed
            if (delaySeconds >= 0) {
                val data = Data.Builder()
                    .putString("title", "Drop in: ${item.name}")
                    .putString("message", "Time to add ${item.name} to the boil")
                    .putString("itemName", item.name)
                    .build()

                val workRequest = OneTimeWorkRequest.Builder(AlertWorker::class.java)
                    .setInitialDelay(delaySeconds.toLong(), TimeUnit.SECONDS)
                    .setInputData(data)
                    .addTag("alert")
                    .build()
                workManager.enqueue(workRequest)
            }
        }

        val finishDelay = longestSeconds - currentElapsed
        if (finishDelay >= 0) {
            val data = Data.Builder()
                .putString("title", "Boil Complete!")
                .putString("message", "Pull everything out of the water")
                .putString("itemName", "")
                .build()
            val workRequest = OneTimeWorkRequest.Builder(AlertWorker::class.java)
                .setInitialDelay(finishDelay.toLong(), TimeUnit.SECONDS)
                .setInputData(data)
                .addTag("alert")
                .build()
            workManager.enqueue(workRequest)
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        workManager.cancelAllWork()
        _state.update { it.copy(running = false) }
    }

    private fun checkAlerts(elapsed: Int, totalSeconds: Int) {
        val fired = _state.value.firedDropTimes.toMutableSet()
        val newAlerts = mutableListOf<String>()

        _state.value.items.forEach { item ->
            val dropAt = (totalSeconds - (item.minutes * 60)).toInt()
            if (elapsed >= dropAt && !fired.contains(item.id)) {
                fired.add(item.id)
                newAlerts.add(item.name)
            }
        }

        if (newAlerts.isNotEmpty()) {
            val itemList = newAlerts.joinToString(" and ")
            _state.update { it.copy(
                firedDropTimes = fired,
                alertMessage = "Drop in: ${newAlerts.joinToString(", ")}"
            ) }
            playAlarmSound(itemList)
        }
    }

    private fun playAlarmSound(itemNames: String? = null) {
        val announcement = if (!itemNames.isNullOrBlank())
            "Time to add $itemNames"
        else
            "Time to add next item"
        AlarmSoundPlayer.playWithAnnouncement(getApplication(), announcement)
    }

    fun reset() {
        stopTimer()
        _state.update { it.copy(elapsed = 0, running = false, alertMessage = "", firedDropTimes = emptySet()) }
    }
}
