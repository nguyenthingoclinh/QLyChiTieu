package com.nhom08.qlychitieu.csdl;
import android.content.Context;
import androidx.room.Room;
public class DatabaseClient {
    private static AppDatabase appDatabase;
    private static DatabaseClient instance;

    private DatabaseClient(Context context) {
        appDatabase = Room.databaseBuilder(context, AppDatabase.class, "app_database").build();
    }

    public static synchronized DatabaseClient getInstance(Context context){
        if(instance == null){
            instance = new DatabaseClient(context);
        }
        return instance;
    }

    public AppDatabase getAppDatabase(){
        return appDatabase;
    }

}
