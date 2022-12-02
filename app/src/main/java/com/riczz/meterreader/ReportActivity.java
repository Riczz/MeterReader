package com.riczz.meterreader;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.res.ResourcesCompat;

import com.bumptech.glide.Glide;

import java.util.Locale;

public final class ReportActivity extends AppCompatActivity {
    private Button takePhotoButton;
    private Button galleryButton;
    private Button sendReportButton;
    private ImageView dialFrame;
    private ImageView imagePreview;
    private LinearLayout dialContainer;
    private Uri previewImageUri;

    private static final byte NUMBER_OF_DIALS = 8;
    private static final byte NUMBER_OF_WHOLE_DIALS = 5;

    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 501;
    private static final int REQUEST_CAMERA_PERMISSION = 500;
    private static final int ANALYZE_TAKEN_IMAGE = 402;
    private static final int REQUEST_TAKE_PHOTO = 401;
    private static final int REQUEST_PICK_IMAGE = 400;
    private static final String LOG_TAG = ReportActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_report);

        dialFrame = findViewById(R.id.dialFrame);
        dialContainer = findViewById(R.id.dials_container);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        galleryButton = findViewById(R.id.galleryButton);
        sendReportButton = findViewById(R.id.sendReportButton);
        imagePreview = findViewById(R.id.photoPreview);

        Glide.with(this)
                .load(ResourcesCompat.getDrawable(getResources(), R.drawable.dial_frame, null))
                .into(dialFrame);

        setupDials();

        galleryButton.setOnClickListener(button -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_PICK_IMAGE);
        });

        takePhotoButton.setOnClickListener(button -> {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CAMERA_PERMISSION);
                return;
            } else if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                return;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, getString(R.string.take_photo_title));
            values.put(MediaStore.Images.Media.DESCRIPTION, getString(R.string.take_photo_description));
            previewImageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, previewImageUri);
            startActivityForResult(intent, REQUEST_TAKE_PHOTO);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            Log.e(LOG_TAG, "Activity responded with result code " + resultCode);
            return;
        }

        switch (requestCode) {
            case REQUEST_PICK_IMAGE:
                assert  data != null;
                previewImageUri = data.getData();
            case REQUEST_TAKE_PHOTO:
                try {
                    Bitmap previewImage = MediaStore.Images.Media.getBitmap(getContentResolver(), previewImageUri);
                    Log.d(LOG_TAG, "Bitmap image saved successfully. URI: " + previewImageUri);
//                    imagePreview.setImageBitmap(previewImage);

                    Intent intent = new Intent(this, PreviewActivity.class);
                    intent.setData(previewImageUri);
                    startActivityForResult(intent, ANALYZE_TAKEN_IMAGE);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }
                break;
            case ANALYZE_TAKEN_IMAGE:
                assert  data != null;
                if (data.hasExtra("DIALS_VALUE")) {
                    setDialValues(data.getFloatExtra("DIALS_VALUE", 0.0f));
                    sendReportButton.setEnabled(true);
                } else {
                    Toast.makeText(this,
                            getString(R.string.intent_data_error), Toast.LENGTH_SHORT).show();
                }
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoButton.callOnClick();
            }
        } else if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePhotoButton.callOnClick();
            }
        }
    }

    private boolean setDialValues(double number) {
        if (number < 0.0 || number >= Math.pow(10d, (double) NUMBER_OF_WHOLE_DIALS)) {
            Log.e(LOG_TAG, "Dial number out of bounds!");
            return false;
        }

        String[] dialStrings = String.format(Locale.getDefault(), "%." +
                (NUMBER_OF_DIALS - NUMBER_OF_WHOLE_DIALS) + "f", number).split(",");

        byte wholeDigits = (byte) dialStrings[0].length();
        byte fractionalDigits = (byte) dialStrings[1].length();

//        for (int i = 0; i < NUMBER_OF_WHOLE_DIALS; i++) {
//            NumberPicker dial = (NumberPicker) dialContainer.getChildAt(i);
//
//            if (i >= NUMBER_OF_WHOLE_DIALS - wholeDigits) {
//                dial.setValue(Character.getNumericValue(dialStrings[0].charAt()));
//            } else {
//                dial.setValue(0);
//            }
//        }

        for (char digit : dialStrings[0].toCharArray()) {
            NumberPicker dial = (NumberPicker) dialContainer.getChildAt(NUMBER_OF_WHOLE_DIALS - (wholeDigits--));
            dial.setValue(Character.getNumericValue(digit));
        }

        for (char digit : dialStrings[1].toCharArray()) {
            NumberPicker dial = (NumberPicker) dialContainer.getChildAt(NUMBER_OF_DIALS - (fractionalDigits--) + 1);
            dial.setValue(Character.getNumericValue(digit));
        }

        return true;
    }

    private void setupDials() {
        for (int i = 0; i < dialContainer.getChildCount(); i++) {
            if (i == NUMBER_OF_WHOLE_DIALS) continue;

            NumberPicker dial = (NumberPicker) dialContainer.getChildAt(i);
            dial.setMinValue(0);
            dial.setMaxValue(9);
        }
    }

    private String getRealPathFromURI(Uri contentUri) {
        String res = null;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);

        if (cursor.moveToFirst()) {

            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            res = cursor.getString(column_index);
        }

        cursor.close();
        return res;
    }
}