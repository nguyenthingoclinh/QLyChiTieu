package com.nhom08.qlychitieu;

import android.app.Application;

import androidx.room.Room;

import com.nhom08.qlychitieu.csdl.AppDatabase;
import com.nhom08.qlychitieu.mo_hinh.User;
import com.nhom08.qlychitieu.truy_van.UserDAO;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyApplication extends Application {
    private AppDatabase database;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        database = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "app_database")
                .build();
    }

    public AppDatabase getDatabase() {
        return database;
    }
}