package com.riczz.meterreader.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.riczz.meterreader.R;
import com.riczz.meterreader.enums.MeterType;

public final class SettingSliderComponent extends SettingComponent {

    private Slider configSlider;
    private MaterialButton sliderValueDescButton;
    private MaterialButton sliderValueAscButton;
    private TextView sliderValueTextView;
    private final int decimalLength;

    public SettingSliderComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.config_slider, this, true);

        initializeComponents();
        initializeAttributes();

        decimalLength = String.valueOf(configSlider.getStepSize()).split("[.]")[1].length();
        setValue(dbHandler.getDoubleValue(meterType, dbColumnName, 0.0d));

        infoButton.setOnClickListener(button -> showInfoDialog());
        sliderValueDescButton.setOnClickListener(button -> setValue(configSlider.getValue() - configSlider.getStepSize(), true));
        sliderValueAscButton.setOnClickListener(button -> setValue(configSlider.getValue() + configSlider.getStepSize(), true));

        configSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                setValue(slider.getValue(), true);
            }
        });
    }

    @Override
    public void refreshValue() {
        setValue(dbHandler.getDoubleValue(meterType, dbColumnName, 0.0d), true);
    }

    @Override
    protected void initializeComponents() {
        infoButton = findViewById(R.id.configInfo);
        configPropertyName = findViewById(R.id.configPropertyName);
        configSlider = findViewById(R.id.configSlider);
        sliderValueDescButton = findViewById(R.id.sliderButtonDesc);
        sliderValueAscButton = findViewById(R.id.sliderButtonAsc);
        sliderValueTextView = findViewById(R.id.sliderValueTextView);
    }

    @Override
    protected void initializeAttributes() {
        attributes = context.obtainStyledAttributes(attributeSet, R.styleable.SettingComponent);

        for (int i = 0; i < attributes.getIndexCount(); i++) {
            int attr = attributes.getIndex(i);

            switch (attr) {
                case R.styleable.SettingComponent_propertyName:
                    configPropertyName.setText(attributes.getString(attr));
                    break;
                case R.styleable.SettingComponent_meterType:
                    meterType = MeterType.values()[attributes.getInt(attr, 0)];
                    break;
                case R.styleable.SettingComponent_dbColumnName:
                    dbColumnName = attributes.getString(attr);
                    break;
                case R.styleable.SettingComponent_infoDescription:
//                    int descriptionId = attributes.getResourceId(attr, 0);
                    this.infoDescription = attributes.getString(attr);
                    break;
                case R.styleable.SettingComponent_infoImage:
                    int imageId = attributes.getResourceId(attr, 0);
                    break;
                case R.styleable.SettingComponent_valueMin:
                    configSlider.setValueFrom(attributes.getFloat(attr, 0.0f));
                    break;
                case R.styleable.SettingComponent_valueMax:
                    configSlider.setValueTo(attributes.getFloat(attr, 0.0f));
                    break;
                case R.styleable.SettingComponent_stepSize:
                    configSlider.setStepSize(attributes.getFloat(attr, 0.0f));
                    break;
            }
        }
    }

    public void setValue(double value, boolean updateDB) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("%." + decimalLength + "f", value));
        stringBuilder.setCharAt(stringBuilder.length() - decimalLength - 1, '.');
        value = Double.parseDouble(stringBuilder.toString());

        configSlider.setValue((float) Math.max(
                configSlider.getValueFrom(),
                Math.min(value, configSlider.getValueTo()))
        );

        sliderValueTextView.setText(String.format("%." + decimalLength + "f", configSlider.getValue()));

        if (updateDB) {
            dbHandler.updateConfig(meterType, dbColumnName, value);
        }
    }

    public void setValue(double value) {
        setValue(value, false);
    }

    public double getValue() {
        return configSlider.getValue();
    }
}
