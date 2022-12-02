package com.riczz.meterreader.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Locale;

public final class ImageHandler {

    private static int savedImageCount = 0;
    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final String LOG_TAG = ImageHandler.class.getName();

    private final Context context;
    private boolean external = true;
    private String directoryName = ".img";

    public ImageHandler(Context context) {
        this.context = context;
    }

    public ImageHandler setExternal(boolean external) {
        this.external = external;
        return this;
    }

    public ImageHandler setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
        return this;
    }

    public Uri saveImage(Mat image) {
        final String fileName = String.format(Locale.getDefault(),
                "image_%d.jpg", savedImageCount++);

        return saveImage(image, fileName);
    }

    public Uri saveImage(Mat image, String fileName) {
        File imageFile = createFile(fileName);
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);

        try (FileOutputStream outputStream = new FileOutputStream(imageFile)) {
            bitmap.compress(COMPRESS_FORMAT, 100, outputStream);
            Uri resultURI = Uri.fromFile(imageFile);
            Log.d(LOG_TAG, "Image saved successfully: " + resultURI.getPath());
            return resultURI;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Bitmap loadImage(String fileName) {
        try (FileInputStream inputStream = new FileInputStream(createFile(fileName))) {
            return BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @NonNull
    private File createFile(String fileName) {
        File directory;

        if (external) {
            directory = getExternalStorageDir(directoryName);
        } else {
            directory = context.getDir(directoryName, Context.MODE_PRIVATE);
        }

        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(LOG_TAG, "Error creating directory " + directory.getPath());
        }

        return new File(directory, fileName + "." + COMPRESS_FORMAT.name().toLowerCase(Locale.ROOT));
    }

    private File getExternalStorageDir(String directoryName) {
        return new File(context.getExternalFilesDir(null).getPath(), directoryName);
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
