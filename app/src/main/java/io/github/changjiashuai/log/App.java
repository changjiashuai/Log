package io.github.changjiashuai.log;

import android.app.Application;

import io.github.changjiashuai.log2file.aspect.CJSLog;

/**
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2017/1/17 17:42.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CJSLog.setEnabled(true);
        String logDir = "log2fileDemo";
        if (BuildConfig.DEBUG) {
            CJSLog.initXlog(this, true, logDir);
        } else {
            CJSLog.initXlog(this, false, logDir);
        }
    }

    @Override
    public void onTerminate() {
        CJSLog.closeXLog();
        super.onTerminate();
    }
}
