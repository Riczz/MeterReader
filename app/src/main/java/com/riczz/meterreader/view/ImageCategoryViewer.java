package com.riczz.meterreader.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

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

public final class ImageCategoryViewer extends ConstraintLayout {

    private static final String LOG_TAG = ImageCategoryViewer.class.getName();

    private final Context context;
    private final List<ImageType> dropdownIndexes = new ArrayList<>();
    private final Map<ImageType, ViewFlipper> imageFlippers = new HashMap<>();
    public ViewFlipper currentViewFlipper;

    private MeterType meterType = MeterType.GAS;
    private AutoCompleteTextView categorySelectorComboBox;
    private FrameLayout imageViewerFrame;
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
        if (imageFlippers.containsKey(imageType)) {
            Log.w(LOG_TAG, String.format("ImageCategoryViewer %d already contains category '%s'.",
                    getId(), imageType.getName()));
            return;
        }

        final List<Uri> imageUris = new ImageHandler(context).getImageCategoryImages(imageType);

        if (imageUris.isEmpty()) {
            Log.w(LOG_TAG, String.format("No images found in category '%s'.", imageType.getName()));
            return;
        }

        ViewFlipper viewFlipper = new ViewFlipper(context, null);
        viewFlipper.setId(generateViewId());
        viewFlipper.setInAnimation(context, android.R.anim.fade_in);
        viewFlipper.setOutAnimation(context, android.R.anim.fade_out);
        imageFlippers.put(imageType, viewFlipper);

        for (Uri imageUri : imageUris) {
            PhotoView image = new PhotoView(context, null);
            ViewFlipper.LayoutParams layoutParams = new ViewFlipper.LayoutParams(
                    ViewFlipper.LayoutParams.MATCH_PARENT,
                    ViewFlipper.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
            );
            layoutParams.setMargins(
                    getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_start_dp),
                    getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_top_dp),
                    getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_end_dp),
                    getResources().getDimensionPixelSize(R.dimen.image_viewer_margin_bottom_dp)
            );
            image.setLayoutParams(layoutParams);
            image.setImageURI(imageUri);
            image.setMaximumScale(5.0f);
            viewFlipper.addView(image);
        }

        imageViewerFrame.addView(viewFlipper);
        imageViewerFrame.invalidate();

        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        viewFlipper.setLayoutParams(layoutParams);
        viewFlipper.setVisibility(INVISIBLE);
        viewFlipper.setDisplayedChild(0);
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
                .map(imageType -> ImageType.getCategoryName(context, meterType, imageType))
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

    public void setComboBoxHeightSp(int height) {
        categorySelectorComboBox.setTextSize(TypedValue.COMPLEX_UNIT_SP, height);
        categorySelectorComboBox.invalidate();
    }

    private void initView() {
        View root = inflate(getContext(), R.layout.image_viewer, this);
        ImageView previousImageArrow = root.findViewById(R.id.previousImageArrow);
        ImageView nextImageArrow = root.findViewById(R.id.nextImageArrow);
        categorySelectorComboBox = root.findViewById(R.id.imageCategoryTextView);
        imageViewerFrame = root.findViewById(R.id.imageViewerFrame);
        setId(View.generateViewId());

        previousImageArrow.setOnClickListener(view -> {
            if (null != currentViewFlipper) {
                currentViewFlipper.showPrevious();
                ((PhotoView) currentViewFlipper.getChildAt(currentViewFlipper.getDisplayedChild()))
                        .setScale(1.0f);
            }
        });

        nextImageArrow.setOnClickListener(view -> {
            if (null != currentViewFlipper) {
                currentViewFlipper.showNext();
                ((PhotoView) currentViewFlipper.getChildAt(currentViewFlipper.getDisplayedChild()))
                        .setScale(1.0f);
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
        TypedArray attributes = context
                .obtainStyledAttributes(attributeSet, R.styleable.ImageCategorySelector);

        for (int i = 0; i < attributes.getIndexCount(); i++) {
            int attr = attributes.getIndex(i);

            if (attr == R.styleable.meterAttributes_meterType) {
                setMeterType(MeterType.values()[attributes.getInt(attr, 0)]);
            } else if (attr == R.styleable.ImageCategorySelector_comboBoxHeight) {
                setComboBoxHeightSp(attributes.getInt(attr, 14));
            } else {
                throw new IllegalStateException("Unexpected value: " + attr);
            }
        }
    }

    public MeterType getMeterType() {
        return meterType;
    }

    public void setMeterType(MeterType meterType) {
        this.meterType = meterType;
    }
}
