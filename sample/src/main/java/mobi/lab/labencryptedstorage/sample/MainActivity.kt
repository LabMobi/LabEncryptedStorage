package mobi.lab.labencryptedstorage.sample

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import mobi.lab.labencryptedstorage.LabEncryptedStorageManager

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        findViewById<TextView>(R.id.text_impl_name).text = LabEncryptedStorageManager.getLastSelectedStorageImplOrSelectOneNow(this).toString()
    }
}
