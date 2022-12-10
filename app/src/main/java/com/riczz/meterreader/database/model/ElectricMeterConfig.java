package com.riczz.meterreader.database.model;

public final class ElectricMeterConfig extends Config {
    private double minRectangularity;
    private double maxLineDistance;

    public ElectricMeterConfig() {
        super();
    }

    public ElectricMeterConfig(
            boolean useColorCorrection, double minDialArea, double maxSkewnessDeg,
            double maxBlackIntensityRatio, double gammaMultiplier, double digitFrameExtensionMultiplier,
            double digitMaxWidthRatio, double digitMaxHeightRatio, double digitHeightWidthRatioMin,
            double digitHeightWidthRatioMax, double digitMinBorderDist, double digitBlackBorderThickness,
            double dialFrameWidthMultiplier, int digitFrameMaxExtensionCount, double minRectangularity,
            double maxLineDistance
    ) {
        super(
                useColorCorrection, minDialArea, maxSkewnessDeg,
                maxBlackIntensityRatio, gammaMultiplier, digitFrameExtensionMultiplier,
                digitMaxWidthRatio, digitMaxHeightRatio, digitHeightWidthRatioMin, digitHeightWidthRatioMax,
                digitMinBorderDist, digitBlackBorderThickness, dialFrameWidthMultiplier,
                digitFrameMaxExtensionCount
        );
        this.minRectangularity = minRectangularity;
        this.maxLineDistance = maxLineDistance;
    }

    public double getMinRectangularity() {
        return minRectangularity;
    }

    public void setMinRectangularity(double minRectangularity) {
        this.minRectangularity = minRectangularity;
    }

    public double getMaxLineDistance() {
        return maxLineDistance;
    }

    public void setMaxLineDistance(double maxLineDistance) {
        this.maxLineDistance = maxLineDistance;
    }
}
