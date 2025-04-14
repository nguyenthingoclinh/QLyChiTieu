package com.nhom08.qlychitieu;

import android.app.Application;

import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.csdl.DatabaseClient;

public class MyApplication extends Application {
    private static AppDatabase database;
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        database = DatabaseClient.getInstance(this).getAppDatabase();
    }

    public static AppDatabase getDatabase() {
        return database;
    }
    public static MyApplication getInstance() {
        return instance;
    }
}
