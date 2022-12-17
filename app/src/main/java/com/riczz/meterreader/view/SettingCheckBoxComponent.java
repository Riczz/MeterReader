package com.riczz.meterreader.view;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.riczz.meterreader.R;
import com.riczz.meterreader.enums.MeterType;

public class SettingCheckBoxComponent extends SettingComponent {
    private MaterialCheckBox checkBox;

    public SettingCheckBoxComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.config_checkbox, this, true);

        initializeComponents();
        initializeAttributes();
        refreshValue();

        infoButton.setOnClickListener(button -> showInfoDialog());
        checkBox.setOnCheckedChangeListener((compoundButton, b) -> setValue(b));
    }

    @Override
    public void refreshValue() {
        setValue(dbHandler.getBooleanValue(meterType, dbColumnName, false));
    }

    @Override
    protected void initializeComponents() {
        configPropertyName = findViewById(R.id.configPropertyName);
        checkBox = findViewById(R.id.configCheckBox);
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
                case R.styleable.SettingComponent_checked:
                    checkBox.setChecked(attributes.getBoolean(attr, false));
                    break;
            }
        }
    }

    public void setValue(boolean value) {
        Log.e("ASD", "SET VALUE: " + value);
        checkBox.setChecked(value);
        dbHandler.updateConfig(meterType, dbColumnName, value);
    }
}
