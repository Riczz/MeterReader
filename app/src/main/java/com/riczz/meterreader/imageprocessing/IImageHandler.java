package com.riczz.meterreader.imageprocessing;

import android.net.Uri;
import android.os.Environment;

import com.riczz.meterreader.enums.ImageType;

import org.opencv.core.Mat;

public interface IImageHandler {
    Uri saveImage(Mat image, ImageType imageType);

    Uri saveImage(Mat image, ImageType imageType, String imageName);

    //    public Bitmap loadImage(String fileName);

    ImageHandler setExternal(boolean external);

    static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
