package mobi.lab.labencryptedstorage

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
public abstract class BaseTestCase {
    /**
     * Helper to get the Context.
     *
     * @return Target context
     */
    public fun getContextForTarget(): Context {
        return InstrumentationRegistry.getInstrumentation().targetContext
    }
}
