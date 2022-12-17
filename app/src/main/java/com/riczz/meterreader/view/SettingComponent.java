package com.riczz.meterreader.view;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatImageView;

import com.google.android.material.card.MaterialCardView;
import com.riczz.meterreader.database.DBHandler;
import com.riczz.meterreader.enums.MeterType;

public abstract class SettingComponent extends MaterialCardView {

    protected final Context context;
    protected DBHandler dbHandler;
    protected MeterType meterType;
    protected String dbColumnName;
    protected AttributeSet attributeSet;
    protected AppCompatImageView infoButton;
    protected ImageView infoImage;
    protected String infoDescription;
    protected TextView configPropertyName;
    protected TypedArray attributes;

    public SettingComponent(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attributeSet = attrs;
        this.dbHandler = new DBHandler(context);
    }

    protected void showInfoDialog() {
        new AlertDialog.Builder(context)
                .setTitle(configPropertyName.getText().toString())
                .setMessage(infoDescription)
                .setPositiveButton(android.R.string.ok, (dialog, i) -> dialog.dismiss())
                .setView(infoImage)
                .show();
    }

    public abstract void refreshValue();

    protected abstract void initializeComponents();

    protected abstract void initializeAttributes();

}
