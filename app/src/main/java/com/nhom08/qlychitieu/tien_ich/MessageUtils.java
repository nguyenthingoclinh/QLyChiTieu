package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.StringRes;

public class MessageUtils {
    private final Context context;

    public MessageUtils(Context context) {
        this.context = context;
    }

    public void showError(@StringRes int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }

    public void showError(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void showSuccess(@StringRes int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }

    public void showSuccess(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public void showInfo(@StringRes int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }
    public void showInfo(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
