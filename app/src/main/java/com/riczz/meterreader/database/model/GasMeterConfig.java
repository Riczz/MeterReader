package com.riczz.meterreader.database.model;

public final class GasMeterConfig extends Config {
    private double maxCircularity;

    public GasMeterConfig() {
        super();
    }

    public GasMeterConfig(
            boolean useColorCorrection, double minDialArea, double maxSkewnessDeg,
            double maxBlackIntensityRatio, double gammaMultiplier, double digitFrameExtensionMultiplier,
            double digitMaxWidthRatio, double digitMaxHeightRatio, double digitHeightWidthRatioMin,
            double digitHeightWidthRatioMax, double digitMinBorderDist, double digitBlackBorderThickness,
            double dialFrameWidthMultiplier, int digitFrameMaxExtensionCount, double maxCircularity
    ) {
        super(
                useColorCorrection, minDialArea, maxSkewnessDeg,
                maxBlackIntensityRatio, gammaMultiplier, digitFrameExtensionMultiplier,
                digitMaxWidthRatio, digitMaxHeightRatio, digitHeightWidthRatioMin,
                digitHeightWidthRatioMax, digitMinBorderDist, digitBlackBorderThickness,
                dialFrameWidthMultiplier, digitFrameMaxExtensionCount
        );
        this.maxCircularity = maxCircularity;
    }

    public double getMaxCircularity() {
        return maxCircularity;
    }

    public void setMaxCircularity(double maxCircularity) {
        this.maxCircularity = maxCircularity;
    }
}
