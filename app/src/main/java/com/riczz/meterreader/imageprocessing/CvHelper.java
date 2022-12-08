package com.riczz.meterreader.imageprocessing;

import android.graphics.Bitmap;
import android.util.Log;

import androidx.annotation.NonNull;

import com.riczz.meterreader.config.Config;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.TransformToGrayscaleOp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CvHelper {
    public static void gammaCorrection(Mat src, Mat dst, float gamma) {
        Mat lut = new Mat(1, 256, CvType.CV_8U);
        for (int i = 0; i < 256; i++) lut.put(0, i, (int) (Math.pow(i / 255.0f, 1 / gamma) * 255));
        Core.LUT(src, lut, dst);
    }

    public static Mat automaticBrightnessContrast(@NonNull Mat image, int clipHistPercent) {
        Mat hist = new Mat();
        MatOfInt channels = new MatOfInt(0);
        MatOfInt histSize = new MatOfInt(256);
        MatOfFloat histRange = new MatOfFloat(0, 256);
        List<Mat> src = new ArrayList<>();
        src.add(image);

        Imgproc.calcHist(src, channels, new Mat(), hist, histSize, histRange, false);

        double[] accumulator = new double[256];
        accumulator[0] = hist.get(0, 0)[0];

        for (int i = 1; i < hist.rows(); i++) {
            accumulator[i] = accumulator[i-1] + hist.get(i, 0)[0];
        }

        double maximum = accumulator[accumulator.length-1];
        clipHistPercent *= (maximum / 100.0d);
        clipHistPercent /= 2.0d;

        int minimumGray = 0;
        while (accumulator[minimumGray] < clipHistPercent) ++minimumGray;

        int maximumGray = 255;
        while (accumulator[maximumGray] >= (maximum - clipHistPercent)) --maximumGray;

        double alpha = 255.0d / (maximumGray - minimumGray);
        double beta = -minimumGray * alpha;

        Mat converted = new Mat();
        Core.convertScaleAbs(image, converted, alpha, beta);
        return converted;
    }

    public static TensorImage preprocess(@NonNull Bitmap bitmap) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeOp(28, 28, ResizeOp.ResizeMethod.BILINEAR))
                .add(new TransformToGrayscaleOp())
                .add(new NormalizeOp(0, 255))
                .build();

        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);
        return tensorImage;
    }

    public static TensorImage preprocess(@NonNull Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap, false);
        return preprocess(bitmap);
    }

    public static int argmax(float[] predictionArray) {
        float max = 0.0f;
        int idx = -1;

        for (int i = 0; i < predictionArray.length; i++) {
            float normalizedValue = predictionArray[i] / 255.0f;

            if (normalizedValue > max) {
                max = normalizedValue;
                idx = i;
            }
        }

        return idx;
    }

    public static double rectangularity(MatOfPoint contour) {
        RotatedRect minAreaRect = Imgproc.minAreaRect(new MatOfPoint2f(contour.toArray()));
        double contourArea = Imgproc.contourArea(contour, false);
        return rectangularity(minAreaRect, contourArea);
    }

    public static double rectangularity(@NonNull RotatedRect rectangle, double contourArea) {
        return contourArea / rectangle.size.area();
    }

    @NonNull
    public static RotatedRect stretchRectangle(RotatedRect rotatedRect, double sizeMultiplier) {
        RotatedRect stretched = new RotatedRect(rotatedRect.center, rotatedRect.size, rotatedRect.angle);

        if (rotatedRect.size.width > rotatedRect.size.height) {
            stretched.size.width *= sizeMultiplier;
        } else {
            stretched.size.height *= sizeMultiplier;
        }

        return stretched;
    }

    public static Point[] orderRectPoints(RotatedRect rotatedRect) {
        // Get the points from the rectangle
        Point[] points = new Point[4];
        rotatedRect.points(points);

        // Sort the rectangle points to be correctly ordered for the transform
        Point[] sortedPoints = new Point[4];
        double rectCenterX = rotatedRect.center.x;
        double rectCenterY = rotatedRect.center.y;

        for (Point point : points) {
            double offsetX = point.x;
            double offsetY = point.y;

            if (offsetX < rectCenterX && offsetY < rectCenterY) {
                sortedPoints[0] = new Point(offsetX, offsetY);
            } else if (offsetX > rectCenterX && offsetY < rectCenterY) {
                sortedPoints[1] = new Point(offsetX, offsetY);
            } else if (offsetX < rectCenterX && offsetY > rectCenterY) {
                sortedPoints[2] = new Point(offsetX, offsetY);
            } else if (offsetX > rectCenterX && offsetY > rectCenterY) {
                sortedPoints[3] = new Point(offsetX, offsetY);
            }
        }

        return sortedPoints;
    }

    public static double circularity(MatOfPoint2f contour) {
        double contourArea = Imgproc.contourArea(contour, false);

        if (contourArea == 0.0d) {
            return 0.0d;
        } else {
            double perimeter = Imgproc.arcLength(contour, true);
            return 1.0 / (Math.pow(perimeter, 2.0) / contourArea);
        }
    }

    public static boolean hasCommonCorner(Mat rect1, Mat rect2) {
        for (int i = 0; i < rect1.rows(); i++) {
            for (int j = 0; j < rect2.rows(); j++) {
                Point p1 = new Point(rect1.get(i, 0));
                Point p2 = new Point(rect2.get(j, 0));
                if (euclideanDistance(p1, p2) <= 1.5) return true;
            }
        }
        return false;
    }

    public static Mat warpBirdsEye(Mat src, Point[] warpPoints, int width, int height) {
        // Build transformation array, apply perspective warp
        MatOfPoint2f srcPoints = new MatOfPoint2f(
                warpPoints[0],
                warpPoints[1],
                warpPoints[2],
                warpPoints[3]
        );

        MatOfPoint2f dstPoints = new MatOfPoint2f(
                new Point(0, 0),
                new Point(width, 0),
                new Point(0, height),
                new Point(width, height)
        );

        Mat dst = new Mat();
        Mat warpMat = Imgproc.getPerspectiveTransform(srcPoints, dstPoints);
        Imgproc.warpPerspective(src, dst, warpMat, new Size(width, height));
        return dst;
    }

    public static Mat warpBirdsEye(Mat src, @NonNull RotatedRect rectArea, int width, int height) {
        Point[] sortedPoints = CvHelper.orderRectPoints(rectArea);
        return warpBirdsEye(src, sortedPoints, width, height);
    }

    public static Point[] getLinePoints(double[] line) {
        return getLinePoints(line, null);
    }


    public static Point[] getLinePoints(double[] line, Mat cropImage) {
        double a = Math.cos(line[1]);
        double b = Math.sin(line[1]);
        double x0 = a * line[0];
        double y0 = b * line[0];

        Point[] points = new Point[] {
                new Point(Math.round(x0 + 9999.0 * (-b)), Math.round(y0 + 9999.0 * a)),
                new Point(Math.round(x0 - 9999.0 * (-b)), Math.round(y0 - 9999.0 * a))
        };

        if (null != cropImage) {
            Size lineMaskSize = new Size(cropImage.width() + 1, cropImage.height() + 1);
            Mat lineMask = new Mat(lineMaskSize, CvType.CV_8UC1);
            Imgproc.line(lineMask, points[0], points[1], new Scalar(255.0d), 1, Imgproc.LINE_8);
            Imgproc.rectangle(lineMask,
                    new Point(1.0d, 1.0d),
                    new Point(cropImage.width() - 1, cropImage.height() -1),
                    new Scalar(0.0d),
                    -1,
                    Imgproc.LINE_4
            );

            Mat nonzero = new Mat(lineMaskSize, CvType.CV_8UC1, new Scalar(0));
            Core.findNonZero(lineMask, nonzero);
            List<Point> newPoints = new MatOfPoint(nonzero).toList();

            if (newPoints.size() == 2) {
                points[0] = newPoints.get(0);
                points[1] = newPoints.get(1);
            }
        }

        Arrays.sort(points, Comparator.comparingDouble(p -> p.x));
        return points;
    }

    public static double[] calculateIQR(double[] values) {
        return calculateIQR(values, 0.0d);
    }

    public static double[] calculateIQR(double[] values, double fence) {
        DescriptiveStatistics statistics = new DescriptiveStatistics(values);
        double q25 = statistics.getPercentile(25);
        double q75 = statistics.getPercentile(75);
        double IQR = fence * (q75-q25);
        return new double[] {q25 - IQR, q75 + IQR};
    }

    public static double euclideanDistance(Point p1, Point p2) {
        double xDiff = p1.y - p2.y;
        double yDiff = p1.x - p2.x;
        return Math.sqrt(xDiff * xDiff + yDiff * yDiff);
    }

    public static double getMiddlepointDistance(double[] line1, double[] line2) {
        Point[][] linePoints = new Point[][] {
                getLinePoints(line1),
                getLinePoints(line2)
        };

        Point[] middlePoints = Arrays.stream(linePoints).map(endpoints ->
                new Point(
                        ((endpoints[0].x + endpoints[0].y) / 2),
                        ((endpoints[1].x + endpoints[1].y) / 2)
                )).toArray(Point[]::new);

        return euclideanDistance(middlePoints[0], middlePoints[1]);
    }

//    public static Mat mergeLines(Mat lines, Mat cropImage) {
    public static Mat mergeLines(List<double[]> lines, Mat cropImage) {
        Set<Integer> modifiedIndexes = new HashSet<>();
        List<double[]> resultLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            if (modifiedIndexes.contains(i)) continue;

            List<Double> rhoValues = new ArrayList<>(),
                    thetaValues = new ArrayList<>();

            double[] line1 = lines.get(i);
            rhoValues.add(line1[0]);
            thetaValues.add(line1[1]);

            // Get rho and theta values from nearby lines
            for (int j = 0; j < lines.size(); j++) {
                double[] line2 = lines.get(j);

                if (i != j && getMiddlepointDistance(line1, line2) <= 15.0d || intersects(line1, line2, cropImage)) {
                    rhoValues.add(line2[0]);
                    thetaValues.add(line2[1]);
                    modifiedIndexes.add(j);
                }
            }

            // Create a new line from the average of the values
            double averageRho = rhoValues.stream().mapToDouble(val -> val).average().orElse(0.0);
            double averageTheta = thetaValues.stream().mapToDouble(val -> val).average().orElse(0.0);
            resultLines.add(new double[]{averageRho, averageTheta});
        }

        // Create result matrix
        // Store the endpoints for each line alongside the rho and theta values for better handling
        // TODO:
        Mat result = new Mat(resultLines.size() + 2, 3, CvType.CV_32FC2);

        for (int i = 0; i < result.rows() - 2; i++) {
            double[] lineData = resultLines.get(i);
            Point[] endpoints = getLinePoints(lineData, cropImage);

            Log.e("ASD", "DATA " + i);
            Log.e("ASD", Arrays.toString(lineData));
            Log.e("ASD", endpoints[0].x + " " + endpoints[0].y);
            Log.e("ASD", endpoints[1].x + " " + endpoints[1].y);
            Log.e("ASD", "");

            result.put(i, 0, lineData);
            result.put(i, 1, endpoints[0].x, endpoints[0].y);
            result.put(i, 2, endpoints[1].x, endpoints[1].y);
        }

        return result;
    }

    public static MatOfPoint getPointsMatFromRect(@NonNull RotatedRect rectangle) {
        Point[] rectanglePoints = new Point[4];
        rectangle.points(rectanglePoints);
        return new MatOfPoint(rectanglePoints);
    }

    public static double mean(double... values) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        for (double value : values) statistics.addValue(value);
        return statistics.getMean();
    }

    public static void drawRectangle(Mat image, @NonNull RotatedRect rectangle, Scalar color, int thickness) {
        Point[] vertices = new Point[4];
        rectangle.points(vertices);

        for (int j = 0; j < 4; j++) {
            Imgproc.line(image, vertices[j], vertices[(j + 1) % 4], color, thickness, Imgproc.LINE_4);
        }
    }

    @NonNull
    public static Mat rotate(@NonNull Mat image, double angle) {
        Point center = new Point(image.width()/2.0d, image.height()/2.0d);
        Mat M = Imgproc.getRotationMatrix2D(center, angle, 1.0d);
        Mat rotated = image.clone();
        Imgproc.warpAffine(rotated, rotated, M, image.size());
        return rotated;
    }

    @NonNull
    public static Mat resize(@NonNull Mat image, double width, double height) {
        Mat resized = new Mat(image.size(), image.type());
        Imgproc.resize(image, resized,
                new Size(width, height), 0, 0,
                Imgproc.INTER_AREA
        );
        return resized;
    }

    public static boolean intersects(double[] line1, double[] line2, Mat cropImage) {
        Point[] line1Points = getLinePoints(line1);
        Point[] line2Points = getLinePoints(line2);
        Mat lined1 = Mat.zeros(cropImage.size(), CvType.CV_8UC1);
        Mat lined2 = lined1.clone();
        Imgproc.line(lined1, line1Points[0], line1Points[1], new Scalar(255), 1, Imgproc.LINE_8);
        Imgproc.line(lined2, line2Points[0], line2Points[1], new Scalar(255), 1, Imgproc.LINE_8);
        Mat result = new Mat();
        Core.bitwise_and(lined1, lined2, result);
        double intersections = Core.countNonZero(result);
        return intersections > 0;
    }

    @NonNull
    public static Mat getRectSubmat(Mat image, RotatedRect rect) {
        Mat points = new Mat();
        Imgproc.boxPoints(rect, points);
        points.convertTo(points, CvType.CV_32S);

        return image.submat(
                (int) Math.max(Math.min(points.get(1,0)[1], points.get(2, 0)[1]), 0),
                (int) Math.min(Math.max(points.get(0,0)[1], points.get(3, 0)[1]), image.height() - 1),
                (int) Math.max(Math.min(points.get(0,0)[0], points.get(1, 0)[0]), 0),
                (int) Math.min(Math.max(points.get(2,0)[0], points.get(3,0)[0]), image.width() - 1)
        );
    }

    @NonNull
    public static Mat canny(Mat image, double threshold1, double threshold2, int apertureSize) {
        Mat cannyImage = new Mat();
        Imgproc.Canny(image, cannyImage, threshold1, threshold2, apertureSize);
        return cannyImage;
    }

    @NonNull
    public static Mat threshold(Mat image) {
        return threshold(image, Config.ImgProc.defaultThresholdValue, 255, Config.ImgProc.DEFAULT_THRESHOLD_TYPE);
    }

    @NonNull
    public static Mat threshold(Mat image, int threshold, double maxVal, int type) {
        Mat thresholdImage = new Mat();
        Imgproc.threshold(image, thresholdImage, threshold, maxVal, type);
        return thresholdImage;
    }

    @NonNull
    public static Mat dilate(Mat image, Mat kernel) {
        return dilate(image, kernel, 1);
    }

    @NonNull
    public static Mat dilate(Mat image, Mat kernel, int iterations) {
        Mat dilated = new Mat(image.size(), image.type());
        Imgproc.dilate(image, dilated, kernel, new Point(-1, -1), iterations);
        return dilated;
    }

    @NonNull
    public static Mat erode(Mat image, Mat kernel) {
        return erode(image, kernel, 1);
    }

    @NonNull
    public static Mat erode(Mat image, Mat kernel, int iterations) {
        Mat eroded = new Mat(image.size(), image.type());
        Imgproc.erode(image, eroded, kernel, new Point(-1, 1), iterations);
        return eroded;
    }

    public static void morphologyEx(Mat image, Mat dst) {
        morphologyEx(image, dst,
                Config.ImgProc.morphologyKernelType,
                new Size(Config.ImgProc.defaultKernelSize, Config.ImgProc.defaultKernelSize),
                Config.ImgProc.MORPHOLOGY_DEFAULT_TYPE);
    }

    public static void morphologyEx(Mat image, Mat dst, Mat kernel, int morphType) {
        Imgproc.morphologyEx(image, dst, morphType, kernel);
    }

    public static void morphologyEx(Mat image, Mat dst, int kernelType, Size kernelSize, int morphType) {
        Mat structuringElement = Imgproc.getStructuringElement(kernelType, kernelSize);
        Imgproc.morphologyEx(image, dst, morphType, structuringElement);
    }

    public static void gaussianBlur(Mat image, Mat dst) {
        gaussianBlur(image, dst,
                new Size(Config.ImgProc.defaultKernelSize, Config.ImgProc.defaultKernelSize),
                Config.ImgProc.blurSigmaX);
    }

    private static void gaussianBlur(Mat image, Mat dst, Size kernelSize, int sigmaX) {
        Imgproc.GaussianBlur(image, dst, kernelSize, sigmaX);
    }
}
