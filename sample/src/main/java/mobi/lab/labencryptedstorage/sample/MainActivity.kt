package mobi.lab.labencryptedstorage.sample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import mobi.lab.labencryptedstorage.SampleClass

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        // Use our library here
        Toast.makeText(this, SampleClass().foo(), Toast.LENGTH_LONG).show()
    }
}
