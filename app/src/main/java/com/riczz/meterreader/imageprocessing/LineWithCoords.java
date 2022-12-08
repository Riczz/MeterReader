package com.riczz.meterreader.imageprocessing;

import org.opencv.core.Mat;
import org.opencv.core.Point;

public class LineWithCoords {
    private double rho, theta;
    private Point startPoint, endPoint;

    public LineWithCoords(Mat lines, int rowIdx) {
        this(lines.get(rowIdx, 0), lines.get(rowIdx, 1), lines.get(rowIdx, 2));
    }

    public LineWithCoords(double[] rhoTheta, double[] startPoint, double[] endPoint) {
        this.rho = rhoTheta[0];
        this.theta = rhoTheta[1];
        this.startPoint = new Point((int)startPoint[0], (int)startPoint[1]);
        this.endPoint = new Point((int)endPoint[0], (int)endPoint[1]);
    }

    public double getRho() {
        return rho;
    }

    public void setRho(double rho) {
        this.rho = rho;
    }

    public double getTheta() {
        return theta;
    }

    public void setTheta(double theta) {
        this.theta = theta;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }
}
