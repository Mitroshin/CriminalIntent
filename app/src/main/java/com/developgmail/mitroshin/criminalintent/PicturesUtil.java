package com.developgmail.mitroshin.criminalintent;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;

public class PicturesUtil {
    //TODO переделать. Без комментария часть кода о масштабировании не очевидна
    // Масштабирует Bitmap под размер конкретной активности
    public static Bitmap getScaledBitmap(String path, Activity activity) {
        Point size = new Point();

        activity.getWindowManager().getDefaultDisplay().getSize(size);
        return getScaledBitmap(path, size.x, size.y);
    }

    public static Bitmap getScaledBitmap(String path, int desWidth, int destHeight) {
        // Чтение размера оригинального зиображения в памяти на диске
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        float srcWidth = options.outWidth;
        float srcHeight = options.outHeight;

        // Определение степени масштабирования
        int inSampleSize = 1;
        // Если нужно масштабировать изображение
        if (srcHeight > destHeight || srcWidth > desWidth) {
            // альбомная ориентация исходника
            if (srcWidth > srcHeight) {
                inSampleSize = Math.round(srcHeight / destHeight);
                // портретная ориентация исходника
            } else {
                inSampleSize = Math.round(srcWidth / desWidth);
            }
        }

        options = new BitmapFactory.Options();
        options.inSampleSize = inSampleSize;

        // Повторно считываем изображение с новыми параметрами для размера
        return BitmapFactory.decodeFile(path, options);
    }
}
