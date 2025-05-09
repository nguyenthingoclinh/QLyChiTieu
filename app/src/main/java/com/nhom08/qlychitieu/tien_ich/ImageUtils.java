package com.nhom08.qlychitieu.tien_ich;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageUtils {
    private final Context context;

    public ImageUtils(Context context) {
        this.context = context;
    }

    public File getReceiptsDirectory() {
        File directory = new File(context.getFilesDir(), Constants.RECEIPTS_DIR);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    public String saveImage(Uri sourceUri) {
        try {
            String fileName = "receipt_" + new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                    .format(new Date()) + ".jpg";
            File outputFile = new File(getReceiptsDirectory(), fileName);

            // Đọc và resize ảnh
            Bitmap bitmap = getBitmapFromUri(sourceUri);
            if (bitmap == null) return null;

            Bitmap resizedBitmap = resizeBitmap(bitmap);

            // Lưu ảnh
            FileOutputStream fos = new FileOutputStream(outputFile);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, Constants.IMAGE_QUALITY, fos);
            fos.close();

            // Cleanup
            if (bitmap != resizedBitmap) bitmap.recycle();
            resizedBitmap.recycle();

            return outputFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e("ImageUtils", "Error saving image: " + e.getMessage());
            return null;
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input);
        if (input != null) input.close();
        return bitmap;
    }

    private Bitmap resizeBitmap(Bitmap original) {
        float scale = Math.min(
                (float) Constants.MAX_IMAGE_DIMENSION / original.getWidth(),
                (float) Constants.MAX_IMAGE_DIMENSION / original.getHeight()
        );

        if (scale >= 1) return original;

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);
        return Bitmap.createBitmap(
                original,
                0, 0,
                original.getWidth(),
                original.getHeight(),
                matrix,
                true
        );
    }
}
