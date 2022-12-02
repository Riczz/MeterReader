package com.riczz.meterreader.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;

import com.google.android.material.textfield.TextInputLayout;
import com.riczz.meterreader.R;

public final class SettingComboBoxComponent extends SettingComponent {

    private TextInputLayout comboBox;

    public SettingComboBoxComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.config_combobox, this, true);

        initializeComponents();
        initializeAttributes();

        infoButton.setOnClickListener(button -> showInfoDialog());

    }

    @Override
    protected void initializeComponents() {
        configPropertyName = findViewById(R.id.configPropertyName);
        comboBox = findViewById(R.id.configComboBox);
        infoButton = findViewById(R.id.configInfo);
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
                case R.styleable.SettingComponent_hint:
                    comboBox.setHint(attr);
                    break;
            }
        }
    }

    @Override
    public double getValue() {
        return 0;
    }
}
