package com.riczz.meterreader;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;

import com.google.android.material.button.MaterialButton;
import com.riczz.meterreader.database.DBHandler;
import com.riczz.meterreader.database.model.Config;
import com.riczz.meterreader.enums.ImageType;
import com.riczz.meterreader.enums.MeterType;
import com.riczz.meterreader.view.ImageCategoryViewer;

import org.apache.commons.lang3.StringUtils;

import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ReportActivity extends AppCompatActivity {
    private static final String LOG_TAG = ReportActivity.class.getName();
    public static final char DECIMAL_SEPARATOR = DecimalFormatSymbols.getInstance().getDecimalSeparator();
    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 501;
    private static final int REQUEST_CAMERA_PERMISSION = 500;
    private static final int ANALYZE_TAKEN_IMAGE = 402;
    private static final int REQUEST_TAKE_PHOTO = 401;
    private static final int REQUEST_PICK_IMAGE = 400;

    private ConstraintLayout dialContainer;
    private ConstraintLayout reportConstraintLayout;
    private MaterialButton galleryButton;
    private MaterialButton takePhotoButton;
    private ImageCategoryViewer previewImageViewer;
    private List<NumberPicker> dials = new ArrayList<>();

    private DBHandler dbHandler;
    private Uri previewImageUri;

    private MeterType meterType;
    private boolean isGasMeter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meter_report);

        dbHandler = new DBHandler(this);
        isGasMeter = getIntent().getBooleanExtra("IS_GAS_METER", true);
        meterType = isGasMeter ? MeterType.GAS : MeterType.ELECTRIC;

        ActionBar actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setTitle(isGasMeter ? R.string.gas_meter_report_title : R.string.electric_meter_report_title);
        actionBar.setDisplayHomeAsUpEnabled(true);

        takePhotoButton = findViewById(R.id.takePhotoButton);
        galleryButton = findViewById(R.id.galleryButton);
        reportConstraintLayout = findViewById(R.id.reportConstraintLayout);
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
                    Intent intent = new Intent(this, PreviewActivity.class);
                    intent.setData(previewImageUri);
                    intent.putExtra("IS_GAS_METER", isGasMeter);
                    startActivityForResult(intent, ANALYZE_TAKEN_IMAGE);
                } catch (Exception e) {
                    Log.e(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }
                break;
            case ANALYZE_TAKEN_IMAGE:
                assert  data != null;
                if (null != previewImageViewer) removePreview();
                if (data.hasExtra("DIALS_VALUE")) {
                    setDialValues(data.getDoubleExtra("DIALS_VALUE", 0.0d));
                    addPreview();
                } else {
                    Toast.makeText(this,
                            getString(R.string.intent_data_error),
                            Toast.LENGTH_SHORT
                    ).show();
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

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private void addPreview() {
        previewImageViewer = new ImageCategoryViewer(this, meterType);

        Config config = isGasMeter ?
                dbHandler.getGasMeterConfig() : dbHandler.getElectricMeterConfig();

        if (config != null && config.isUseColorCorrection()) {
            previewImageViewer.addImageCategory(ImageType.COLOR_CORRECTION);
        }
        if (isGasMeter) {
            previewImageViewer.addImageCategory(ImageType.SKEWNESS_CORRECTION);
        }
        previewImageViewer.addImageCategory(ImageType.FRAME_DETECTION);
        previewImageViewer.addImageCategory(ImageType.DIAL_SEARCH);
        previewImageViewer.addImageCategory(ImageType.DIGIT_DETECTION);
        previewImageViewer.refreshView();
        previewImageViewer.setSelected(ImageType.DIGIT_DETECTION, -1);
        reportConstraintLayout.addView(previewImageViewer);

        ConstraintLayout.LayoutParams layoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                getResources().getDimensionPixelSize(R.dimen.image_viewer_report_height_dp)
        );
        layoutParams.setMargins(
                getResources().getDimensionPixelSize(R.dimen.image_viewer_report_margin_start_dp), 0,
                getResources().getDimensionPixelSize(R.dimen.image_viewer_report_margin_end_dp), 0
        );
        previewImageViewer.setLayoutParams(layoutParams);

        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(reportConstraintLayout);
        constraintSet.connect(
                previewImageViewer.getId(), ConstraintSet.TOP,
                R.id.dialRelativeLayout, ConstraintSet.BOTTOM
        );
        constraintSet.connect(
                previewImageViewer.getId(), ConstraintSet.START,
                reportConstraintLayout.getId(), ConstraintSet.START
        );
        constraintSet.connect(
                previewImageViewer.getId(), ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END
        );
        constraintSet.connect(
                previewImageViewer.getId(), ConstraintSet.BOTTOM,
                R.id.linearLayout, ConstraintSet.TOP
        );
        constraintSet.applyTo(reportConstraintLayout);
        reportConstraintLayout.invalidate();
    }

    private void removePreview() {
        reportConstraintLayout.removeView(previewImageViewer);
        reportConstraintLayout.invalidate();
        previewImageViewer = null;
    }

    private boolean setDialValues(double number) {
        int numberOfDigits = meterType.getNumberOfDigits();
        int numberOfWholeDigits = meterType.getWholeDigits();
        int numberOfFractionalDigits = meterType.getFractionalDigits();

        if (number < 0.0 || number >= Math.pow(10d, numberOfWholeDigits)) {
            Log.e(LOG_TAG, "Dial number out of bounds!");
            return false;
        }

        String dialString = String
                .format(Locale.getDefault(), "%." + numberOfFractionalDigits + "f", number)
                .replace(String.valueOf(DECIMAL_SEPARATOR), "");

        dialString = StringUtils.leftPad(dialString, numberOfDigits, '0');

        dials = dials.stream()
                .sorted(Comparator.comparingDouble(View::getX))
                .collect(Collectors.toList());

        for (int i = 0; i < dials.size(); i++) {
            dials.get(i).setValue(Character.getNumericValue(dialString.charAt(i)));
        }

        return true;
    }

    private void setupDials() {
        View dialFrameView = getLayoutInflater()
                .inflate(isGasMeter ? R.layout.gas_dial_frame : R.layout.electric_dial_frame,
                        findViewById(R.id.dialRelativeLayout),
                        true
                );

        dialContainer = dialFrameView.findViewById(isGasMeter ? R.id.gasDialFrame : R.id.electricDialFrame);

        for (int i = 0; i < dialContainer.getChildCount(); i++) {
            View child = dialContainer.getChildAt(i);

            if (child instanceof NumberPicker) {
                child.setFocusable(false);
                child.setClickable(false);
                ((NumberPicker)child).setMinValue(0);
                ((NumberPicker)child).setMaxValue(9);
                ((NumberPicker) child).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                dials.add((NumberPicker) child);
            }
        }
    }
}