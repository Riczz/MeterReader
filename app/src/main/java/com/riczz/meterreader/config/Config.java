package com.riczz.meterreader.config;

import org.opencv.core.Range;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public final class Config {
    public static final boolean DEBUG_MODE = true;

    private Config() {}

    public static final class ImgProc {
        public static Range
                digitHeightWidthRatio = new Range(new double[]{0.1d, 6.5d});

        public static double
//                dialFrameWidthMultiplier = 4.0d,
//                maxCircularity = 0.6d,
//                minDialArea = 1000.0d;
                dialFrameWidthMultiplier = 4.0d,
                maxCircularity = 0.6d,
                minDialArea = 1000.0d,
                maxBlackIntensityRatio = 0.95d,
                digitFrameExtensionMultiplier = 1.05d,
                digitMinBorderDist = 0.02d,
                digitMaxWidth = 0.15d,
                digitMaxHeight = 0.65d,
                digitBlackBorderThickness = 0.275d;

        public static int
//                blurSigmaX = 5,
//                defaultKernelSize = 5,
//                defaultThresholdType = Imgproc.THRESH_BINARY,
//                defaultThresholdValue = 50,
//                dialImageHeight = 400,
//                morphologyDefaultType = Imgproc.MORPH_CLOSE,
//                morphologyKernelType = Imgproc.MORPH_RECT,

                numberOfDigits = 8,
                fractionalDigits = 3,
                blurSigmaX = 5,
                defaultKernelSize = 5,
                defaultThresholdValue = 30,
                digitFrameMaxExtensionCount = 2,
                maxSkewnessDeg = 30,
                morphologyKernelType = Imgproc.MORPH_RECT;

        public static final int
                DEFAULT_THRESHOLD_TYPE = Imgproc.THRESH_BINARY,
                MORPHOLOGY_DEFAULT_TYPE = Imgproc.MORPH_CLOSE,
                DIAL_IMAGE_HEIGHT = 400;

//                HISTOGRAM_CLIP_PERCENT = 25,
//                HISTOGRAM_DARK_INTENSITY_THRESHOLD = 100,
//                HISTOGRAM_IMAGE_HEIGHT = 400,
//                HISTOGRAM_IMAGE_WIDTH = 512,
//                HISTOGRAM_SIZE = 255,

        public static final Scalar
                HSV_BLACK_INTENSITY_LOWER = new Scalar(0, 10, 0),
                HSV_BLACK_INTENSITY_UPPER = new Scalar(180, 255, 120),
                HSV_RED_INTENSITY_LOWER = new Scalar(150, 20, 100),
                HSV_RED_INTENSITY_UPPER = new Scalar(180, 255, 255);

        private ImgProc() {}
    }
}
