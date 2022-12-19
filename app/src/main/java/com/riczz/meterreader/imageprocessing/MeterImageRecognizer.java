package com.riczz.meterreader.imageprocessing;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.riczz.meterreader.database.model.Config;
import com.riczz.meterreader.database.model.GasMeterConfig;
import com.riczz.meterreader.enums.ImageType;
import com.riczz.meterreader.enums.MeterType;
import com.riczz.meterreader.exception.FrameDetectionException;
import com.riczz.meterreader.exception.NumberRecognizationException;
import com.riczz.meterreader.exception.SkewnessCorrectionException;
import com.riczz.meterreader.imageprocessing.utils.DialFrame;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ximgproc.Ximgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MeterImageRecognizer implements IMeterImageRecognizer {

    private static final String LOG_TAG = MeterImageRecognizer.class.getName();
    private static final Scalar HSV_BLACK_INTENSITY_LOWER = new Scalar(0, 0, 0);
    private static final Scalar HSV_BLACK_INTENSITY_UPPER = new Scalar(180, 255, 120);
    protected static final String TFLITE_MODEL_ASSET_PATH = "dial_model.tflite";
    protected static final int DIAL_IMAGE_HEIGHT = 400;

    protected Config config;
    protected Interpreter tflite;
    protected final ImageHandler imageHandler;
    protected final Map<Pair<Mat, String>, ImageType> resultImages;
    protected final MeterType meterType;

    public MeterImageRecognizer(Context context, Config config) {
        this(context, MeterType.GAS, config);
    }

    public MeterImageRecognizer(Context context, MeterType meterType, Config config) {
        this.imageHandler = new ImageHandler(context);
        this.resultImages = new HashMap<>();
        this.meterType = meterType;
        this.config = config;

        try {
            tflite = new Interpreter(loadModelFile((Activity) context));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Mat detectDialFrame(Bitmap image) throws FrameDetectionException {
        Mat imageMat = new Mat();
        Utils.bitmapToMat(image, imageMat);
        return detectDialFrame(imageMat);
    }

    public Mat detectDialFrame(Mat image) throws FrameDetectionException {
        image = CvHelper.resize(image, 1280, 720);

        Mat morphedImage = new Mat();
        CvHelper.morphologyEx(image, morphedImage);

        Mat blurredImage = new Mat();
        CvHelper.gaussianBlur(morphedImage, blurredImage);

        // Split image channels (R,G,B)
        List<Mat> channels = new ArrayList<>(3);
        Core.split(blurredImage, channels);

        // Subtract B-G maximum from R channel
        Mat bgMax = new Mat();
        Mat subtracted = new Mat();
        Core.max(channels.get(1), channels.get(2), bgMax);
        Core.subtract(channels.get(0), bgMax, subtracted);

        // Segment red dial frame
        // Apply dilatation in order to get wider contour area
        Mat thresholdImage = CvHelper.threshold(subtracted);
        Mat dilatationKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(2, 2));
        Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(30, 30));
        Mat dilatedImage = CvHelper.dilate(thresholdImage, dilatationKernel, 2);
        Imgproc.erode(dilatedImage, dilatedImage, dilatationKernel, new Point(-1, -1), 1);
        Imgproc.morphologyEx(dilatedImage, dilatedImage, Imgproc.MORPH_CLOSE, morphKernel);

        // Canny edge detection + finding contours
        Mat cannyImage = CvHelper.canny(dilatedImage, 0.0d, 255.0d, 3);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(cannyImage, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        Mat contouredOriginal = new Mat(cannyImage.size(), CvType.CV_8UC3, new Scalar(0, 0, 0));
        Mat contoured = new Mat(contouredOriginal.size(), contouredOriginal.type(), new Scalar(0, 0, 0));

        for (int i = 0; i < contours.size(); i++) {
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            Imgproc.drawContours(contouredOriginal, contours, i, new Scalar(255, 0, 0), 2);
            Imgproc.putText(
                    contouredOriginal,
                    String.valueOf(i + 1),
                    new Point(boundingRect.x, boundingRect.y),
                    Imgproc.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    new Scalar(255, 0, 255), 2
            );
        }

        // Filtering contours by area, rectangularity and circularity values
        contours = contours.stream().filter(contour -> {
            MatOfPoint2f contourPoints = new MatOfPoint2f(contour.toArray());
            double area = Imgproc.contourArea(contour, false);
            double perimeter = Imgproc.arcLength(contourPoints, true);

            if (area < config.getMinDialArea()) {
                return false;
            }

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourPoints, approxCurve, 0.03 * perimeter, true);
            return CvHelper.circularity(contourPoints) <= ((GasMeterConfig) config).getMaxCircularity() && approxCurve.rows() == 4;
        }).collect(Collectors.toList());

        Imgproc.drawContours(contoured, contours, -1, new Scalar(255, 0, 0), 2);

        if (contours.size() == 0) {
            throw new FrameDetectionException("No suitable contours have been found on the image!", 101);
        }

        // Get the contour of the red dial frame
        MatOfPoint dialContour = new MatOfPoint();
        double maxRectangularity = 0;

        for (MatOfPoint contour : contours) {
            MatOfPoint2f point2f = new MatOfPoint2f(contour.toArray());
            RotatedRect minAreaRect = Imgproc.minAreaRect(point2f);
            double area = Imgproc.contourArea(contour, false);
            double rectangularity = CvHelper.rectangularity(minAreaRect, area);

            if (rectangularity > maxRectangularity) {
                maxRectangularity = rectangularity;
                dialContour = contour;
            }
        }

        assert !dialContour.empty();
        DialFrame redDialFrame = new DialFrame(dialContour);

        Mat dialFramePoints = new Mat();
        Imgproc.boxPoints(redDialFrame.getMinAreaRect(), dialFramePoints);

        // Get the search rectangle frame by extending the red frame horizontally
        RotatedRect extendedDialRect = CvHelper.stretchRectangle(
                redDialFrame.getMinAreaRect(),
                config.getDialFrameWidthMultiplier()
        );

        // Check which side contains the rest of the dial numbers
        RotatedRect wholeDialsRect = new RotatedRect();

        double darkIntensityRatio = findDials(image, redDialFrame.getMinAreaRect(), extendedDialRect, wholeDialsRect);
        int extensionCount = 0;

        while (darkIntensityRatio > config.getMaxBlackIntensityRatio() &&
                extensionCount++ < config.getDigitFrameMaxExtensionCount()) {
            Log.d(LOG_TAG, String.format("Whole dials rectangle is likely too small. Dark intensity ratio: %.2f", darkIntensityRatio));
            Log.d(LOG_TAG, String.format("Extending whole dial width by %d %%\n",
                    (int) ((config.getDigitFrameExtensionMultiplier() - 1.0d) * 100))
            );

            Mat previousPoints = new Mat();
            Imgproc.boxPoints(wholeDialsRect, previousPoints);
            extendedDialRect = CvHelper.stretchRectangle(extendedDialRect, config.getDigitFrameExtensionMultiplier());

            darkIntensityRatio = findDials(image, redDialFrame.getMinAreaRect(), extendedDialRect, wholeDialsRect, previousPoints);
        }

        Mat modified = image.clone();
        CvHelper.drawRectangle(modified, extendedDialRect, new Scalar(255d, 0d, 0d, 255d), 1);
        CvHelper.drawRectangle(modified, redDialFrame.getMinAreaRect(), new Scalar(255d, 255d, 0d, 255d), 2);
        CvHelper.drawRectangle(modified, wholeDialsRect, new Scalar(0d, 0d, 255d, 255d), 3);

        Point[] wholeDialPoints = CvHelper.orderRectPoints(wholeDialsRect);
        Point[] redDialPoints = CvHelper.orderRectPoints(redDialFrame.getMinAreaRect());

        Point[] pointsToWarp = new Point[]{
                wholeDialPoints[0],
                redDialPoints[1],
                wholeDialPoints[2],
                redDialPoints[3]
        };

        Mat warped = CvHelper.warpBirdsEye(image, pointsToWarp, image.width(), DIAL_IMAGE_HEIGHT);

        resultImages.put(Pair.create(image, "001_Resized"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(morphedImage, "002_Morphed"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(blurredImage, "003_Blurred"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(bgMax, "004_BGMax"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(subtracted, "005_Subtracted"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(thresholdImage, "006_Thresholded"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(dilatedImage, "007_Dilated"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(cannyImage, "008_Canny"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(contouredOriginal, "009_Contoured_Original"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(contoured, "010_Contoured"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(modified, "011_Modified"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(warped, "012_Warped"), ImageType.FRAME_DETECTION);

        try {
            Mat corrected = correctSkew(warped);
            resultImages.put(Pair.create(corrected, "013_Corrected"), ImageType.FRAME_DETECTION);
            return corrected;
        } catch (SkewnessCorrectionException e) {
            e.printStackTrace();
        }

        return warped;
    }

    private Mat correctSkew(Mat image) throws SkewnessCorrectionException {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_RGB2GRAY);

        Mat thresholded = new Mat();
        Imgproc.threshold(gray, thresholded, 0.0d, 255.0d, Imgproc.THRESH_OTSU);

        Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(30, 33));
        Mat morphOpen = new Mat();
        Imgproc.morphologyEx(thresholded, morphOpen, Imgproc.MORPH_OPEN, morphKernel);

        morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(100, 50));
        Mat morphClose = new Mat();
        Imgproc.morphologyEx(morphOpen, morphClose, Imgproc.MORPH_CLOSE, morphKernel);

        Mat canny = CvHelper.canny(morphClose, 0.0d, 255.0d, 3);

        Mat lines = new Mat();
        Mat linedImage = image.clone();
        Imgproc.HoughLines(canny, lines, 1.0f, Math.PI / 180.0f, 140);

        List<double[]> filteredLines = new ArrayList<>();

        for (int i = 0; i < lines.rows(); i++) {
            double angle = Math.toDegrees(lines.get(i, 0)[1]);
            if (angle >= 90.0f - config.getMaxSkewnessDeg() && angle <= 90.0f + config.getMaxSkewnessDeg()) {
                filteredLines.add(lines.get(i, 0));
            }
        }

        if (filteredLines.isEmpty()) {
            Log.d(LOG_TAG, "No lines found on warped image. Image correction skipped.");
            throw new SkewnessCorrectionException("Could not do skewness correction on image.", 102);
        } else {
            Log.d(LOG_TAG, "Total lines found on warped image: " + filteredLines.size());
        }

        for (double[] line : filteredLines) {
            Point[] linePoints = CvHelper.getLinePoints(line);
            Imgproc.line(linedImage, linePoints[0], linePoints[1],
                    new Scalar(255.0d, 0d, 0d, 255.0d), 1, Imgproc.LINE_AA);
        }

        double[] strongestLine = filteredLines.get(0);
        Point[] strongestPoints = CvHelper.getLinePoints(strongestLine);

        Imgproc.line(linedImage, strongestPoints[0], strongestPoints[1],
                new Scalar(255.0d, 255.0d, 0d, 255.0d), 3, Imgproc.LINE_AA);

        double skewness = 90.0d - Math.toDegrees(strongestLine[1]);
        Log.d(LOG_TAG, String.format("Skewness amount: %.16f\n", skewness));

        Mat skewnessCorrected = CvHelper.rotate(image, -skewness);
        resultImages.put(Pair.create(thresholded, "001_Thresholded"), ImageType.SKEWNESS_CORRECTION);
        resultImages.put(Pair.create(morphOpen, "002_MorphOpen"), ImageType.SKEWNESS_CORRECTION);
        resultImages.put(Pair.create(morphClose, "003_MorphClose"), ImageType.SKEWNESS_CORRECTION);
        resultImages.put(Pair.create(canny, "004_Canny"), ImageType.SKEWNESS_CORRECTION);
        resultImages.put(Pair.create(linedImage, "005_Lines"), ImageType.SKEWNESS_CORRECTION);
        return skewnessCorrected;
    }

    public double findDials(
            Mat image, RotatedRect redDialRect, RotatedRect searchRect,
            RotatedRect dst
    ) throws FrameDetectionException {
        return findDials(image, redDialRect, searchRect, dst, null);
    }

    public double findDials(
            Mat image, RotatedRect redDialRect, RotatedRect searchRect,
            RotatedRect dst, Mat resultPoints
    ) throws FrameDetectionException {
        Mat searchMask = Mat.zeros(image.size(), CvType.CV_8UC1);
        Imgproc.fillConvexPoly(searchMask, CvHelper.getPointsMatFromRect(searchRect), new Scalar(255.0d), Imgproc.LINE_AA);
        Imgproc.fillConvexPoly(searchMask, CvHelper.getPointsMatFromRect(redDialRect), new Scalar(0d), Imgproc.LINE_AA);
        Imgproc.rectangle(searchMask, new Point(0, 0),
                new Point(searchMask.width() - 4, searchMask.height() - 4),
                new Scalar(0d, 0d, 0d), 5, Imgproc.LINE_AA
        );
        Mat morphed = new Mat();
        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        CvHelper.morphologyEx(searchMask, morphed, kernel, Imgproc.MORPH_OPEN);
        Mat cannyImage = CvHelper.canny(morphed, 0.0d, 255.0d, 3);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(cannyImage, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (null != resultPoints && !resultPoints.empty()) {
            resultPoints.convertTo(resultPoints, CvType.CV_32S);

            for (MatOfPoint contour : contours) {
                Mat rectPoints = new Mat();
                MatOfPoint2f point2f = new MatOfPoint2f(contour.toArray());
                Imgproc.boxPoints(Imgproc.minAreaRect(point2f), rectPoints);
                rectPoints.convertTo(rectPoints, CvType.CV_32S);

                if (CvHelper.hasCommonCorner(resultPoints, rectPoints)) {
                    contours.clear();
                    contours.add(contour);
                    break;
                }
            }
        }

        Log.d(LOG_TAG, "Searching whole dials frame...");

        if (null != resultPoints && !resultPoints.empty()) {
            if (contours.size() > 2) {
                Log.w(LOG_TAG, "Warning: More than 2 contours found on input mask! Processing may take longer.\n");
            } else if (contours.size() == 2) {
                Log.d(LOG_TAG, "Input mask size optional. Mask contains 2 segments.\n");
            } else if (contours.size() == 1) {
                Log.e(LOG_TAG, "Error: Input mask only has one segment.\n");
            } else {
                Log.e(LOG_TAG, "Error: Mask image does not contain any segments!\n");
            }
        }

        if (contours.isEmpty()) {
            throw new FrameDetectionException("Could not find whole dials frame!", 103);
        }

        RotatedRect dialFrameRect = new RotatedRect();
        int darkIntensityCountMax = 0;
        double darkIntensityRatio = 0.0d;

        if (contours.size() == 2) {
            contours = contours.stream()
                    .sorted(Comparator.comparingDouble(Imgproc::contourArea))
                    .collect(Collectors.toList());

            Collections.reverse(contours);
            double[] contourAreas = contours.stream().mapToDouble(Imgproc::contourArea).toArray();

            if (contourAreas[0] > (contourAreas[1] * 1.25d)) {
                Log.d(LOG_TAG, "Result rectangle found by checking search mask borders.\n");
                contours.remove(1);
            }
        }

        resultImages.put(Pair.create(morphed, "001_Morphed"), ImageType.DIAL_SEARCH);
        resultImages.put(Pair.create(cannyImage, "002_Canny"), ImageType.DIAL_SEARCH);
        int maskCount = 0;

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contourPointsMat = new MatOfPoint2f(contour.toArray());
            RotatedRect contourRect = Imgproc.minAreaRect(contourPointsMat);
            Mat contourMask = searchMask.submat(contourRect.boundingRect());

            if (contourMask.channels() == 3) {
                Imgproc.cvtColor(contourMask, contourMask, Imgproc.COLOR_RGB2GRAY);
            }

            // Crop image part specified by the contour
            Mat cropped = image.submat(contourRect.boundingRect());
            Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_RGB2HSV);

            // Count the dark intensities on the cropped image
            // Return the one which has the most dark areas
            Core.inRange(cropped,
                    HSV_BLACK_INTENSITY_LOWER,
                    HSV_BLACK_INTENSITY_UPPER,
                    cropped
            );

            Core.bitwise_and(cropped, cropped, cropped, contourMask);
            int darkIntensityCount = Core.countNonZero(cropped);

            if (darkIntensityCount > darkIntensityCountMax) {
                darkIntensityCountMax = darkIntensityCount;
                darkIntensityRatio = (double) darkIntensityCount / Core.countNonZero(contourMask);
                dialFrameRect = contourRect;
            }

            resultImages.put(Pair.create(cropped, "003_Cropped_" + maskCount++), ImageType.DIAL_SEARCH);
        }

        dst.set(new double[]{
                dialFrameRect.center.x, dialFrameRect.center.y,
                dialFrameRect.size.width, dialFrameRect.size.height, dialFrameRect.angle
        });
        return darkIntensityRatio;
    }

    private Mat preprocessFrameImage(Mat image) {
        Mat median = new Mat();
        Imgproc.medianBlur(image, median, 7);

        Mat corrected = CvHelper.automaticBrightnessContrast(median, 15);

        Mat lab = new Mat();
        Imgproc.cvtColor(corrected, lab, Imgproc.COLOR_RGB2Lab);

        List<Mat> channels = new ArrayList<>(3);
        Core.split(lab, channels);

        // Histogram equalization
        Imgproc.equalizeHist(channels.get(0), channels.get(0));

        // Gamma correction
        Mat gammaCorrected = new Mat();
        CvHelper.gammaCorrection(channels.get(0), gammaCorrected, (float) config.getGammaMultiplier());

        // Multiply a and b channels
        Mat lut = new Mat(1, 256, CvType.CV_8U);

        for (int i = 0; i < 256; i++) {
            int lutValue = (int) Math.min(255.0d, i * 1.1d);
            lut.put(0, i, lutValue);
        }

        Core.LUT(channels.get(1), lut, channels.get(1));
        Core.LUT(channels.get(2), lut, channels.get(2));

        Mat merged = new Mat();
        Core.merge(channels, merged);

        Mat result = new Mat();
        Imgproc.cvtColor(merged, result, Imgproc.COLOR_Lab2RGB);

        resultImages.put(Pair.create(median, "001_Median"), ImageType.COLOR_CORRECTION);
        resultImages.put(Pair.create(corrected, "002_Auto_BG"), ImageType.COLOR_CORRECTION);
        resultImages.put(Pair.create(gammaCorrected, "003_Gamma"), ImageType.COLOR_CORRECTION);
        resultImages.put(Pair.create(result, "004_Result"), ImageType.COLOR_CORRECTION);
        return result;
    }

    public double getDialReadings(Mat image) throws NumberRecognizationException {
        Size imageSize = image.size();
        Mat corrected = new Mat(imageSize, image.type());

        if (config.isUseColorCorrection()) {
            corrected = preprocessFrameImage(image);
            Imgproc.cvtColor(corrected, corrected, Imgproc.COLOR_RGB2GRAY);
        } else {
            Imgproc.cvtColor(image, corrected, Imgproc.COLOR_RGB2GRAY);
        }

        Mat niblack = new Mat();
        Ximgproc.niBlackThreshold(corrected, niblack, 255, Imgproc.THRESH_BINARY,
                129, 0.4, Ximgproc.BINARIZATION_NICK);

        // Morphological opening to get rid of noise
        List<Mat> morphImages = new ArrayList<>();
        morphImages.add(new Mat());

        Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(6, 12));
        Imgproc.morphologyEx(niblack.clone(), morphImages.get(0), Imgproc.MORPH_OPEN, morphKernel, new Point(-1, -1));
        resultImages.put(Pair.create(morphImages.get(0), "002_Morphed"), ImageType.DIGIT_DETECTION);

        // Get rid of top and bottom border on electricity meter image
        if (meterType == MeterType.ELECTRIC) {
            Mat morphImage = morphImages.get(0);
            Mat borderRemoved = morphImage.clone();

            int thickness = (int) (morphImage.height() * 0.35d);
            Imgproc.rectangle(borderRemoved,
                    new Point(-thickness, 0),
                    new Point(morphImage.width() + thickness, morphImage.height()),
                    new Scalar(0), thickness, Imgproc.LINE_AA
            );
            morphImages.add(borderRemoved);
            resultImages.put(Pair.create(borderRemoved, "003_Border removed"), ImageType.DIGIT_DETECTION);
        }

        // Morphological closing to connect the dials vertically
        morphImages.add(new Mat());
        morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(1, 40));
        Imgproc.morphologyEx(
                morphImages.get(Math.max(0, morphImages.size() - 2)),
                morphImages.get(morphImages.size() - 1),
                Imgproc.MORPH_CLOSE, morphKernel,
                new Point(-1, -1)
        );

        resultImages.put(
                Pair.create(morphImages.get(morphImages.size() - 1), "004_Contoured_orig"),
                ImageType.DIGIT_DETECTION
        );

        // Contoured image that will contain the filtered contours
        Mat contoured = new Mat(imageSize, CvType.CV_8UC1, new Scalar(0, 0, 0));

        // Leave only the possible digit contours on the image
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(
                morphImages.get(morphImages.size() - 1),
                contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1
        );

        contours = contours.stream().filter(contour -> {
            Rect boundingRect = Imgproc.boundingRect(contour);

            return (boundingRect.x >= imageSize.width * config.getDigitMinBorderDist() &&
                    boundingRect.y >= imageSize.height * config.getDigitMinBorderDist() &&
                    boundingRect.x + boundingRect.width <= imageSize.width - (imageSize.width * config.getDigitMinBorderDist()) &&
                    boundingRect.y + boundingRect.height <= imageSize.height - (imageSize.height * config.getDigitMinBorderDist()) &&
                    boundingRect.width <= config.getDigitMaxWidthRatio() * imageSize.width &&
                    boundingRect.height <= config.getDigitMaxHeightRatio() * imageSize.height);
        }).collect(Collectors.toList());

        Imgproc.drawContours(contoured, contours, -1, new Scalar(255, 255, 255), -1, Imgproc.LINE_AA);

        // Erase the red dial frame from the contoured image to prevent unwanted contours
        if (meterType == MeterType.GAS) {
            List<Mat> channels = new ArrayList<>();
            Core.split(image, channels);

            Mat bgMax = new Mat();
            Core.max(channels.get(1), channels.get(2), bgMax);

            Mat subtracted = new Mat();
            Core.subtract(channels.get(0), bgMax, subtracted);

            Mat rectTreshold = new Mat();
            Imgproc.threshold(subtracted, rectTreshold, 0, 255.0d, Imgproc.THRESH_OTSU);
            contoured.setTo(new Scalar(0), rectTreshold);
        }

        // Draw black border on the image to remove possibly unwanted contours
        int thickness = (int) (contoured.height() * config.getDigitBlackBorderThickness());

        Imgproc.rectangle(contoured,
                new Point(0, 0), new Point(contoured.width(), contoured.height()),
                new Scalar(0), thickness, Imgproc.LINE_AA
        );

        // Connect the digit segments if they were fragmented
        morphImages.add(new Mat());
        morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4));
        Imgproc.morphologyEx(
                contoured, morphImages.get(morphImages.size() - 1),
                Imgproc.MORPH_DILATE, morphKernel, new Point(-1, -1), 3
        );

        resultImages.put(
                Pair.create(morphImages.get(morphImages.size() - 1), "006_Connected"),
                ImageType.DIGIT_DETECTION
        );

        // Take the digit contours and order them from left to right
        contours.clear();
        Imgproc.findContours(morphImages.get(morphImages.size() - 1), contours, new Mat(),
                Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1
        );

        contours = contours.stream()
                .filter(contour -> Imgproc.contourArea(contour) >= config.getMinDialArea())
                .sorted(Comparator.comparingDouble(Imgproc::contourArea))
                .collect(Collectors.toList());
        Collections.reverse(contours);

        contours = contours.stream()
                .limit(meterType.getNumberOfDigits())
                .sorted(Comparator.comparingInt(contour -> Imgproc.boundingRect(contour).x))
                .collect(Collectors.toList());

        if (contours.size() < meterType.getNumberOfDigits()) {
            saveResultImages();
            throw new NumberRecognizationException("Could not detect all digits.", 104);
        }

        // Get meter dial readings
        StringBuilder digitsValue = new StringBuilder();
        Mat resultMask = new Mat(contoured.size(), CvType.CV_8UC1);
        Mat result = morphImages.get(morphImages.size() - 1).clone();
        Imgproc.cvtColor(result, result, Imgproc.COLOR_GRAY2RGB);

        for (int i = 0; i < contours.size(); i++) {
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            Mat cropped = image.submat(boundingRect);

            // Make prediction
            TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 40}, DataType.FLOAT32);
            TensorImage dialTensorImage = CvHelper.preprocess(cropped);
            tflite.run(dialTensorImage.getBuffer(), probabilityBuffer.getBuffer());

            int predictionValue = CvHelper.argmax(probabilityBuffer.getFloatArray());
            String prediction = String.valueOf(predictionValue);
            digitsValue.append(prediction);

            if (predictionValue > 9 || predictionValue < 0) {
                saveResultImages();
                throw new NumberRecognizationException("There was an error during digit image prediction.", 105);
            }

            // Add label
            Mat croppedMask = new Mat(imageSize, CvType.CV_8UC1, new Scalar(0, 0, 0));
            Imgproc.rectangle(croppedMask,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(255, 255, 255), -1);
            image.copyTo(result, croppedMask);

            Size textSize = Imgproc.getTextSize(prediction, Imgproc.FONT_HERSHEY_SIMPLEX, 0.75d, 2, null);

            Imgproc.rectangle(result,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(0, 255, 0), 2);

            Imgproc.rectangle(result,
                    new Point(boundingRect.x - 1, boundingRect.y - 20),
                    new Point(boundingRect.x + textSize.width, boundingRect.y),
                    new Scalar(0, 255, 0), -1);

            Imgproc.putText(result, prediction,
                    new Point(boundingRect.x + 2, boundingRect.y - 4),
                    Imgproc.FONT_HERSHEY_SIMPLEX, 0.6d, new Scalar(255, 0, 0), 2);

            Mat dialImage = new Mat();
            Imgproc.resize(cropped, dialImage, new Size(64, 64), 0, 0, Imgproc.INTER_AREA);

            // Make mask to clean up result image
            Imgproc.rectangle(resultMask,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(255), -1);

            Imgproc.rectangle(resultMask,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(255), 2);

            Imgproc.rectangle(resultMask,
                    new Point(boundingRect.x - 1, boundingRect.y - 20),
                    new Point(boundingRect.x + textSize.width, boundingRect.y),
                    new Scalar(255), -1);
        }

        // Clean up result image
        Core.bitwise_and(result, result, resultMask);

        // Save output images
        resultImages.put(Pair.create(niblack, "001_Niblack"), ImageType.DIGIT_DETECTION);
        resultImages.put(Pair.create(contoured, "005_Contoured"), ImageType.DIGIT_DETECTION);
        resultImages.put(Pair.create(result, "007_Result"), ImageType.DIGIT_DETECTION);

        // Return detected dial value or throw exception in case of failure
        if (digitsValue.length() == 0) {
            throw new NumberRecognizationException("Could not detect any digits on the image.", 106);
        } else {
            double digitResult =
                    Float.parseFloat(digitsValue.toString()) /
                            Math.pow(10, meterType.getFractionalDigits());

            Log.d(LOG_TAG, String.format("Detected digits:\n%s ==> %f", digitsValue, digitResult));
            return digitResult;
        }
    }

    public double getDialReadings(Bitmap bitmap) throws NumberRecognizationException {
        Mat imageMat = new Mat();
        Utils.bitmapToMat(bitmap, imageMat);
        return getDialReadings(imageMat);
    }

    public void saveResultImages() {
        resultImages.forEach((key, imageType) ->
                imageHandler.saveImage(key.first, imageType, key.second.toLowerCase(Locale.ROOT)));
    }

    protected MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        return FileUtil.loadMappedFile(activity, TFLITE_MODEL_ASSET_PATH);
    }

    public void setConfig(Config config) {
        this.config = config;
    }
}
