package com.automaker.plugin;

import android.content.Context;
import android.os.Handler;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Timer;
import java.util.TimerTask;

import static org.junit.Assert.*;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        assertEquals("com.automaker.plugin", appContext.getPackageName());
    }


    private Handler mHandler = new Handler();

    private TimerTask mTimerTask = new TimerTask() {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    System.out.println("for what");
                }
            });
        }
    };

    @Test
    public void timerTask(){
        Timer mTimer = new Timer();
        mTimer.scheduleAtFixedRate(mTimerTask,0,1000L);
    }
}
