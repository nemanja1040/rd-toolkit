package org.rdtoolkit.ui.capture

import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rdtoolkit.model.diagnostics.DiagnosticsRepository
import org.rdtoolkit.model.diagnostics.RdtDiagnosticProfile
import org.rdtoolkit.model.session.SessionRepository
import org.rdtoolkit.model.session.TestReadableState
import org.rdtoolkit.model.session.TestSession
import org.rdtoolkit.model.session.TestSessionResult
import java.util.*
import kotlin.collections.HashMap

const val TAG = "CaptureViewModel"

class CaptureViewModel(var sessionRepository: SessionRepository,
                       var diagnosticsRepository: DiagnosticsRepository
) : ViewModel() {

    private var resolveTimer : CountDownTimer? = null

    private var readableTimer : CountDownTimer? = null

    private val testSession : MutableLiveData<TestSession> = MutableLiveData()

    private val testState : MutableLiveData<TestReadableState> = MutableLiveData()

    private val testProfile : MutableLiveData<RdtDiagnosticProfile> = MutableLiveData()

    private val resolveMillisecondsLeft : MutableLiveData<Long> = MutableLiveData()

    private val readableMillisecondsLeft : MutableLiveData<Long> = MutableLiveData()

    private val rawImageCapturePath : MutableLiveData<String> = MutableLiveData()

    private val testSessionResult : MutableLiveData<TestSessionResult> = MutableLiveData()

    fun getMillisUntilResolved() : LiveData<Long> {
        return resolveMillisecondsLeft
    }

    fun getMillisUntilExpired() : LiveData<Long> {
        return readableMillisecondsLeft
    }

    fun getTestState() : LiveData<TestReadableState> {
        return testState
    }

    fun getTestProfile() : LiveData<RdtDiagnosticProfile> {
        return testProfile
    }

    fun getTestSessionResult() : LiveData<TestSessionResult> {
        return testSessionResult
    }

    fun setResultValue(key: String, value: String) {
        val results = testSessionResult.value!!.results
        if (!results.containsKey(key) || results[key] != value) {
            results.put(key, value)
            testSessionResult.value = testSessionResult.value
        }
    }

    fun getRawImageCapturePath() : LiveData<String> {
        return rawImageCapturePath
    }

    fun setRawImageCapturePath(rawImagePath : String) {
        if (rawImageCapturePath.value != rawImagePath) {
            rawImageCapturePath.value = rawImagePath

            val result = testSessionResult.value!!
            result.timeRead = Date()
            result.rawCapturedImageFilePath = rawImagePath
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = sessionRepository.load(sessionId)

            val profile = diagnosticsRepository.getTestProfile(session.testProfileId)

            testSessionResult.postValue(TestSessionResult(session.sessionId, null, null, HashMap()))

            testProfile.postValue(profile)
            testSession.postValue(session)

            testState.postValue(session.getTestReadableState())

            startTimersForState(session, profile)
        }
    }

    private fun startTimersForState(session : TestSession, profile : RdtDiagnosticProfile) {
        viewModelScope.launch(Dispatchers.Main) {
            if (session.getTestReadableState() == TestReadableState.RESOLVING) {
                resolveTimer = object : CountDownTimer(session.timeResolved.time - System.currentTimeMillis(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        resolveMillisecondsLeft.postValue(millisUntilFinished)
                    }

                    override fun onFinish() {
                        Thread.sleep(500L)
                        testState.postValue(session.getTestReadableState())
                        startTimersForState(session, profile)
                    }
                }.start()
            } else if (session.getTestReadableState() == TestReadableState.READABLE) {
                readableTimer = object : CountDownTimer(session.timeExpired.time - System.currentTimeMillis(), 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        readableMillisecondsLeft.postValue(millisUntilFinished)
                    }

                    override fun onFinish() {
                        Thread.sleep(500L)
                        testState.postValue(session.getTestReadableState())
                    }
                }.start()
            }
        }
    }

    /**
     * Callback called when the ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        resolveTimer?.cancel()
        readableTimer?.cancel()
    }


    init {
        testState.value = TestReadableState.LOADING
    }
}