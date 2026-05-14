package com.example.test.qna.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AndroidSTTProvider(
    context: Context
) : SpeechToTextProvider {

    private val appContext = context.applicationContext

    companion object {

        private const val TAG = "AndroidSTT"

        private const val SILENCE_TIMEOUT_MS = 3000L

        private const val FINAL_RESULT_WAIT_MS = 1200L

        /**
         * Ignore tiny noise
         */
        private const val RMS_THRESHOLD = 2f

        /**
         * Prevent timer spam
         */
        private const val RMS_DEBOUNCE_MS = 150L
    }

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                    Dispatchers.Main.immediate
        )

    private var speechRecognizer:
            SpeechRecognizer? = null

    private var silenceJob: Job? = null

    /**
     * Refers to whether the SpeechRecognizer engine is actively running
     */
    private var isEngineRunning = false

    private var lastSpeechTimestamp = 0L

    /**
     * True when recognizer
     * already heard something
     */
    private var hasRecognizedSpeech = false

    private var lastPartialTranscript: String? = null

    /**
     * Store the listener reference so it can be re-attached
     * if the recognizer is recreated.
     */
    private var currentListener: RecognitionListener? = null

    private val _stateFlow =
        MutableStateFlow<STTState>(
            STTState.Idle
        )

    override val stateFlow =
        _stateFlow.asStateFlow()

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (!scope.isActive) {
            Log.e(TAG, "ensureRecognizer: Provider scope is cancelled")
            return null
        }
        
        if (speechRecognizer == null) {
            Log.d(TAG, "ensureRecognizer: Creating new SpeechRecognizer")
            
            if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
                Log.e(TAG, "ensureRecognizer: Recognition not available")
                return null
            }

            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
                currentListener?.let {
                    Log.d(TAG, "ensureRecognizer: Re-attaching existing listener")
                    speechRecognizer?.setRecognitionListener(it)
                }
            } catch (e: Exception) {
                Log.e(TAG, "ensureRecognizer: Failed to create", e)
                return null
            }
        }
        return speechRecognizer
    }

    override val transcriptFlow:
            Flow<STTEvent> =

        callbackFlow {

            val listener =
                object : RecognitionListener {

                    override fun onReadyForSpeech(
                        params: Bundle?
                    ) {

                        Log.d(TAG, "onReadyForSpeech")
                        isEngineRunning = true
                        _stateFlow.value = STTState.Listening

                        startSilenceTimer {
                            Log.d(TAG, "Silence timeout triggered")
                            trySend(STTEvent.SilenceTimeout)
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "onBeginningOfSpeech")
                        resetSilenceTimer {
                            trySend(STTEvent.SilenceTimeout)
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        val now = SystemClock.elapsedRealtime()
                        val passedDebounce = now - lastSpeechTimestamp > RMS_DEBOUNCE_MS

                        if (rmsdB > RMS_THRESHOLD && passedDebounce) {
                            Log.v(TAG, "RMS: $rmsdB")
                            lastSpeechTimestamp = now
                            resetSilenceTimer {
                                Log.d(TAG, "Silence timeout (RMS)")
                                trySend(STTEvent.SilenceTimeout)
                            }
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        Log.d(TAG, "onEndOfSpeech")
                        cancelSilenceTimer()
                        _stateFlow.value = STTState.Processing
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        hasRecognizedSpeech = true
                        val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

                        if (!partial.isNullOrBlank()) {
                            lastPartialTranscript = partial
                            Log.d(TAG, "Partial: $partial")
                        }

                        resetSilenceTimer {
                            trySend(STTEvent.SilenceTimeout)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        Log.d(TAG, "onResults")
                        cancelSilenceTimer()
                        isEngineRunning = false
                        hasRecognizedSpeech = true

                        var matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                        if (matches.isNullOrEmpty() && !lastPartialTranscript.isNullOrBlank()) {
                            Log.d(TAG, "Results fallback to partial: $lastPartialTranscript")
                            matches = arrayListOf(lastPartialTranscript!!)
                        }

                        Log.d(TAG, "Final Matches: $matches")

                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            trySend(STTEvent.Transcript(text))
                        }

                        _stateFlow.value = STTState.Idle
                    }

                    override fun onError(error: Int) {
                        cancelSilenceTimer()
                        isEngineRunning = false

                        val message = errorMessage(error)
                        Log.e(TAG, "STT Error [$error]: $message")

                        // Error 5 (Client) or 8 (Busy) often require recreation
                        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || 
                            error == SpeechRecognizer.ERROR_CLIENT) {
                            Log.w(TAG, "Recoverable critical error, clearing instance")
                            speechRecognizer?.destroy()
                            speechRecognizer = null
                        }

                        trySend(STTEvent.Error(error, message))
                        _stateFlow.value = STTState.Error(code = error, message = message)
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                }

            currentListener = listener
            ensureRecognizer()?.setRecognitionListener(listener)

            awaitClose {
                Log.d(TAG, "callbackFlow closed")
                currentListener = null
                cancelSilenceTimer()
            }
        }
            .buffer(1)
            .flowOn(Dispatchers.Main.immediate)

    override suspend fun startListening() {
        Log.d(TAG, "startListening (isEngineRunning=$isEngineRunning)")

        if (isEngineRunning) {
            Log.d(TAG, "Engine already running, stopping first...")
            stopListening()
            delay(200) // Give it a moment to stabilize
        }

        val recognizer = ensureRecognizer() ?: run {
            Log.e(TAG, "startListening: Failed to get recognizer")
            return
        }

        hasRecognizedSpeech = false
        lastPartialTranscript = null

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Removed extra silence length params as they can be unstable across devices
        }

        try {
            isEngineRunning = true
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "startListening: Exception", e)
            isEngineRunning = false
        }
    }

    override suspend fun stopListening() {
        Log.d(TAG, "stopListening")
        cancelSilenceTimer()
        
        // Use the underlying instance directly to avoid accidental recreation
        speechRecognizer?.stopListening()
        // We don't set isEngineRunning = false here because we wait for onResults/onError
        _stateFlow.value = STTState.Idle
    }

    override fun release() {
        Log.d(TAG, "release")
        cancelSilenceTimer()
        speechRecognizer?.destroy()
        speechRecognizer = null
        currentListener = null
        scope.cancel()
    }

    private fun startSilenceTimer(onTimeout: suspend () -> Unit) {
        silenceJob?.cancel()
        silenceJob = scope.launch {
            delay(SILENCE_TIMEOUT_MS)
            Log.d(TAG, "Silence timer reached")
            
            // Finalize current speech
            speechRecognizer?.stopListening()
            delay(FINAL_RESULT_WAIT_MS)

            if (!hasRecognizedSpeech) {
                Log.d(TAG, "Timer: No speech recognized, finalizing...")
                isEngineRunning = false
                onTimeout()
            }
        }
    }

    private fun resetSilenceTimer(onTimeout: suspend () -> Unit) {
        startSilenceTimer(onTimeout)
    }

    private fun cancelSilenceTimer() {
        silenceJob?.cancel()
        silenceJob = null
    }

    private fun errorMessage(code: Int): String {
        return when (code) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permission denied"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error"
        }
    }
}
