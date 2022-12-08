package com.riczz.meterreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.helper.widget.Carousel;

import com.riczz.meterreader.config.ConfigHelper;
import com.riczz.meterreader.exception.FrameDetectionException;
import com.riczz.meterreader.exception.NumberRecognizationException;
import com.riczz.meterreader.imageprocessing.ElectricityMeterImageRecognizer;
import com.riczz.meterreader.imageprocessing.MeterImageRecognizer;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PreviewActivity extends AppCompatActivity {

    private static final int TASK_COUNT = 3;
    private static final String LOG_TAG = PreviewActivity.class.getName();

    private Bitmap rawImage;
    private Button retryButton;
    private Carousel previewCarousel;
    private ConfigHelper configHelper;
    private Drawable progressCircle;
    private ProgressBar progressBar;
    private String errorText;
    private TextView progressText;
    private Uri rawImageUri;
    private ViewFlipper viewFlipper;

    private Mat dialsImage;
    private MeterImageRecognizer meterImageRecognizer;
    private ExecutorService executorService;
    private Handler handler;

    private int currentTaskNumber = 1;
    private double dialsValue = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        checkIntentData(getIntent());

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        previewCarousel = findViewById(R.id.previewCarousel);
        retryButton = findViewById(R.id.retryRecognitionButton);
        viewFlipper = findViewById(R.id.previewViewFlipper);
        viewFlipper.setDisplayedChild(0);

        configHelper = new ConfigHelper(this);

        meterImageRecognizer = getIntent().getBooleanExtra("IS_GAS_METER", true) ?
                new MeterImageRecognizer(this) : new ElectricityMeterImageRecognizer(this);

        progressCircle = progressBar.getIndeterminateDrawable();
        executorService = Executors.newSingleThreadExecutor();
        handler = new Handler(Looper.getMainLooper());

        retryButton.setOnClickListener(button -> executeDialRecognition());
        executeDialRecognition();
    }

    private void executeDialRecognition() {
        resetProgress();
        executorService.execute(() -> {
            updateProgress(getString(R.string.progress_frame_detection));
            Intent resultIntent = new Intent();

            try {
                dialsImage = meterImageRecognizer.detectDialFrame(rawImage);
            } catch (FrameDetectionException e) {
                Log.e(LOG_TAG, "There was an error during dial frame detection phase.");
                errorText = getString(R.string.frame_detection_error);
                runOnUiThread(() -> showFinishedDialog(false));
            }

            updateProgress(getString(R.string.progress_dial_value_detection));

            try {
                dialsValue = meterImageRecognizer.getDialReadings(dialsImage);
            } catch (NumberRecognizationException e) {
                Log.e(LOG_TAG, "There was an error during dial numbers detection phase.");
                errorText = getString(R.string.dial_value_detection_error);
                runOnUiThread(() -> showFinishedDialog(false));
            }

            updateProgress(getString(R.string.progress_finalization));
            meterImageRecognizer.saveResultImages();
//            List<Pair<Mat, Uri>> resultImages = new ArrayList<>(meterImageRecognizer.getResultImages().values());
//
//            previewCarousel.setAdapter(new Carousel.Adapter() {
//                @Override
//                public int count() {
//                    return 4;
//                }
//
//                @Override
//                public void populate(View view, int index) {
//                    int previewImageId = getResources().getIdentifier("previewImageView" + index, "id", getPackageName());
//                    AppCompatImageView previewImage = findViewById(previewImageId);
//                    Uri resultImageUri = resultImages.get(index).second;
//
//                    runOnUiThread(() -> {
//                        previewImage.setImageURI(resultImageUri);
////                        Glide.with(getApplicationContext())
////                                .load(resultImageUri)
////                                .into(previewImage);
//                    });
//                }
//
//                @Override
//                public void onNewItem(int index) {}
//            });

//            //TODO:
//            for (int i = 0; i <= 4; i++) {
//                int previewImageId = getResources().getIdentifier("previewImageView" + i, "id", getPackageName());
//                ImageView previewImage = findViewById(previewImageId);
//                Uri resultImageUri = resultImages.get(i).second;
//
//                runOnUiThread(() -> {
//                    Glide.with(this)
//                            .load(resultImageUri)
//                            .into(previewImage);
//                });
//            }

            updateProgress("");
            runOnUiThread(this::showFinishedDialog);
        });
    }

    private void showFinishedDialog() {
        showFinishedDialog(true);
    }

    private void showFinishedDialog(boolean success) {
        Intent resultIntent = new Intent();
        AlertDialog.Builder resultDialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.results));

        if (success) {
            resultDialog
                    .setMessage(String.format(getString(R.string.results_description), dialsValue))
                    .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                        dialog.dismiss();
                        resultIntent.putExtra("DIALS_VALUE", dialsValue);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .setNegativeButton(getString(R.string.details), (dialog, i) -> {
                        dialog.dismiss();
                        resultIntent.putExtra("DIALS_VALUE", dialsValue);
                        setResult(RESULT_OK, resultIntent);
                        viewFlipper.setDisplayedChild(1);
                    });
        } else {
            resultDialog
                    .setMessage(errorText)
                    .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                        dialog.dismiss();
                        setResult(RESULT_CANCELED);
                        finish();
                    });
        }

        Dialog dialog = resultDialog.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void updateProgress(String text) {
        final int progress = Math.round(((float) currentTaskNumber / TASK_COUNT) * 100.0f);

        if (progress <= progressBar.getMax()) {
            handler.post(() -> {
                progressBar.setIndeterminate(true);
                progressText.setText(String.format(
                        Locale.getDefault(), "%s (%d/%d)",
                        text, currentTaskNumber++, TASK_COUNT)
                );
            });
        } else {
            handler.post(() -> {
                progressText.setText("");
                progressBar.setIndeterminateDrawable(AppCompatResources
                        .getDrawable(this, R.drawable.progress_circle_finished));
            });
        }
    }

    private void resetProgress() {
        handler.post(() -> {
            progressBar.setIndeterminate(true);
            progressBar.setIndeterminateDrawable(progressCircle);
            progressText.setText("");
        });

        currentTaskNumber = 1;
        viewFlipper.setDisplayedChild(0);
    }

    private void checkIntentData(Intent intent) {
        if (null == intent.getData()) {
            Log.e(LOG_TAG, "Meter image URI data missing. Finishing activity...");
            setResult(RESULT_CANCELED);
            finish();
        }

        try {
            rawImageUri = intent.getData();
            rawImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), rawImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
