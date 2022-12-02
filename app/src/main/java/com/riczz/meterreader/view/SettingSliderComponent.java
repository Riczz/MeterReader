package com.riczz.meterreader.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.riczz.meterreader.R;

import java.util.Locale;

public final class SettingSliderComponent extends SettingComponent {

    private Slider configSlider;
    private MaterialButton sliderValueDescButton;
    private MaterialButton sliderValueAscButton;
    private TextView sliderValueTextView;

    public SettingSliderComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.config_slider, this, true);

        initializeComponents();
        initializeAttributes();

        setValue(configSlider.getValueFrom());

        infoButton.setOnClickListener(button -> showInfoDialog());

        sliderValueDescButton.setOnClickListener(button -> setValue(configSlider.getValue() - configSlider.getStepSize()));

        sliderValueAscButton.setOnClickListener(button -> setValue(configSlider.getValue() + configSlider.getStepSize()));

        configSlider.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {}

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                setValue(slider.getValue());
            }
        });
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
                case R.styleable.SettingComponent_infoDescription:
//                    int descriptionId = attributes.getResourceId(attr, 0);
                    this.infoDescription = attributes.getString(attr);
                    break;
                case R.styleable.SettingComponent_infoImage:
                    int imageId = attributes.getResourceId(attr, 0);
//                    this.infoImage =
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

    public void setValue(float value) {
        if (value < configSlider.getValueFrom()) {
            configSlider.setValue(configSlider.getValueFrom());
        } else configSlider.setValue(Math.min(value, configSlider.getValueTo()));

        sliderValueTextView.setText(String.format(
                Locale.getDefault(),
                "%.2f", configSlider.getValue())
        );
    }

    @Override
    public double getValue() {
        return configSlider.getValue();
    }
}
