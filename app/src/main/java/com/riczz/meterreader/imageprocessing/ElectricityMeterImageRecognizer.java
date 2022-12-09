package com.riczz.meterreader.imageprocessing;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Pair;

import com.riczz.meterreader.config.Config;
import com.riczz.meterreader.enums.ImageType;
import com.riczz.meterreader.exception.FrameDetectionException;

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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ElectricityMeterImageRecognizer extends MeterImageRecognizer implements IMeterImageRecognizer {

    private static final String LOG_TAG = ElectricityMeterImageRecognizer.class.getName();

    public ElectricityMeterImageRecognizer(Context context) {
        super(context);
    }

    @Override
    public Mat detectDialFrame(Bitmap image) throws FrameDetectionException {
        return super.detectDialFrame(image);
    }

    @Override
    public Mat detectDialFrame(Mat image) throws FrameDetectionException {
        image = CvHelper.resize(image, 1280, 720);

        Mat median = new Mat();
        Imgproc.medianBlur(image, median, 5);

        Mat gray = new Mat();
        Imgproc.cvtColor(median, gray, Imgproc.COLOR_RGB2GRAY);

        Mat lines = new Mat();
        Mat linedImage = image.clone();
        Mat canny = CvHelper.canny(median, 0.0d, 255.0d, 3);
        Imgproc.HoughLines(canny, lines, 1.0f, Math.PI / 180.0f, 190);

        // Draw original lines, then filter by skewness angle
        List<double[]> filteredLines = new ArrayList<>();

        for (int i = 0; i < lines.rows(); i++) {
            double[] line = lines.get(i, 0);
            Point[] linePoints = CvHelper.getLinePoints(line);
            Imgproc.circle(linedImage, linePoints[0], 5, new Scalar(255, 0, 0), 10, Imgproc.LINE_AA);
            Imgproc.circle(linedImage, linePoints[1], 5, new Scalar(255, 0, 0), 10, Imgproc.LINE_AA);
            Imgproc.line(linedImage, linePoints[0], linePoints[1],
                    new Scalar(255.0d, 0d, 0d, 255.0d), 1, Imgproc.LINE_AA);

            double angle = Math.toDegrees(line[1]);

            if (90 - Config.ImgProc.maxSkewnessDeg <= angle && angle <= 90 + Config.ImgProc.maxSkewnessDeg) {
                filteredLines.add(line);
            }
        }

        // Filter outliers
        double[] lineAngles = filteredLines.stream().mapToDouble(line -> line[1]).toArray();
        double[] limits = CvHelper.calculateIQR(lineAngles, 0.2);

        filteredLines = filteredLines.stream()
                .filter(line -> line[1] >= limits[0] && line[1] <= limits[1])
                .collect(Collectors.toList());

        // Merge close lines together, add top and bottom lines
        lines = CvHelper.mergeLines(filteredLines, linedImage);

        lines.put(lines.rows()-2, 0, 0d, 0d);
        lines.put(lines.rows()-2, 1, 0, 0);
        lines.put(lines.rows()-2, 2, image.width() - 1, 0);
        lines.put(lines.rows()-1, 0, 0d, 0d);
        lines.put(lines.rows()-1, 1, 0, image.height() - 1);
        lines.put(lines.rows()-1, 2, image.width() - 1, image.height() - 1);

        // Draw lines, create dial search mask
        Mat dialSearchMask = new Mat(linedImage.size(), CvType.CV_8UC1, new Scalar(255));

        Imgproc.rectangle(dialSearchMask,
                new Point(0, 0),
                new Point(dialSearchMask.width() - 1, dialSearchMask.height() - 1),
                new Scalar(0), 3, Imgproc.LINE_AA
        );

        // Create image with the filtered lines
        Mat filteredLineImage = image.clone();
        List<LineWithCoords> linesWithCoords = new ArrayList<>();

        for (int i = 0; i < lines.rows(); i++) {
            Point startPoint = new Point((int)lines.get(i, 1)[0], (int)lines.get(i, 1)[1]);
            Point endPoint = new Point((int)lines.get(i, 2)[0], (int)lines.get(i, 2)[1]);
            Imgproc.circle(filteredLineImage, startPoint, 5, new Scalar(255, 0, 0), 10, Imgproc.LINE_AA);
            Imgproc.circle(filteredLineImage, endPoint, 5, new Scalar(255, 0, 0), 10, Imgproc.LINE_AA);
            Imgproc.line(filteredLineImage, startPoint, endPoint, new Scalar(255.0d, 0d, 0d, 255.0d), 1, Imgproc.LINE_AA);
            Imgproc.line(dialSearchMask, startPoint, endPoint, new Scalar(0), 2, Imgproc.LINE_4);
            linesWithCoords.add(new LineWithCoords(lines, i));
        }

        // Sort the lines by Y coordinates
        linesWithCoords = linesWithCoords.stream()
                .sorted(Comparator.comparingInt(line -> (int) Math.min(line.getStartPoint().y, line.getEndPoint().y)))
                .collect(Collectors.toList());

        // Search every segmented region for red dial rect
        Rect redDialRect = null;
        Mat regionMask = null;
        double skewness = 0.0d;

        for (int i = 1; i < linesWithCoords.size(); i++) {
            Point[] regionPoints = new Point[] {
                    linesWithCoords.get(i-1).getStartPoint(),
                    linesWithCoords.get(i-1).getEndPoint(),
                    linesWithCoords.get(i).getEndPoint(),
                    linesWithCoords.get(i).getStartPoint()
            };

            double regionHeight = CvHelper.mean(
                    Math.abs(regionPoints[0].y - regionPoints[2].y),
                    Math.abs(regionPoints[1].y - regionPoints[3].y)
            );

            skewness = 90.0d - Math.toDegrees(linesWithCoords.get(i).getTheta());

            // Mask out the current region of the image
            regionMask = Mat.zeros(image.size(), CvType.CV_8UC1);
            Imgproc.fillConvexPoly(regionMask,
                    new MatOfPoint(regionPoints[0], regionPoints[1], regionPoints[2], regionPoints[3]),
                    new Scalar(255.0d, 255.0d, 255.0d), Imgproc.LINE_8);

            Mat masked = Mat.zeros(image.size(), CvType.CV_8UC1);
            image.copyTo(masked, regionMask);

            // Convert to HSV - Look for red dial contour
            Mat hsv = new Mat();
            Imgproc.cvtColor(masked, hsv, Imgproc.COLOR_RGB2HSV);
            Core.inRange(hsv, new Scalar(150, 20, 100), new Scalar(180, 255, 255), hsv);
            Imgproc.rectangle(hsv, new Point(0, 0), new Point(hsv.width(), hsv.height()),
                    new Scalar(0, 0, 0), 2, Imgproc.LINE_AA);

            // Remove noise
            Mat morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(5, 5));
            Mat morphed = new Mat();
            Imgproc.morphologyEx(hsv, morphed, Imgproc.MORPH_OPEN, morphKernel);

            morphKernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, new Size(10, 10));
            Imgproc.dilate(morphed, morphed, morphKernel);

            // Get the detected contours with red color
            List<MatOfPoint> redContours = new ArrayList<>();
            Imgproc.findContours(morphed, redContours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            for (MatOfPoint contour : redContours) {
                Rect boundingRect = Imgproc.boundingRect(contour);

                if ((boundingRect.height / regionHeight) * 100.0d < 80.0d ||
                        CvHelper.rectangularity(contour) < 0.650d ||
                        Imgproc.contourArea(contour) < 300.0d
                ) continue;

                // Suitable contour found
                redDialRect = boundingRect;
                break;
            }

            if (null != redDialRect) break;
        }

        if (null == redDialRect) {
            Log.e(LOG_TAG, "Error! Could not detect red dial frame.");
            throw new FrameDetectionException("Could not detect red dial frame!", 201);
        }

        // Red dial mask
        Mat redDialMask = Mat.zeros(image.size(), CvType.CV_8UC1);
        Imgproc.rectangle(redDialMask,
                new Point(redDialRect.x, 0),
                new Point(redDialRect.x + redDialRect.width, image.height() - 1),
                new Scalar(255), -1, Imgproc.LINE_4);
        Core.bitwise_and(redDialMask, regionMask, redDialMask);

        // Get red dial rotated rectangle from mask
        canny = CvHelper.canny(redDialMask, 0.0, 255.0, 5);
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(canny, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            Log.e(LOG_TAG, "Error! No contours have been found.");
            throw new FrameDetectionException("Error while getting red dial minAreaRect", 202);
        }

        RotatedRect dialMinAreaRect = Imgproc.minAreaRect(new MatOfPoint2f(contours.get(0).toArray()));

        // Fix skewness amount if rotated rectangle has zero angle
        if (dialMinAreaRect.angle == 90.0d || dialMinAreaRect.angle == 0.0d) {
            Log.d(LOG_TAG, "Could not determine red dial angle. Using skewness value from region.");
            Log.d(LOG_TAG, String.format("Angle: %f ==> %f\n", dialMinAreaRect.angle, dialMinAreaRect.angle - skewness));
            dialMinAreaRect.set(new double[] {
                    dialMinAreaRect.center.x, dialMinAreaRect.center.y,
                    dialMinAreaRect.size.width, dialMinAreaRect.size.height,
                    dialMinAreaRect.angle - skewness}
            );
        }

        // Search whole dials rect
        RotatedRect wholeDialsRect = new RotatedRect();
        RotatedRect extendedDialRect = CvHelper.stretchRectangle(dialMinAreaRect, 13.0d);

        double darkIntensityRatio = findDials(image, dialMinAreaRect, extendedDialRect, wholeDialsRect);
        int extensionCount = 0;

        while (darkIntensityRatio > Config.ImgProc.maxBlackIntensityRatio &&
                extensionCount++ < 5) {
            Log.d(LOG_TAG,
                    String.format("Whole dials rectangle is likely too small. Dark intensity ratio: %.2f",
                    darkIntensityRatio));
            Log.d(LOG_TAG, String.format("Extending whole dial width by %df %%\n",
                    (int) (Config.ImgProc.digitFrameExtensionMultiplier - 1.0d) * 100));

            Mat previousPoints = new Mat();
            Imgproc.boxPoints(wholeDialsRect, previousPoints);
            extendedDialRect = CvHelper.stretchRectangle(extendedDialRect, Config.ImgProc.digitFrameExtensionMultiplier);
            darkIntensityRatio = findDials(image, dialMinAreaRect, extendedDialRect, wholeDialsRect, previousPoints);
        }

        Mat modified = image.clone();
        CvHelper.drawRectangle(modified, extendedDialRect, new Scalar(255, 0, 0), 1);
        CvHelper.drawRectangle(modified, dialMinAreaRect, new Scalar(255, 255, 0), 2);
        CvHelper.drawRectangle(modified, wholeDialsRect, new Scalar(0, 0, 255), 3);

        // Create warped image
        Point[] wholeDialPoints = CvHelper.orderRectPoints(wholeDialsRect);
        Point[] redDialPoints = CvHelper.orderRectPoints(dialMinAreaRect);

        Point[] pointsToWarp = new Point[]{
                wholeDialPoints[0],
                redDialPoints[1],
                wholeDialPoints[2],
                redDialPoints[3]
        };

        Mat warped = CvHelper.warpBirdsEye(image, pointsToWarp, image.width(), Config.ImgProc.DIAL_IMAGE_HEIGHT);
        Mat corrected = CvHelper.rotate(warped, -skewness);

        resultImages.put(Pair.create(image, "Resized"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(median, "Blurred"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(canny, "Canny"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(linedImage, "Lines_orig"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(filteredLineImage, "Lines_filtered"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(regionMask, "Region_mask"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(redDialMask, "Red_dial_mask"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(modified, "Modified"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(warped, "Warped"), ImageType.FRAME_DETECTION);
        resultImages.put(Pair.create(corrected, "Corrected"), ImageType.FRAME_DETECTION);
        return corrected;
    }
}
