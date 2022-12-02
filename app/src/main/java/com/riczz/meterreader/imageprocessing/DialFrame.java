package com.riczz.meterreader.imageprocessing;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.RotatedRect;
import org.opencv.imgproc.Imgproc;

public final class DialFrame {
    private final MatOfPoint contour;

    private final double perimeter;

    private final double area;

    private final RotatedRect minAreaRect;

    public DialFrame(MatOfPoint contour) {
        this.contour = contour;
        area = Imgproc.contourArea(contour, false);

        MatOfPoint2f rectPoints = new MatOfPoint2f(contour.toArray());
        perimeter = Imgproc.arcLength(rectPoints, true);
        minAreaRect = Imgproc.minAreaRect(rectPoints);
    }

    public MatOfPoint getContour() {
        return contour;
    }

    public double getPerimeter() {
        return perimeter;
    }

    public double getArea() {
        return area;
    }

    public RotatedRect getMinAreaRect() {
        return minAreaRect;
    }
}
