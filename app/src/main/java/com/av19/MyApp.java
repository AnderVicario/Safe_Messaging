package com.av19;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

public class MyApp extends Application implements Application.ActivityLifecycleCallbacks {
    private final AtomicInteger activityCount = new AtomicInteger(0);

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activityCount.getAndIncrement();
        Log.d("MyApp", "Aplicación en primer plano");
    }

    @SuppressLint("ObsoleteSdkInt")
    @Override
    public void onActivityStopped(Activity activity) {
        int count = activityCount.decrementAndGet();
        Log.d("MyApp", "Activity detenida, contador: " + count);
        if (count == 0) {
            Log.d("MyApp", "App en segundo plano - Servicio ya debería estar corriendo");
        }
    }

    // Resto de métodos del ciclo de vida (pueden estar vacíos)
    @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}
    @Override public void onActivityResumed(Activity activity) {}
    @Override public void onActivityPaused(Activity activity) {}
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
    @Override public void onActivityDestroyed(Activity activity) {}
}

