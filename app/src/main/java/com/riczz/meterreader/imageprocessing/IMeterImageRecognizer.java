package com.riczz.meterreader.imageprocessing;

import android.graphics.Bitmap;

import com.riczz.meterreader.exception.FrameDetectionException;
import com.riczz.meterreader.exception.NumberRecognizationException;

import org.opencv.core.Mat;
import org.opencv.core.RotatedRect;

public interface IMeterImageRecognizer {

    double getDialReadings(Mat image) throws NumberRecognizationException;

    double getDialReadings(Bitmap bitmap) throws NumberRecognizationException;

    Mat detectDialFrame(Mat image) throws FrameDetectionException;

    Mat detectDialFrame(Bitmap image) throws FrameDetectionException;

    double findDials(Mat image, RotatedRect rect, RotatedRect search, RotatedRect dst) throws FrameDetectionException;

    double findDials(Mat image, RotatedRect rect, RotatedRect search, RotatedRect dst, Mat resultPoints) throws FrameDetectionException;

    void saveResultImages();
}
