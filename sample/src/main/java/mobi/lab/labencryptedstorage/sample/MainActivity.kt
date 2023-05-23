package mobi.lab.labencryptedstorage.sample

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import io.reactivex.rxjava3.schedulers.Schedulers
import mobi.lab.labencryptedstorage.LabEncryptedStorageManager
import mobi.lab.labencryptedstorage.entity.EncryptionPreferredType
import mobi.lab.labencryptedstorage.inter.KeyValueStorage

class MainActivity : AppCompatActivity() {
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        createRxJavaUndeliverableExceptionHandler()
        updateResult(getString(R.string.state_press_the_button_to_run_the_test))
        findViewById<Button>(R.id.button_run_test).setOnClickListener { runTest() }
    }

    @Suppress("SpreadOperator")
    private fun runTest() {
        updateResult(getString(R.string.state_testing))

        // Create a manager via the builder
        val manager = with(LabEncryptedStorageManager.Builder(this)) {
            // Configure if needed
            // For example, allow hardware-key based encrypted storage
            encryptionEnabled(true)
            // For example, add device that should not use encrypted storage ever
            // First device
            encryptionBlocklist("Samsung FirstDevice")
            // Add multiple
            val blocklist = arrayOf("Google SomePixel", "Samsung SomeOtherDeviceModel")
            encryptionBlocklist(*blocklist)
            // Set the preferred encryption type if needed.
            encryptionPreferredType(EncryptionPreferredType.Tee)
            // Build it
            build()
        }

        // Get the best storage to be used
        val storage: KeyValueStorage = manager.getOrSelectStorage()

        // Use it (in non-UI-thread)
        disposable?.dispose()
        disposable = Single
            .fromCallable { testReadWrite(storage) }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::onTestEnded, ::onTestFailed)
    }

    private fun testReadWrite(storage: KeyValueStorage): String {
        val key1 = "key1"
        val value1 = "value1"

        // Write a value
        storage.store(key1, value1)

        // Read out the same value
        val value1b = storage.read<String>(key1, String::class.java)
        assert(value1 == value1b) { "Stored value does not match the original!" }

        // Delete the value
        storage.delete(key1)

        // Make sure it is deleted
        val value1C = storage.read<String>(key1, String::class.java)
        assert(value1C == null) { "Value was not correctly deleted!" }

        return "Test successful with ${storage.getStorageName()}"
    }

    private fun onTestFailed(throwable: Throwable) {
        updateResult("Test failed: ${throwable.stackTraceToString()}")
    }

    private fun onTestEnded(result: String) {
        updateResult(result)
    }

    private fun updateResult(resultString: String) {
        findViewById<TextView>(R.id.text_test_result).text = resultString
    }

    private fun createRxJavaUndeliverableExceptionHandler() {
        RxJavaPlugins.setErrorHandler { e ->
            if (e is UndeliverableException) {
                // Merely log undeliverable exceptions
                Log.wtf("MainActivity", "${e.message}\n${e.stackTraceToString()}")
            } else {
                // Forward all others to current thread's uncaught exception handler
                Thread.currentThread().also { thread ->
                    thread.uncaughtExceptionHandler?.uncaughtException(thread, e)
                }
            }
        }
    }
}
