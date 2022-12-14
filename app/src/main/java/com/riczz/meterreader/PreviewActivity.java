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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;

import com.riczz.meterreader.database.DBHandler;
import com.riczz.meterreader.enums.MeterType;
import com.riczz.meterreader.exception.BaseException;
import com.riczz.meterreader.exception.FrameDetectionException;
import com.riczz.meterreader.exception.NumberRecognizationException;
import com.riczz.meterreader.imageprocessing.listeners.DropdownToggleListener;
import com.riczz.meterreader.imageprocessing.ElectricMeterImageRecognizer;
import com.riczz.meterreader.imageprocessing.MeterImageRecognizer;

import net.cachapa.expandablelayout.ExpandableLayout;

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
    private DBHandler dbHandler;
    private Drawable progressCircle;
    private ProgressBar progressBar;
    private String errorText;
    private TextView progressText;
    private Uri rawImageUri;
    private ViewFlipper viewFlipper;
    private Dialog resultDialog;
    private BaseException exception;

    private Mat dialsImage;
    private MeterImageRecognizer meterImageRecognizer;
    private ExecutorService executorService;
    private Handler handler;

    private MeterType meterType;
    private int currentTaskNumber = 1;
    private double dialsValue = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        checkIntentData(getIntent());

        progressBar = findViewById(R.id.progressBar);
        progressText = findViewById(R.id.progressText);
        retryButton = findViewById(R.id.retryRecognitionButton);
        viewFlipper = findViewById(R.id.previewViewFlipper);
        viewFlipper.setDisplayedChild(0);

        dbHandler = new DBHandler(this);

        meterType = (getIntent().getBooleanExtra("IS_GAS_METER", true)) ?
            MeterType.GAS : MeterType.ELECTRIC;

        meterImageRecognizer = (meterType == MeterType.GAS) ?
                new MeterImageRecognizer(this, dbHandler.getGasMeterConfig()) :
                new ElectricMeterImageRecognizer(this, dbHandler.getElectricMeterConfig());

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
            } catch (Exception e) {
                Log.e(LOG_TAG, "There was an error during dial frame detection phase.");
                Log.e(LOG_TAG, e.getMessage());
                exception = e instanceof FrameDetectionException ?
                        (FrameDetectionException)e : new FrameDetectionException(-1);
                errorText = getString(R.string.frame_detection_error);
                runOnUiThread(() -> showFinishedDialog(false));
                return;
            }

            updateProgress(getString(R.string.progress_dial_value_detection));

            try {
                dialsValue = meterImageRecognizer.getDialReadings(dialsImage);
            } catch (Exception e) {
                Log.e(LOG_TAG, "There was an error during dial numbers detection phase.");
                Log.e(LOG_TAG, e.getMessage());
                exception = e instanceof  NumberRecognizationException ?
                        (NumberRecognizationException)e : new NumberRecognizationException(-1);
                errorText = getString(R.string.dial_value_detection_error);
                runOnUiThread(() -> showFinishedDialog(false));
                return;
            }

            updateProgress(getString(R.string.progress_finalization));
            meterImageRecognizer.saveResultImages();

            updateProgress("");
            runOnUiThread(this::showFinishedDialog);
        });
    }

    private void showFinishedDialog() {
        showFinishedDialog(true);
    }

    private void showFinishedDialog(boolean success) {
        Intent resultIntent = new Intent();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        if (success) {
            dialogBuilder
                    .setTitle(getString(R.string.results))
                    .setMessage(String.format(meterType == MeterType.GAS ?
                            getString(R.string.results_description_gas) :
                            getString(R.string.results_description_elec),
                            dialsValue
                    ))
                    .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                        dialog.dismiss();
                        resultIntent.putExtra("DIALS_VALUE", dialsValue);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    })
                    .setNegativeButton(getString(R.string.options), (dialog, i) -> {
                        dialog.dismiss();
                        resultIntent.putExtra("DIALS_VALUE", dialsValue);
                        setResult(RESULT_OK, resultIntent);
                        viewFlipper.setDisplayedChild(1);
                    }).setOnCancelListener(dialogInterface -> {
                        setResult(RESULT_CANCELED, resultIntent);
                        finish();
                    });
        } else {
            View dialogView = getLayoutInflater().inflate(R.layout.error_description, null);

            dialogBuilder
                    .setTitle(getString(R.string.results_failed_code, exception.getErrorCode()))
                    .setMessage(errorText)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.ok, (dialog, i) -> {
                        dialog.dismiss();
                        setResult(RESULT_CANCELED, resultIntent);
                        finish();
                    })
                    .setNegativeButton(getString(R.string.options), (dialog, i) -> {
                        dialog.dismiss();
                        setResult(RESULT_CANCELED, resultIntent);
                        viewFlipper.setDisplayedChild(1);
                    }).setOnCancelListener(dialogInterface -> {
                        setResult(RESULT_CANCELED, resultIntent);
                        finish();
                    });

            ImageView errorDropdownArrow = dialogView.findViewById(R.id.errorDetailsArrow);
            ExpandableLayout errorDescription = dialogView.findViewById(R.id.expandable_layout);
            DropdownToggleListener dropdownToggleListener = new DropdownToggleListener(errorDescription, errorDropdownArrow);

            Button errorDetailsDropdownButton = dialogView.findViewById(R.id.errorDetailsButton);
            errorDetailsDropdownButton.setOnClickListener(dropdownToggleListener);

            TextView errorDetailsHeader = dialogView.findViewById(R.id.errorDetailsHeader);
            errorDetailsHeader.setText(getString(R.string.error_dropdown_header));
            ((TextView)dialogView.findViewById(R.id.errorTextView))
                    .setText(BaseException.getDetails(this, exception.getErrorCode()));
        }

        resultDialog = dialogBuilder.create();
        resultDialog.setCanceledOnTouchOutside(false);
        resultDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != resultDialog && resultDialog.isShowing()) {
            resultDialog.dismiss();
            resultDialog = null;
        }
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
