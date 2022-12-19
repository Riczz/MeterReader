package com.riczz.meterreader;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.riczz.meterreader.database.DBHandler;
import com.riczz.meterreader.enums.MeterType;
import com.riczz.meterreader.exception.BaseException;
import com.riczz.meterreader.exception.FrameDetectionException;
import com.riczz.meterreader.exception.NumberRecognizationException;
import com.riczz.meterreader.imageprocessing.ElectricMeterImageRecognizer;
import com.riczz.meterreader.imageprocessing.ImageHandler;
import com.riczz.meterreader.imageprocessing.MeterImageRecognizer;
import com.riczz.meterreader.imageprocessing.listeners.DropdownToggleListener;
import com.riczz.meterreader.view.SettingComponent;

import net.cachapa.expandablelayout.ExpandableLayout;

import org.opencv.core.Mat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public final class PreviewActivity extends AppCompatActivity {

    private static final int TASK_COUNT = 3;
    private static final String LOG_TAG = PreviewActivity.class.getName();

    private ActionBar actionBar;
    private Bitmap rawImage;
    private ImageHandler imageHandler;
    private Drawable progressCircle;
    private ProgressBar progressBar;
    private String errorText;
    private TextView progressText;
    private ViewFlipper viewFlipper;
    private Dialog resultDialog;
    private BaseException exception;

    private Mat dialsImage;
    private MeterImageRecognizer meterImageRecognizer;
    private ExecutorService executorService;
    private Handler handler;

    private DBHandler dbHandler;
    private MeterType meterType;
    private ViewFlipper configViewFlipper;
    private int currentTaskNumber = 1;
    private double dialsValue = 0.0f;
    private boolean isSettingsOnlyPage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        actionBar = Objects.requireNonNull(getSupportActionBar());
        actionBar.setTitle(getString(R.string.settings));
        actionBar.setDisplayHomeAsUpEnabled(true);

        isSettingsOnlyPage = (getIntent().hasExtra("SETTINGS_ONLY") &&
                getIntent().getBooleanExtra("SETTINGS_ONLY", false)
        );

        viewFlipper = findViewById(R.id.previewViewFlipper);
        dbHandler = new DBHandler(this);

        if (isSettingsOnlyPage) {
            viewFlipper.removeViewAt(0);
        } else {
            imageHandler = new ImageHandler(this);
            progressBar = findViewById(R.id.progressBar);
            progressText = findViewById(R.id.progressText);
            checkIntentData(getIntent());
        }

        meterType = (getIntent().getBooleanExtra("IS_GAS_METER", true)) ?
            MeterType.GAS : MeterType.ELECTRIC;

        meterImageRecognizer = meterType == MeterType.GAS ?
                new MeterImageRecognizer(this, dbHandler.getGasMeterConfig()) :
                new ElectricMeterImageRecognizer(this, dbHandler.getElectricMeterConfig());

        if (!isSettingsOnlyPage) {
            progressCircle = progressBar.getIndeterminateDrawable();
            executorService = Executors.newSingleThreadExecutor();
            handler = new Handler(Looper.getMainLooper());
            executeDialRecognition();
        } else {
            initSettingsPage();
        }
    }

    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return true;
    }

    private void executeDialRecognition() {
        resetProgress();
        executorService.execute(() -> {
            updateProgress(getString(R.string.progress_frame_detection));

            try {
                dialsImage = meterImageRecognizer.detectDialFrame(rawImage);
            } catch (Exception e) {
                Log.e(LOG_TAG, "There was an error during dial frame detection phase.");
                Log.e(LOG_TAG, e.getMessage());
                exception = e instanceof FrameDetectionException ?
                        (FrameDetectionException)e : new FrameDetectionException(-1);
                errorText = getString(R.string.frame_detection_error);
                runOnUiThread(() -> {
                    resetProgressCircle();
                    showFinishedDialog(false);
                });
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
                runOnUiThread(() -> {
                    resetProgressCircle();
                    showFinishedDialog(false);
                });
                return;
            }

            updateProgress(getString(R.string.progress_finalization));
            meterImageRecognizer.saveResultImages();

            updateProgress("");
            runOnUiThread(this::showFinishedDialog);
        });
    }

    private void initSettingsPage() {
        Button retryButton = findViewById(R.id.retryRecognitionButton);
        Button resetDefaultsButton = findViewById(R.id.resetDefaultsButton);
        ImageView previousConfigButton = findViewById(R.id.previousConfigArrow);
        ImageView nextConfigButton = findViewById(R.id.nextConfigArrow);
        configViewFlipper = findViewById(R.id.configViewFlipper);
        actionBar.show();

        configViewFlipper.removeAllViews();
        getLayoutInflater().inflate(R.layout.gas_meter_config_page, configViewFlipper, true);
        getLayoutInflater().inflate(R.layout.electric_meter_config_page, configViewFlipper, true);
        configViewFlipper.setDisplayedChild(meterType == MeterType.GAS ? 0 : 1);

        previousConfigButton.setColorFilter(configViewFlipper.getDisplayedChild() == 0 ?
                getColor(R.color.unavailable) : getColor(R.color.green_700));

        nextConfigButton.setColorFilter(configViewFlipper.getDisplayedChild() == configViewFlipper.getChildCount()-1 ?
                getColor(R.color.unavailable) : getColor(R.color.green_700));

        if (!previousConfigButton.hasOnClickListeners()) {
            previousConfigButton.setOnClickListener(arrow -> {
                if (configViewFlipper.getDisplayedChild() != 0) {
                    configViewFlipper.showPrevious();
                    nextConfigButton.setColorFilter(getColor(R.color.green_700));

                    if (configViewFlipper.getDisplayedChild() == 0)
                        previousConfigButton.setColorFilter(getColor(R.color.unavailable));
                }
            });
        }

        if (!nextConfigButton.hasOnClickListeners()) {
            nextConfigButton.setOnClickListener(arrow -> {
                if (configViewFlipper.getDisplayedChild() != configViewFlipper.getChildCount()-1) {
                    configViewFlipper.showNext();
                    previousConfigButton.setColorFilter(getColor(R.color.green_700));

                    if (configViewFlipper.getDisplayedChild() == configViewFlipper.getChildCount()-1)
                        nextConfigButton.setColorFilter(getColor(R.color.unavailable));
                }
            });
        }

        if (isSettingsOnlyPage) {
            ConstraintLayout settingsPage = (ConstraintLayout) viewFlipper.getChildAt(0);
            settingsPage.removeView(settingsPage.findViewById(R.id.retryRecognitionButton));

            // Change constraints
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(settingsPage);
            constraintSet.connect(R.id.configViewFlipper, ConstraintSet.BOTTOM, R.id.resetDefaultsButton,ConstraintSet.TOP);
            constraintSet.connect(R.id.previousConfigArrow,ConstraintSet.BOTTOM,R.id.resetDefaultsButton,ConstraintSet.TOP);
            constraintSet.connect(R.id.nextConfigArrow,ConstraintSet.BOTTOM,R.id.resetDefaultsButton,ConstraintSet.TOP);
            constraintSet.applyTo(settingsPage);
            settingsPage.invalidate();

            // Change reset defaults button color
            settingsPage.findViewById(R.id.resetDefaultsButton)
                    .setBackgroundColor(getColor(R.color.green_400));

        } else if (!retryButton.hasOnClickListeners()) {
            retryButton.setOnClickListener(button -> {
                meterImageRecognizer.setConfig(meterType == MeterType.GAS ?
                        dbHandler.getGasMeterConfig() :
                        dbHandler.getElectricMeterConfig()
                );
                executeDialRecognition();
            });
        }

        if (!resetDefaultsButton.hasOnClickListeners()) {
            LinearLayout resetConfigView = new LinearLayout(this);
            resetConfigView.setOrientation(LinearLayout.VERTICAL);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            );
            layoutParams.setMargins(
                    getResources().getDimensionPixelSize(R.dimen.reset_config_margin_start_dp), 0,
                    getResources().getDimensionPixelSize(R.dimen.reset_config_margin_end_dp), 0
            );
            resetConfigView.setLayoutParams(layoutParams);

            // Checkbox color values
            int [][] states = {{android.R.attr.state_checked}, {}};
            int greenColorId = getColor(R.color.green_400);
            int unavailableColorId = getColor(R.color.unavailable);
            ColorStateList enabledColorStateList = new ColorStateList(states, new int[] {greenColorId, unavailableColorId});
            ColorStateList disabledColorStateList = new ColorStateList(states, new int[] {unavailableColorId, unavailableColorId});

            // Checkboxes for each category
            final Map<MeterType, MaterialCheckBox> checkBoxes = new HashMap<>();

            View allCategoryRow = getLayoutInflater()
                    .inflate(R.layout.checkbox_list_item, resetConfigView, false);
            ((TextView)allCategoryRow.findViewById(R.id.checkboxListItemName))
                    .setText(getString(R.string.category_all));

            resetConfigView.addView(allCategoryRow);
            resetConfigView.invalidate();

            for (MeterType meterType : MeterType.values()) {
                View row  = getLayoutInflater()
                        .inflate(R.layout.checkbox_list_item, resetConfigView, false);
                ((TextView)row.findViewById(R.id.checkboxListItemName))
                        .setText(MeterType.getMeterCategoryName(this, meterType));

                MaterialCheckBox checkBox = row.findViewById(R.id.listItemCheckbox);
                checkBox.setButtonTintList(enabledColorStateList);
                checkBoxes.put(meterType, checkBox);
                resetConfigView.addView(row);
                resetConfigView.invalidate();
            }

            MaterialCheckBox allCategoryCheckBox = allCategoryRow.findViewById(R.id.listItemCheckbox);
            allCategoryCheckBox.setButtonTintList(enabledColorStateList);
            allCategoryCheckBox.setOnCheckedChangeListener((compoundButton, b) -> {
                for (MaterialCheckBox checkBox : checkBoxes.values()) {
                    checkBox.setChecked(b);
                    checkBox.setEnabled(!b);
                    checkBox.setButtonTintList(checkBox.isEnabled() ?
                            enabledColorStateList : disabledColorStateList
                    );
                }
            });

            resetDefaultsButton.setOnClickListener(button -> {
                if(resetConfigView.getParent() != null) {
                    ((ViewGroup)resetConfigView.getParent()).removeView(resetConfigView);
                }

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.reset_defaults_title))
                        .setMessage(getString(R.string.reset_defaults_description))
                        .setView(resetConfigView)
                        .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.dismiss())
                        .setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
                            if (allCategoryCheckBox.isChecked()) {
                                dbHandler.resetDefaults();
                                updateSettingValues(configViewFlipper);
                            } else {
                                Set<MeterType> selectedMeterTypes = checkBoxes
                                        .entrySet()
                                        .stream()
                                        .filter(entry -> entry.getValue().isChecked())
                                        .map(Map.Entry::getKey)
                                        .collect(Collectors.toSet());

                                dbHandler.resetDefaults(selectedMeterTypes);
                                if (!selectedMeterTypes.isEmpty()) updateSettingValues(configViewFlipper);
                            }
                        })
                        .create()
                        .show();
            });
        }
    }

    private void updateSettingValues(ViewGroup parent) {
        for (int i = 0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);

            if (child instanceof SettingComponent) {
                ((SettingComponent) child).refreshValue();
            } else if (child instanceof ViewGroup) {
                updateSettingValues((ViewGroup) child);
            }
        }
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
                        initSettingsPage();
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
                        initSettingsPage();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != resultDialog && resultDialog.isShowing()) {
            resultDialog.dismiss();
            resultDialog = null;
        }
    }

    private void resetProgress() {
        resetProgressCircle();
        actionBar.hide();
        currentTaskNumber = 1;
        viewFlipper.setDisplayedChild(0);
        imageHandler.clearDirectory();
    }

    private void resetProgressCircle() {
        handler.post(() -> {
            progressBar.setIndeterminate(true);
            progressBar.setIndeterminateDrawable(progressCircle);
            progressText.setText("");
        });
    }

    private void checkIntentData(Intent intent) {
        if (null == intent.getData()) {
            Log.e(LOG_TAG, "Meter image URI data missing. Finishing activity...");
            setResult(RESULT_CANCELED);
            finish();
        }

        try {
            Uri rawImageUri = intent.getData();
            rawImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), rawImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
