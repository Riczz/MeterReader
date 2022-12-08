package com.riczz.meterreader.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;

import com.riczz.meterreader.enums.ImageType;

import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Locale;

public final class ImageHandler {

    private static int savedImageCount = 0;
    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;
    private static final String LOG_TAG = ImageHandler.class.getName();
    private static final String DIR_NAME = ".img";

    private final Context context;
    private boolean external = true;

    public ImageHandler(Context context) {
        this.context = context;
        String imageDir = getExternalStorageDir().getAbsolutePath();

        for (ImageType type : ImageType.values()) {
            File imageFolder = new File(imageDir + File.separator + type.getFolderName());

            if (!imageFolder.exists() && !imageFolder.mkdirs()) {
                Log.e(LOG_TAG, "Error creating directory " + imageFolder.getPath());
            }
        }
    }

    public Uri saveImage(Mat image, ImageType imageType) {
        return saveImage(image, imageType,
                imageType.getFolderName() + "_" + ++savedImageCount);
    }

    public Uri saveImage(Mat image, ImageType imageType, String imageName) {
        Bitmap bitmap = Bitmap.createBitmap(image.cols(), image.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(image, bitmap);

        File imageFile = createFile(imageName, imageType);

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

//    public Bitmap loadImage(String fileName) {
//        try (FileInputStream inputStream = new FileInputStream(createFile(fileName))) {
//            return BitmapFactory.decodeStream(inputStream);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    @NonNull
    private File createFile(String fileName) {
        return new File(getExternalStorageDir(), fileName + "." + COMPRESS_FORMAT.name().toLowerCase(Locale.ROOT));
    }

    @NonNull
    private File createFile(String fileName, ImageType imageType) {
        String imageDir = getExternalStorageDir().getAbsolutePath();
        String parentFolder = imageDir + File.separator + imageType.getFolderName();
        return new File(parentFolder, fileName + "." + COMPRESS_FORMAT.name().toLowerCase(Locale.ROOT));
    }

    private File getExternalStorageDir() {
        File directory;

        if (external) {
            directory = new File(context.getExternalFilesDir(null).getPath(), DIR_NAME);
        } else {
            directory = context.getDir(DIR_NAME, Context.MODE_PRIVATE);
        }

        if (!directory.exists() && !directory.mkdirs()) {
            Log.e(LOG_TAG, "Error creating directory " + directory.getPath());
        }

        return directory;
    }

    public ImageHandler setExternal(boolean external) {
        this.external = external;
        return this;
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
