package io.github.changjiashuai.log;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.tencent.mars.xlog.Log;

import io.github.changjiashuai.log2file.weaving.DebugLog;

@DebugLog
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: ");
    }
}
