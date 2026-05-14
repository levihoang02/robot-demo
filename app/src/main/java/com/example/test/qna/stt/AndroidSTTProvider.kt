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
import kotlinx.coroutines.launch

class AndroidSTTProvider(
    private val context: Context
) : SpeechToTextProvider {

    companion object {

        private const val TAG = "AndroidSTT"

        private const val SILENCE_TIMEOUT_MS = 3000L

        private const val FINAL_RESULT_WAIT_MS = 1200L

        /**
         * Ignore tiny noise
         */
        private const val RMS_THRESHOLD = 5f

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

    private var isListening = false

    private var lastSpeechTimestamp = 0L

    /**
     * True when recognizer
     * already heard something
     */
    private var hasRecognizedSpeech = false

    private var lastPartialTranscript: String? = null

    private val _stateFlow =
        MutableStateFlow<STTState>(
            STTState.Idle
        )

    override val stateFlow =
        _stateFlow.asStateFlow()

    init {

        speechRecognizer =
            SpeechRecognizer
                .createSpeechRecognizer(
                    context
                )
    }

    override val transcriptFlow:
            Flow<STTEvent> =

        callbackFlow {

            val listener =
                object : RecognitionListener {

                    override fun onReadyForSpeech(
                        params: Bundle?
                    ) {

                        Log.d(
                            TAG,
                            "onReadyForSpeech"
                        )

                        _stateFlow.value =
                            STTState.Listening

                        startSilenceTimer {

                            Log.d(
                                TAG,
                                "Silence timeout"
                            )

                            trySend(
                                STTEvent.SilenceTimeout
                            )
                        }
                    }

                    override fun onBeginningOfSpeech() {

                        Log.d(
                            TAG,
                            "onBeginningOfSpeech"
                        )

                        resetSilenceTimer {

                            Log.d(
                                TAG,
                                "Silence timeout"
                            )

                            trySend(
                                STTEvent.SilenceTimeout
                            )
                        }
                    }

                    override fun onRmsChanged(
                        rmsdB: Float
                    ) {
                        val now =
                            SystemClock.elapsedRealtime()

                        val passedDebounce =
                            now - lastSpeechTimestamp >
                                    RMS_DEBOUNCE_MS

                        if (
                            rmsdB > RMS_THRESHOLD &&
                            passedDebounce
                        ) {
                            Log.d(
                                TAG,
                                "Voice detected (RMS: $rmsdB)"
                            )

                            lastSpeechTimestamp = now

                            resetSilenceTimer {

                                Log.d(
                                    TAG,
                                    "Silence timeout"
                                )

                                trySend(
                                    STTEvent.SilenceTimeout
                                )
                            }
                        }
                    }

                    override fun onBufferReceived(
                        buffer: ByteArray?
                    ) {}

                    override fun onEndOfSpeech() {

                        Log.d(
                            TAG,
                            "onEndOfSpeech"
                        )

                        cancelSilenceTimer()

                        _stateFlow.value =
                            STTState.Processing
                    }

                    override fun onPartialResults(
                        partialResults: Bundle?
                    ) {

                        hasRecognizedSpeech = true

                        val partial =
                            partialResults
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )
                                ?.firstOrNull()

                        if (!partial.isNullOrBlank()) {
                            lastPartialTranscript = partial
                            Log.d(
                                TAG,
                                "Partial: $partial"
                            )
                        }

                        resetSilenceTimer {

                            Log.d(
                                TAG,
                                "Silence timeout"
                            )

                            trySend(
                                STTEvent.SilenceTimeout
                            )
                        }
                    }

                    override fun onResults(
                        results: Bundle?
                    ) {

                        cancelSilenceTimer()

                        hasRecognizedSpeech = true

                        var matches =
                            results
                                ?.getStringArrayList(
                                    SpeechRecognizer.RESULTS_RECOGNITION
                                )

                        if (matches.isNullOrEmpty() && !lastPartialTranscript.isNullOrBlank()) {
                            Log.d(TAG, "Final results null/empty, falling back to partial: $lastPartialTranscript")
                            matches = arrayListOf(lastPartialTranscript!!)
                        }

                        Log.d(
                            TAG,
                            "Results: $matches"
                        )

                        if (matches.isNullOrEmpty()) {

                            Log.w(
                                TAG,
                                "Empty final results"
                            )

                            isListening = false

                            _stateFlow.value =
                                STTState.Idle

                            return
                        }

                        val text =
                            matches.firstOrNull()

                        if (!text.isNullOrBlank()) {

                            Log.d(
                                TAG,
                                "Final transcript: $text"
                            )

                            trySend(
                                STTEvent.Transcript(
                                    text
                                )
                            )
                        }

                        isListening = false

                        _stateFlow.value =
                            STTState.Idle
                    }

                    override fun onError(
                        error: Int
                    ) {

                        cancelSilenceTimer()

                        isListening = false

                        val message =
                            errorMessage(error)

                        Log.e(
                            TAG,
                            "STT Error [$error]: $message"
                        )

                        trySend(
                            STTEvent.Error(
                                error,
                                message
                            )
                        )

                        _stateFlow.value =
                            STTState.Error(
                                code = error,
                                message = message
                            )
                    }

                    override fun onEvent(
                        eventType: Int,
                        params: Bundle?
                    ) {}
                }

            speechRecognizer
                ?.setRecognitionListener(
                    listener
                )

            awaitClose {

                Log.d(
                    TAG,
                    "callbackFlow closed"
                )

                cancelSilenceTimer()
            }
        }
            .buffer(1)
            .flowOn(
                Dispatchers.Main.immediate
            )

    override suspend fun startListening() {

        Log.d(
            TAG,
            "startListening"
        )

        if (isListening) {

            Log.d(
                TAG,
                "Already listening"
            )

            return
        }

        val recognizer =
            speechRecognizer ?: run {

                Log.e(
                    TAG,
                    "SpeechRecognizer is null"
                )

                return
            }

        hasRecognizedSpeech = false
        lastPartialTranscript = null

        val intent =
            Intent(
                RecognizerIntent
                    .ACTION_RECOGNIZE_SPEECH
            ).apply {

                putExtra(
                    RecognizerIntent
                        .EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent
                        .LANGUAGE_MODEL_FREE_FORM
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_LANGUAGE,
                    "vi-VN"
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_PARTIAL_RESULTS,
                    true
                )

                /**
                 * Android internal silence handling
                 */
                putExtra(
                    RecognizerIntent
                        .EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                    2000
                )

                putExtra(
                    RecognizerIntent
                        .EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                    1500
                )
            }

        isListening = true

        recognizer.startListening(intent)
    }

    override suspend fun stopListening() {

        Log.d(
            TAG,
            "stopListening"
        )

        cancelSilenceTimer()

        isListening = false

        speechRecognizer?.stopListening()

        _stateFlow.value =
            STTState.Idle
    }

    override fun release() {

        Log.d(
            TAG,
            "release"
        )

        cancelSilenceTimer()

        speechRecognizer?.destroy()

        speechRecognizer = null

        scope.cancel()
    }

    private fun startSilenceTimer(
        onTimeout: suspend () -> Unit
    ) {

        silenceJob?.cancel()

        silenceJob = scope.launch {

            delay(SILENCE_TIMEOUT_MS)

            Log.d(
                TAG,
                "Timeout reached"
            )

            /**
             * Ask recognizer to finalize
             * current speech
             */
            speechRecognizer?.stopListening()

            /**
             * Wait final result callback
             */
            delay(FINAL_RESULT_WAIT_MS)

            /**
             * Truly no speech
             */
            if (!hasRecognizedSpeech) {

                Log.d(
                    TAG,
                    "No speech recognized"
                )

                isListening = false

                onTimeout()
            }
        }
    }

    private fun resetSilenceTimer(
        onTimeout: suspend () -> Unit
    ) {

        startSilenceTimer(onTimeout)
    }

    private fun cancelSilenceTimer() {

        silenceJob?.cancel()

        silenceJob = null
    }

    private fun errorMessage(
        code: Int
    ): String {

        return when (code) {

            SpeechRecognizer.ERROR_AUDIO ->
                "Audio recording error"

            SpeechRecognizer.ERROR_CLIENT ->
                "Client error"

            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                "Permission denied"

            SpeechRecognizer.ERROR_NETWORK ->
                "Network error"

            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "Network timeout"

            SpeechRecognizer.ERROR_NO_MATCH ->
                "No speech match"

            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                "Recognizer busy"

            SpeechRecognizer.ERROR_SERVER ->
                "Server error"

            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                "Speech timeout"

            else ->
                "Unknown error"
        }
    }
}