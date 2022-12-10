package com.riczz.meterreader.database.model;

public abstract class Config {
    protected int id;
    protected boolean useColorCorrection;
    protected double minDialArea;
    protected double maxSkewnessDeg;
    protected double maxBlackIntensityRatio;
    protected double gammaMultiplier;
    protected double digitFrameExtensionMultiplier;
    protected double digitMaxWidthRatio;
    protected double digitMaxHeightRatio;
    protected double digitHeightWidthRatioMin;
    protected double digitHeightWidthRatioMax;
    protected double digitMinBorderDist;
    protected double digitBlackBorderThickness;
    protected double dialFrameWidthMultiplier;
    protected int digitFrameMaxExtensionCount;

    protected Config() {
    }

    protected Config(
            boolean useColorCorrection,
            double minDialArea, double maxSkewnessDeg,
            double maxBlackIntensityRatio, double gammaMultiplier,
            double digitFrameExtensionMultiplier, double digitMaxWidthRatio,
            double digitMaxHeightRatio, double digitHeightWidthRatioMin,
            double digitHeightWidthRatioMax, double digitMinBorderDist,
            double digitBlackBorderThickness, double dialFrameWidthMultiplier,
            int digitFrameMaxExtensionCount
    ) {
        this.useColorCorrection = useColorCorrection;
        this.minDialArea = minDialArea;
        this.maxSkewnessDeg = maxSkewnessDeg;
        this.maxBlackIntensityRatio = maxBlackIntensityRatio;
        this.gammaMultiplier = gammaMultiplier;
        this.digitFrameExtensionMultiplier = digitFrameExtensionMultiplier;
        this.digitMaxWidthRatio = digitMaxWidthRatio;
        this.digitMaxHeightRatio = digitMaxHeightRatio;
        this.digitHeightWidthRatioMin = digitHeightWidthRatioMin;
        this.digitHeightWidthRatioMax = digitHeightWidthRatioMax;
        this.digitMinBorderDist = digitMinBorderDist;
        this.digitBlackBorderThickness = digitBlackBorderThickness;
        this.digitFrameMaxExtensionCount = digitFrameMaxExtensionCount;
        this.dialFrameWidthMultiplier = dialFrameWidthMultiplier;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public boolean isUseColorCorrection() {
        return useColorCorrection;
    }

    public void setUseColorCorrection(boolean useColorCorrection) {
        this.useColorCorrection = useColorCorrection;
    }

    public double getMinDialArea() {
        return minDialArea;
    }

    public void setMinDialArea(double minDialArea) {
        this.minDialArea = minDialArea;
    }

    public double getMaxSkewnessDeg() {
        return maxSkewnessDeg;
    }

    public void setMaxSkewnessDeg(double maxSkewnessDeg) {
        this.maxSkewnessDeg = maxSkewnessDeg;
    }

    public double getMaxBlackIntensityRatio() {
        return maxBlackIntensityRatio;
    }

    public void setMaxBlackIntensityRatio(double maxBlackIntensityRatio) {
        this.maxBlackIntensityRatio = maxBlackIntensityRatio;
    }

    public double getGammaMultiplier() {
        return gammaMultiplier;
    }

    public void setGammaMultiplier(double gammaMultiplier) {
        this.gammaMultiplier = gammaMultiplier;
    }

    public double getDigitFrameExtensionMultiplier() {
        return digitFrameExtensionMultiplier;
    }

    public void setDigitFrameExtensionMultiplier(double digitFrameExtensionMultiplier) {
        this.digitFrameExtensionMultiplier = digitFrameExtensionMultiplier;
    }

    public double getDigitMaxWidthRatio() {
        return digitMaxWidthRatio;
    }

    public void setDigitMaxWidthRatio(double digitMaxWidthRatio) {
        this.digitMaxWidthRatio = digitMaxWidthRatio;
    }

    public double getDigitMaxHeightRatio() {
        return digitMaxHeightRatio;
    }

    public void setDigitMaxHeightRatio(double digitMaxHeightRatio) {
        this.digitMaxHeightRatio = digitMaxHeightRatio;
    }

    public double getDigitHeightWidthRatioMin() {
        return digitHeightWidthRatioMin;
    }

    public void setDigitHeightWidthRatioMin(double digitHeightWidthRatioMin) {
        this.digitHeightWidthRatioMin = digitHeightWidthRatioMin;
    }

    public double getDigitHeightWidthRatioMax() {
        return digitHeightWidthRatioMax;
    }

    public void setDigitHeightWidthRatioMax(double digitHeightWidthRatioMax) {
        this.digitHeightWidthRatioMax = digitHeightWidthRatioMax;
    }

    public double getDigitMinBorderDist() {
        return digitMinBorderDist;
    }

    public void setDigitMinBorderDist(double digitMinBorderDist) {
        this.digitMinBorderDist = digitMinBorderDist;
    }

    public double getDigitBlackBorderThickness() {
        return digitBlackBorderThickness;
    }

    public void setDigitBlackBorderThickness(double digitBlackBorderThickness) {
        this.digitBlackBorderThickness = digitBlackBorderThickness;
    }

    public int getDigitFrameMaxExtensionCount() {
        return digitFrameMaxExtensionCount;
    }

    public void setDigitFrameMaxExtensionCount(int digitFrameMaxExtensionCount) {
        this.digitFrameMaxExtensionCount = digitFrameMaxExtensionCount;
    }

    public double getDialFrameWidthMultiplier() {
        return dialFrameWidthMultiplier;
    }

    public void setDialFrameWidthMultiplier(double dialFrameWidthMultiplier) {
        this.dialFrameWidthMultiplier = dialFrameWidthMultiplier;
    }
}
