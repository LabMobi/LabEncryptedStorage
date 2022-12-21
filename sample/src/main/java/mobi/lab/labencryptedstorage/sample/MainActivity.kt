package mobi.lab.labencryptedstorage.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import mobi.lab.labencryptedstorage.LabEncryptedStorageManager
import mobi.lab.labencryptedstorage.impl.SimpleEncryptedStorageDeviceCompatibilityTester

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        val manager = LabEncryptedStorageManager.Builder(this).storageCompatibilityTester(SimpleEncryptedStorageDeviceCompatibilityTester()).build()
        findViewById<TextView>(R.id.text_impl_name).text = manager.getOrSelectStorage().toString()
    }
}
