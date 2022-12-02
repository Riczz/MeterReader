package com.riczz.meterreader.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.util.Pair;

import com.riczz.meterreader.config.Config;
import com.riczz.meterreader.enums.ImageType;
import com.riczz.meterreader.exception.FrameDetectionException;
import com.riczz.meterreader.exception.NumberRecognizationException;
import com.riczz.meterreader.exception.SkewnessCorrectionException;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MeterImageRecognizer implements IMeterImageRecognizer {

    public static final Size DIGIT_SHAPE = new Size(28, 28);
    private static final String LOG_TAG = MeterImageRecognizer.class.getName();

    private final Context context;
    private final ImageHandler imageHandler;
    private final Map<ImageType, Pair<Mat, Uri>> resultImages;

    public MeterImageRecognizer(Context context) {
        this.context = context;
        this.imageHandler = new ImageHandler(context);
        this.resultImages = new EnumMap<>(ImageType.class);
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
                    Core.FONT_HERSHEY_SIMPLEX,
                    0.5,
                    new Scalar(255, 0, 255), 2
            );
        }

        // Filtering contours by area, rectangularity and circularity values
        contours = contours.stream().filter(contour -> {
            MatOfPoint2f contourPoints = new MatOfPoint2f(contour.toArray());
            double area = Imgproc.contourArea(contour, false);
            double perimeter = Imgproc.arcLength(contourPoints, true);

            //TODO:
            Log.e("ASD", "CIRCUL: " + 1d / ((Math.pow(perimeter, 2d) / area)));
            Log.e("ASD", "AREA: " + area);

            if (area < Config.ImgProc.minDialArea) {
                return false;
            }

            MatOfPoint2f approxCurve = new MatOfPoint2f();
            Imgproc.approxPolyDP(contourPoints, approxCurve, 0.03 * perimeter, true);
            return CvHelper.circularity(contourPoints) <= Config.ImgProc.maxCircularity && approxCurve.rows() == 4;
        }).collect(Collectors.toList());

        Imgproc.drawContours(contoured, contours, -1, new Scalar(255, 0, 0), 2);

        if (contours.size() == 0) {
            throw new FrameDetectionException("No suitable contours have been found on the image!");
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

                Log.e("ASD", "RECTANG: " + rectangularity);
                Log.e("ASD", "AREA: " + area);
            }
        }

        assert !dialContour.empty();
        DialFrame redDialFrame = new DialFrame(dialContour);

        Mat dialFramePoints = new Mat();
        Imgproc.boxPoints(redDialFrame.getMinAreaRect(), dialFramePoints);

        // Get the search rectangle frame by extending the red frame horizontally
        RotatedRect extendedDialRect = CvHelper.stretchRectangle(
                redDialFrame.getMinAreaRect(),
                Config.ImgProc.dialFrameWidthMultiplier
        );

        // Check which side contains the rest of the dial numbers
        RotatedRect wholeDialsRect = new RotatedRect();

        double darkIntensityRatio = findDials(image, redDialFrame.getMinAreaRect(), extendedDialRect, wholeDialsRect);
        int extensionCount = 0;

        while (darkIntensityRatio > Config.ImgProc.maxBlackIntensityRatio &&
                extensionCount++ < Config.ImgProc.digitFrameMaxExtensionCount) {
            Log.d(LOG_TAG, String.format("Whole dials rectangle is likely too small. Dark intensity ratio: %.2f",
                    darkIntensityRatio));
            Log.d(LOG_TAG, String.format("Extending whole dial width by %df %%\n",
                    (int)(Config.ImgProc.digitFrameExtensionMultiplier - 1.0d) * 100));

            Mat previousPoints = new Mat();
            Imgproc.boxPoints(wholeDialsRect, previousPoints);
            extendedDialRect = CvHelper.stretchRectangle(extendedDialRect, Config.ImgProc.digitFrameExtensionMultiplier);

            darkIntensityRatio = findDials(image, redDialFrame.getMinAreaRect(), extendedDialRect,
                    wholeDialsRect, previousPoints);
        }

        Mat modified = image.clone();
        CvHelper.drawRectangle(modified, extendedDialRect, new Scalar(255d, 0d, 0d, 255d), 1);
        CvHelper.drawRectangle(modified, redDialFrame.getMinAreaRect(), new Scalar(255d, 255d, 0d, 255d), 2);
        CvHelper.drawRectangle(modified, wholeDialsRect, new Scalar(0d, 0d, 255d, 255d), 3);

        Point[] wholeDialPoints = CvHelper.orderRectPoints(wholeDialsRect);
        Point[] redDialPoints = CvHelper.orderRectPoints(redDialFrame.getMinAreaRect());

        Point[] pointsToWarp = new Point[] {
                wholeDialPoints[0],
                redDialPoints[1],
                wholeDialPoints[2],
                redDialPoints[3]
        };

        Mat warped = CvHelper.warpBirdsEye(image, pointsToWarp, image.width(), Config.ImgProc.DIAL_IMAGE_HEIGHT);

        resultImages.put(ImageType.IMAGE_RESIZED, Pair.create(image, null));
        resultImages.put(ImageType.IMAGE_MORPHED, Pair.create(morphedImage, null));
        resultImages.put(ImageType.IMAGE_BLURRED, Pair.create(blurredImage, null));
        resultImages.put(ImageType.IMAGE_BGMAX, Pair.create(bgMax, null));
        resultImages.put(ImageType.IMAGE_SUBTRACTED, Pair.create(subtracted, null));
        resultImages.put(ImageType.IMAGE_CONTOURED, Pair.create(contoured, null));
        resultImages.put(ImageType.IMAGE_CANNY, Pair.create(cannyImage, null));
        resultImages.put(ImageType.IMAGE_TRESHOLDED, Pair.create(thresholdImage, null));
        resultImages.put(ImageType.IMAGE_DILATED, Pair.create(dilatedImage, null));
        resultImages.put(ImageType.IMAGE_MODIFIED, Pair.create(modified, null));
        resultImages.put(ImageType.IMAGE_WARPED, Pair.create(warped, null));

        try {
            Mat corrected = correctSkew(warped);
            resultImages.put(ImageType.IMAGE_CORRECTED, Pair.create(corrected, null));
        } catch (SkewnessCorrectionException e) {
            e.printStackTrace();
        }

        return warped;
    }

    private Mat correctSkew(Mat image) throws SkewnessCorrectionException {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

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
            Log.d(LOG_TAG, "ANGLE: " + angle);
            if (angle >= 90.0f - Config.ImgProc.maxSkewnessDeg && angle <= 90.0f + Config.ImgProc.maxSkewnessDeg) {
                filteredLines.add(lines.get(i, 0));
            }
        }

        if (filteredLines.isEmpty()) {
            Log.d(LOG_TAG, "No lines found on warped image. Image correction skipped.");
            throw new SkewnessCorrectionException("Could not do skewness correction on image.");
        } else {
            Log.d(LOG_TAG, "Total lines found on warped image: " + filteredLines.size());
        }

        for (double[] line : filteredLines) {
            Point[] linePoints = CvHelper.getLinePoints(line);
            Imgproc.line(linedImage, linePoints[0], linePoints[1], new Scalar(255.0d, 0d, 0d, 255.0d), 1, Imgproc.LINE_AA);
        }

        double[] strongestLine = filteredLines.get(0);
        Point[] strongestPoints = CvHelper.getLinePoints(strongestLine);
        Imgproc.line(linedImage, strongestPoints[0], strongestPoints[1], new Scalar(255.0d, 255.0d, 0d, 255.0d), 3, Imgproc.LINE_AA);

        double skewness = 90.0d - Math.toDegrees(strongestLine[1]);
        Log.d(LOG_TAG, String.format("Skewness amount: %.16f\n", skewness));

        Mat skewnessCorrected = CvHelper.rotate(image, -skewness);
        resultImages.put(ImageType.SKEWNESS_THRESHOLDED, Pair.create(thresholded, null));
        resultImages.put(ImageType.SKEWNESS_MORPH_OPEN, Pair.create(morphOpen, null));
        resultImages.put(ImageType.SKEWNESS_MORPH_CLOSE, Pair.create(morphClose, null));
        resultImages.put(ImageType.SKEWNESS_CANNY, Pair.create(canny, null));
        resultImages.put(ImageType.SKEWNESS_LINES, Pair.create(linedImage, null));
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
    ) throws FrameDetectionException
    {
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
            throw new FrameDetectionException("Could not find whole dials frame!");
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

        resultImages.put(ImageType.DIAL_SEARCH_MASK, Pair.create(morphed, null));
        resultImages.put(ImageType.DIAL_SEARCH_CANNY, Pair.create(cannyImage, null));

        for (MatOfPoint contour : contours) {
            MatOfPoint2f contourPointsMat = new MatOfPoint2f(contour.toArray());
            RotatedRect contourRect = Imgproc.minAreaRect(contourPointsMat);
            Mat contourMask = searchMask.submat(contourRect.boundingRect());

            if (contourMask.channels() == 3) {
                Imgproc.cvtColor(contourMask, contourMask, Imgproc.COLOR_BGR2GRAY);
            }

            // Crop image part specified by the contour
            Mat cropped = image.submat(contourRect.boundingRect());
            Imgproc.cvtColor(cropped, cropped, Imgproc.COLOR_BGR2HSV);

            // Count the dark intensities on the cropped image
            // Return the one which has the most dark areas
            Core.inRange(cropped,
                    Config.ImgProc.HSV_BLACK_INTENSITY_LOWER,
                    Config.ImgProc.HSV_BLACK_INTENSITY_UPPER,
                    cropped
            );

            Core.bitwise_and(cropped, cropped, cropped, contourMask);
            int darkIntensityCount = Core.countNonZero(cropped);

            Log.e("ASD", "DARK: " + darkIntensityCount);

            if (darkIntensityCount > darkIntensityCountMax) {
                resultImages.put(ImageType.DIAL_SEARCH_CROPPED, Pair.create(cropped, null));
                darkIntensityCountMax = darkIntensityCount;
                darkIntensityRatio = (double) darkIntensityCount / Core.countNonZero(contourMask);
                dialFrameRect = contourRect;
            }

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
        CvHelper.gammaCorrection(channels.get(0), gammaCorrected, 3.5f);

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

        resultImages.put(ImageType.CORRECTION_MEDIAN, Pair.create(median, null));
        resultImages.put(ImageType.CORRECTION_B_C, Pair.create(corrected, null));
        resultImages.put(ImageType.CORRECTION_GAMMA, Pair.create(gammaCorrected, null));
        resultImages.put(ImageType.CORRECTION_RESULT, Pair.create(result, null));
        return result;
    }

    public double getDialReadings(Mat image) throws NumberRecognizationException {
        Size imageSize = image.size();
        Mat corrected = preprocessFrameImage(image);

        //tresh niblack

        // Morphological opening to get rid of noise
        List<Mat> morphImages = new ArrayList<>();
        morphImages.add(new Mat());

        Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(6, 12));
        Imgproc.morphologyEx(thresholded, morphImages.get(0), Imgproc.MORPH_OPEN, morphKernel, new Point(-1, -1));

        // Morphological closing to connect the dials vertically
        morphImages.add(new Mat());
        morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(1, 40));
        Imgproc.morphologyEx(morphImages.get(0), morphImages.get(1), Imgproc.MORPH_CLOSE, morphKernel, new Point(-1, -1));

        // Contoured image that will contain the filtered contours
        Mat contoured = new Mat(imageSize, CvType.CV_8UC1, new Scalar(0, 0, 0));

        // Leave only the possible digit contours on the image
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(morphImages.get(1), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);

        contours = contours.stream().filter(contour -> {
            Rect boundingRect = Imgproc.boundingRect(contour);

            return (boundingRect.x >= imageSize.width * Config.ImgProc.digitMinBorderDist &&
                    boundingRect.y >= imageSize.height * Config.ImgProc.digitMinBorderDist &&
                    boundingRect.x + boundingRect.width <= imageSize.width - (imageSize.width * Config.ImgProc.digitMinBorderDist) &&
                    boundingRect.y + boundingRect.height <= imageSize.height - (imageSize.height * Config.ImgProc.digitMinBorderDist) &&
                    boundingRect.width <= Config.ImgProc.digitMaxWidth * imageSize.width &&
                    boundingRect.height <= Config.ImgProc.digitMaxHeight * imageSize.height);
        }).collect(Collectors.toList());

        Imgproc.drawContours(contoured, contours, -1, new Scalar(255, 255, 255), -1, Imgproc.LINE_AA);

        // Erase the red dial frame from the contoured image to prevent unwanted contours
        List<Mat> channels = new ArrayList<>();
        Core.split(image, channels);

        Mat bgMax = new Mat();
        Core.max(channels.get(1), channels.get(2), bgMax);

        Mat subtracted = new Mat();
        Core.subtract(channels.get(0), bgMax, subtracted);

        Mat rectTreshold = new Mat();
        Imgproc.threshold(subtracted, rectTreshold, 0, 255.0d, Imgproc.THRESH_OTSU);
        contoured.setTo(new Scalar(0), rectTreshold);

        // Draw black border on the image to remove possibly unwanted contours
        int thickness = (int) (contoured.height() * Config.ImgProc.digitBlackBorderThickness);

        Imgproc.rectangle(contoured,
                new Point(0, 0), new Point(contoured.width(), contoured.height()),
                new Scalar(0), thickness, Imgproc.LINE_AA
        );

        // Connect the digit segments if they were fragmented
        morphImages.add(new Mat());
        morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(4, 4));
        Imgproc.morphologyEx(contoured, morphImages.get(2), Imgproc.MORPH_DILATE, morphKernel, new Point(-1, -1), 3);

        // Take the digit contours and order them from left to right
        contours.clear();
        Imgproc.findContours(morphImages.get(2), contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_TC89_L1);

        contours = contours.stream()
                .filter(contour -> Imgproc.contourArea(contour) >= Config.ImgProc.minDialArea)
                .sorted(Comparator.comparingDouble(Imgproc::contourArea))
                .collect(Collectors.toList());
        Collections.reverse(contours);

        contours = contours.stream()
                .limit(Config.ImgProc.numberOfDigits)
                .sorted(Comparator.comparingInt(contour -> Imgproc.boundingRect(contour).x))
                .collect(Collectors.toList());

        if (contours.size() < Config.ImgProc.numberOfDigits) {
            throw new NumberRecognizationException("Could detect all digits.");
        }

        // Get meter dial readings
        List<Mat> detectedDigits = new ArrayList<>();
        StringBuilder digitsValue = new StringBuilder();
        Mat resultMask = new Mat(contoured.size(), CvType.CV_8UC1);
        Mat result = morphImages.get(2).clone();

        for (int i = 0; i < contours.size(); i++) {
            Rect boundingRect = Imgproc.boundingRect(contours.get(i));
            Mat cropped = image.submat(boundingRect);

            // make prediction TODO:
            String prediction = "1";

            // Add label
            Mat croppedMask = new Mat(imageSize, CvType.CV_8UC1, new Scalar(0));
            Imgproc.rectangle(croppedMask,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(255), -1);
            result.setTo(cropped, croppedMask);

            Size textSize = Imgproc.getTextSize(prediction, Core.FONT_HERSHEY_SIMPLEX, 0.75d, 2, null);

            Imgproc.rectangle(result,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(0, 255, 0), 2);

            Imgproc.rectangle(result,
                    new Point(boundingRect.x, boundingRect.y - 20),
                    new Point(boundingRect.x + textSize.width, boundingRect.y),
                    new Scalar(0, 255, 0), -1);

            Imgproc.putText(result, prediction,
                    new Point(boundingRect.x, boundingRect.y - 5),
                    Core.FONT_HERSHEY_SIMPLEX, 0.6d, new Scalar(255, 0, 0), 2);

            Mat dialImage = new Mat();
            Imgproc.resize(cropped, dialImage, new Size(64, 64), 0, 0, Imgproc.INTER_AREA);

            detectedDigits.add(dialImage);
            digitsValue.append(prediction);

            // Make mask to clean up result image
            Imgproc.rectangle(resultMask,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(255), -1);

            Imgproc.rectangle(resultMask,
                    new Point(boundingRect.x, boundingRect.y),
                    new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                    new Scalar(255), 2);

            Imgproc.rectangle(result,
                    new Point(boundingRect.x, boundingRect.y - 20),
                    new Point(boundingRect.x + textSize.width, boundingRect.y),
                    new Scalar(255), -1);
        }

        // Clean up result image
        Core.bitwise_and(result, result, resultMask);

        double digitResult;

        if (digitsValue.length() == 0) {
            throw new NumberRecognizationException("Could not detect any digits on the image.");
        } else {
            digitResult = Float.parseFloat(digitsValue.toString()) / Math.pow(10, Config.ImgProc.fractionalDigits);
            Log.d(LOG_TAG, String.format("Detected digits:\n%s ==> %f", digitsValue, digitResult));
        }

        return digitResult;
    }

    public double getDialReadings(Bitmap bitmap) throws NumberRecognizationException {
        Mat imageMat = new Mat();
        Utils.bitmapToMat(bitmap, imageMat);
        return getDialReadings(imageMat);
    }

    public void saveResultImages() {
        for (Map.Entry<ImageType, Pair<Mat, Uri>> entry : resultImages.entrySet()) {
            saveImage(entry.getKey(), entry.getValue().first);
        }
    }

    private void saveImage(ImageType imageType, Mat image) {
        saveImage(imageType, image, imageType.name());
    }

    private void saveImage(ImageType imageType, Mat image, String fileName) {
        Uri resultURI = imageHandler.saveImage(image, fileName);
        resultImages.put(imageType, Pair.create(image, resultURI));
    }

    public Map<ImageType, Pair<Mat, Uri>> getResultImages() {
        return resultImages;
    }
}
