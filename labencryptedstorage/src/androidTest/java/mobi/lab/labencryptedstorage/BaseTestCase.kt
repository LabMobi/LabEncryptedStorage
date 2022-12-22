package mobi.lab.labencryptedstorage

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry

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
