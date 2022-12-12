package com.riczz.meterreader.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewFlipper;

import androidx.annotation.Nullable;

import com.github.chrisbanes.photoview.PhotoView;
import com.riczz.meterreader.R;
import com.riczz.meterreader.enums.ImageType;
import com.riczz.meterreader.enums.MeterType;
import com.riczz.meterreader.imageprocessing.ImageHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ImageCategoryViewer extends RelativeLayout {

    private final Context context;
    private final List<ImageType> dropdownIndexes = new ArrayList<>();
    private final Map<ImageType, ViewFlipper> imageFlippers = new HashMap<>();
    public ViewFlipper currentViewFlipper;

    private MeterType meterType = MeterType.GAS;
    private AutoCompleteTextView categorySelectorComboBox;
    private AttributeSet attributeSet;

    public ImageCategoryViewer(Context context, MeterType meterType) {
        super(context);
        this.context = context;
        this.meterType = meterType;
        initView();
    }

    public ImageCategoryViewer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attributeSet = attrs;
        initView();
    }

    public ImageCategoryViewer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.attributeSet = attrs;
        initView();
    }

    public ImageCategoryViewer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        this.attributeSet = attrs;
        initView();
    }

    public void addImageCategory(ImageType imageType) {
        if (imageFlippers.containsKey(imageType)) return;

        final List<Uri> imageUris = new ImageHandler(context).getImageCategoryImages(imageType);
        if (imageUris.isEmpty()) return;

        ViewFlipper viewFlipper = new ViewFlipper(context, null);
        imageFlippers.put(imageType, viewFlipper);

        for (Uri imageUri : imageUris) {
            PhotoView image = new PhotoView(context, null);

            ViewFlipper.LayoutParams layoutParams = new ViewFlipper.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
            layoutParams.gravity = Gravity.CENTER;
            layoutParams.setMargins(0, 0, 0, 0);
            image.setLayoutParams(layoutParams);

            image.setImageURI(imageUri);
            image.setMaximumScale(2.5f);
            viewFlipper.addView(image);
        }

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                context.getResources().getDimensionPixelSize(R.dimen.image_viewer_height_dp)
        );
        layoutParams.setMargins(
                context.getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_start_dp),
                context.getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_top_dp),
                context.getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_end_dp), 0
        );
        layoutParams.addRule(RelativeLayout.BELOW, R.id.configComboBox);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
        viewFlipper.setLayoutParams(layoutParams);
        viewFlipper.setVisibility(INVISIBLE);
        viewFlipper.setDisplayedChild(0);
        addView(viewFlipper);
        invalidate();
        requestLayout();
    }

    public void refreshView() {
        List<ImageType> orderedTypes = imageFlippers.keySet()
                .stream()
                .sorted(Comparator.comparingInt(Enum::ordinal))
                .collect(Collectors.toList());

        dropdownIndexes.addAll(orderedTypes);

        List<String> categories = orderedTypes
                .stream()
                .map(imageType -> ImageType.getCategoryName(context, imageType))
                .collect(Collectors.toList());

        categorySelectorComboBox.setAdapter(null);
        categorySelectorComboBox.setAdapter(new ArrayAdapter<>(context,
                android.R.layout.simple_dropdown_item_1line,
                categories
        ));

        if (!categorySelectorComboBox.getAdapter().isEmpty()) {
            categorySelectorComboBox.setText(categorySelectorComboBox.getAdapter()
                    .getItem(0)
                    .toString(), false
            );
        }
    }

    public void setSelected(ImageType imageType, int imageIndex) {
        if (imageFlippers.containsKey(imageType)) {
            categorySelectorComboBox.setText(categorySelectorComboBox.getAdapter()
                    .getItem(dropdownIndexes.indexOf(imageType))
                    .toString(), false
            );
            currentViewFlipper = imageFlippers.get(imageType);
            if (null == currentViewFlipper) return;
            int childCount = currentViewFlipper.getChildCount();
            if (imageIndex < 0) imageIndex += childCount;
            if (imageIndex < 0) return;
            currentViewFlipper.setDisplayedChild(Math.min(childCount - 1, imageIndex));
            refreshViewFlippers();
        }
    }

    public MeterType getMeterType() {
        return meterType;
    }

    public void setMeterType(MeterType meterType) {
        this.meterType = meterType;
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.image_viewer, this);
        ImageView previousImageArrow = root.findViewById(R.id.previousImageArrow);
        ImageView nextImageArrow = root.findViewById(R.id.nextImageArrow);
        categorySelectorComboBox = root.findViewById(R.id.imageSelectorTextView);

        previousImageArrow.setOnClickListener(view -> {
            if (null != currentViewFlipper) {
                currentViewFlipper.showPrevious();
            }
        });

        nextImageArrow.setOnClickListener(view -> {
            if (null != currentViewFlipper) {
                currentViewFlipper.showNext();
            }
        });

        categorySelectorComboBox.setOnItemClickListener((adapterView, view, i, l) -> {
            ImageType selectedCategory = dropdownIndexes.get(i);
            currentViewFlipper = imageFlippers.get(selectedCategory);
            refreshViewFlippers();
        });

        initializeAttributes();
    }

    private void refreshViewFlippers() {
        for (ViewFlipper viewFlipper : imageFlippers.values()) {
            viewFlipper.setVisibility(viewFlipper == currentViewFlipper ? VISIBLE : INVISIBLE);
        }
    }

    private void initializeAttributes() {
        if (null == attributeSet) return;
        TypedArray attributes = context.obtainStyledAttributes(attributeSet, R.styleable.ImageCategorySelector);

        for (int i = 0; i < attributes.getIndexCount(); i++) {
            int attr = attributes.getIndex(i);

            if (attr == R.styleable.ImageCategorySelector_meterType) {
                setMeterType(MeterType.values()[attr]);
            } else {
                throw new IllegalStateException("Unexpected value: " + attr);
            }
        }
    }
}
