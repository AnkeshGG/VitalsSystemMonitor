package com.example.vitals.controllers;

import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import java.util.Objects;

public class DashboardController {
    // Load HistoricalView Component
    @FXML private Button viewHistoryButton;

    // CPU Components
    @FXML private LineChart<String, Number> cpuChart;
    @FXML private Label cpuPercentageLabel;

    // Memory Components
    @FXML private Circle memoryBackgroundCircle;
    @FXML private Circle memoryProgressCircle;
    @FXML private Label memoryUsedLabel;
    @FXML private Label memoryPercentageLabel;
    @FXML private Label memoryUsedStatLabel;
    @FXML private Label memoryAvailableLabel;
    @FXML private Label memoryTotalLabel;

    // Disk Components - Main System Disk
    @FXML private ProgressBar totalDiskProgressBar;
    @FXML private Label diskUsedLabel;
    @FXML private Label diskAvailableLabel;
    @FXML private Label diskTotalLabel;

    // Network Components
    @FXML private LineChart<String, Number> networkChart;
    @FXML private Label uploadSpeedLabel;
    @FXML private Label downloadSpeedLabel;

    // Chart data series
    private XYChart.Series<String, Number> cpuSeries;
    private XYChart.Series<String, Number> networkUploadSeries;
    private XYChart.Series<String, Number> networkDownloadSeries;

    // Data point counter for time axis
    private int dataPointCounter = 0;

    // Maximum data points to keep in charts
    private static final int MAX_DATA_POINTS = 50;

    // System tracking variables
    private double totalSystemDiskSpace = 0;
    private double totalSystemUsedSpace = 0;

    // Memory circle properties
    private static final double MEMORY_CIRCLE_RADIUS = 60.0;
    private static final double MEMORY_CIRCLE_CIRCUMFERENCE = 2 * Math.PI * MEMORY_CIRCLE_RADIUS;

    // Animation timeline for smooth updates
    private Timeline updateTimeline;

    @FXML
    private void handleViewHistory() {
        // Load HistoricalView.fxml and display it in a new Scene or Dialog
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/HistoricalView.fxml"));
            Parent historicalRoot = loader.load();
            Scene historicalScene = new Scene(historicalRoot, 800, 600);
            historicalScene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/historical-styles.css")).toExternalForm());
            Stage stage = new Stage();

            stage.setScene(historicalScene);
            stage.setTitle("Historical Metrics");
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void initialize() {
        initializeCPUChart();
        initializeMemoryCircle();
        initializeNetworkChart();
        initializeDiskBar();
        initializeAnimations();

        // Apply initial styling classes
        applyInitialStyling();
    }

    private void initializeCPUChart() {
        cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU Usage");
        cpuChart.getData().add(cpuSeries);

        // Configure chart properties
        cpuChart.setCreateSymbols(false);
        cpuChart.setLegendVisible(false);
        cpuChart.setAnimated(false);

        // Set Y-axis bounds
        if (cpuChart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis) {
            yAxis.setLowerBound(0);
            yAxis.setUpperBound(100);
            yAxis.setTickUnit(20);
            yAxis.setAutoRanging(false);
        }
    }

    private void initializeMemoryCircle() {
        // Set up the progress circle with proper dash array
        memoryProgressCircle.getStrokeDashArray().clear();
        memoryProgressCircle.getStrokeDashArray().addAll(MEMORY_CIRCLE_CIRCUMFERENCE, MEMORY_CIRCLE_CIRCUMFERENCE);
        memoryProgressCircle.setStrokeDashOffset(MEMORY_CIRCLE_CIRCUMFERENCE); // Start fully hidden

        // Ensure circles have proper styling classes
        memoryBackgroundCircle.getStyleClass().add("circle-background");
        memoryProgressCircle.getStyleClass().add("circle-progress");

        // Set initial rotation for starting at top
        memoryProgressCircle.setRotate(-90);
    }

    private void initializeNetworkChart() {
        networkUploadSeries = new XYChart.Series<>();
        networkUploadSeries.setName("Upload");

        networkDownloadSeries = new XYChart.Series<>();
        networkDownloadSeries.setName("Download");

        networkChart.getData().addAll(networkUploadSeries, networkDownloadSeries);
        networkChart.setCreateSymbols(false);
        networkChart.setLegendVisible(false);
        networkChart.setAnimated(false);

        // Configure Y-axis for network chart
        if (networkChart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis) {
            yAxis.setAutoRanging(true);
            yAxis.setForceZeroInRange(true);
        }
    }

    private void initializeDiskBar() {
        // Initialize progress bar to 0
        totalDiskProgressBar.setProgress(0);

        // Set initial label values
        diskUsedLabel.setText("0 GB");
        diskAvailableLabel.setText("0 GB");
        diskTotalLabel.setText("0 GB");

        // Apply styling class
        totalDiskProgressBar.getStyleClass().add("disk-progress-main");
    }

    private void initializeAnimations() {
        // Create a timeline for smooth UI updates (optional - for future enhancements)
        updateTimeline = new Timeline();
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void applyInitialStyling() {
        // Ensure all components have their proper style classes
        cpuPercentageLabel.getStyleClass().add("stat-value");

        memoryUsedLabel.getStyleClass().add("circle-main-text");
        memoryPercentageLabel.getStyleClass().add("circle-percentage-text");

        diskUsedLabel.getStyleClass().add("disk-main-text");

        uploadSpeedLabel.getStyleClass().add("network-speed");
        downloadSpeedLabel.getStyleClass().add("network-speed-large");
    }

    public void updateCPUUsage(double cpuPercentage) {
        Platform.runLater(() -> {
            // Update label with proper formatting
            cpuPercentageLabel.setText(String.format("%.1f%%", cpuPercentage));

            // Add data point to chart with time-based x-axis
            String timePoint = String.valueOf(dataPointCounter++);
            cpuSeries.getData().add(new XYChart.Data<>(timePoint, cpuPercentage));

            // Maintain chart data size
            if (cpuSeries.getData().size() > MAX_DATA_POINTS) {
                cpuSeries.getData().removeFirst();
            }

            // Update chart Y-axis range dynamically if needed
            updateChartYAxisRange(cpuChart, cpuSeries);
        });
    }

    public void updateMemoryUsage(double totalGB, double usedGB, double availableGB) {
        Platform.runLater(() -> {
            // Update main circular display - show used amount
            memoryUsedLabel.setText(String.format("%.1f GB", usedGB));

            // Calculate and display percentage
            double usagePercentage = (usedGB / totalGB) * 100;
            memoryPercentageLabel.setText(String.format("%.1f%%", usagePercentage));

            // Update detailed statistics
            memoryUsedStatLabel.setText(String.format("%.1f GB", usedGB));
            memoryAvailableLabel.setText(String.format("%.1f GB", availableGB));
            memoryTotalLabel.setText(String.format("%.1f GB", totalGB));

            // Update circular progress with smooth animation
            updateMemoryCircularProgress(usagePercentage);
        });
    }

    public void updateDiskUsage(double totalGB, double usedGB, double availableGB) {
        Platform.runLater(() -> {
            // Store total system values
            totalSystemDiskSpace = totalGB;
            totalSystemUsedSpace = usedGB;

            // Calculate and display percentage
            double usagePercentage = totalGB > 0 ? (usedGB / totalGB) * 100 : 0;

            // Update progress bar
            totalDiskProgressBar.setProgress(usagePercentage / 100.0);

            // Update labels with proper formatting
            diskUsedLabel.setText(String.format("%.1f GB", usedGB));
            diskAvailableLabel.setText(String.format("%.1f GB", availableGB));
            diskTotalLabel.setText(String.format("%.1f GB", totalGB));
        });
    }

    private void updateMemoryCircularProgress(double percentage) {
        // Calculate the dash offset for the progress circle
        double progress = Math.max(0, Math.min(100, percentage)) / 100.0;
        double dashOffset = MEMORY_CIRCLE_CIRCUMFERENCE * (1 - progress);

        // Apply the offset to show progress
        memoryProgressCircle.setStrokeDashOffset(dashOffset);
    }

    public void updateNetworkActivity(double uploadKbps, double downloadKbps) {
        Platform.runLater(() -> {
            // Update upload speed with appropriate units
            uploadSpeedLabel.setText(formatNetworkSpeed(uploadKbps));

            // Update download speed with appropriate units
            downloadSpeedLabel.setText(formatNetworkSpeed(downloadKbps));

            // Add data points to network chart
            String timePoint = String.valueOf(dataPointCounter);
            networkUploadSeries.getData().add(new XYChart.Data<>(timePoint, uploadKbps));
            networkDownloadSeries.getData().add(new XYChart.Data<>(timePoint, downloadKbps));

            // Maintain chart data size for both series
            if (networkUploadSeries.getData().size() > MAX_DATA_POINTS) {
                networkUploadSeries.getData().removeFirst();
            }
            if (networkDownloadSeries.getData().size() > MAX_DATA_POINTS) {
                networkDownloadSeries.getData().removeFirst();
            }

            // Update network chart Y-axis range
            updateNetworkChartYAxisRange();
        });
    }

    private String formatNetworkSpeed(double speedKbps) {
        if (speedKbps >= 1000000) {
            return String.format("%.2f Gbps", speedKbps / 1000000.0);
        } else if (speedKbps >= 1000) {
            return String.format("%.2f Mbps", speedKbps / 1000.0);
        } else {
            return String.format("%.0f Kbps", speedKbps);
        }
    }

    private void updateChartYAxisRange(LineChart<String, Number> chart, XYChart.Series<String, Number> series) {
        if (chart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis && !series.getData().isEmpty()) {

            // For CPU chart, keep fixed range 0-100
            if (chart == cpuChart) {
                yAxis.setLowerBound(0);
                yAxis.setUpperBound(100);
                yAxis.setAutoRanging(false);
            }
        }
    }

    private void updateNetworkChartYAxisRange() {
        if (networkChart.getYAxis() instanceof javafx.scene.chart.NumberAxis yAxis) {
            yAxis.setAutoRanging(true);
            yAxis.setForceZeroInRange(true);
        }
    }

    // Comprehensive update method for all system statistics
    public void updateAllSystemStats(double cpuPercentage,
                                     double totalMemoryGB, double usedMemoryGB, double availableMemoryGB,
                                     double totalDiskGB, double usedDiskGB, double availableDiskGB,
                                     double uploadKbps, double downloadKbps) {
        updateCPUUsage(cpuPercentage);
        updateMemoryUsage(totalMemoryGB, usedMemoryGB, availableMemoryGB);
        updateDiskUsage(totalDiskGB, usedDiskGB, availableDiskGB);
        updateNetworkActivity(uploadKbps, downloadKbps);
    }

    // Utility method to reset all charts
    public void resetAllCharts() {
        Platform.runLater(() -> {
            cpuSeries.getData().clear();
            networkUploadSeries.getData().clear();
            networkDownloadSeries.getData().clear();
            dataPointCounter = 0;
        });
    }

    // Method to pause/resume chart updates
    public void pauseUpdates() {
        if (updateTimeline != null) {
            updateTimeline.pause();
        }
    }

    public void resumeUpdates() {
        if (updateTimeline != null) {
            updateTimeline.play();
        }
    }

    // Getters for current system state
    public double getTotalSystemDiskSpace() {
        return totalSystemDiskSpace;
    }

    public double getTotalSystemUsedSpace() {
        return totalSystemUsedSpace;
    }

    public int getCurrentDataPointCount() {
        return dataPointCounter;
    }

    public double getCurrentCPUUsage() {
        if (!cpuSeries.getData().isEmpty()) {
            return cpuSeries.getData().getLast().getYValue().doubleValue();
        }
        return 0.0;
    }

    // Method to get memory usage percentage
    public double getCurrentMemoryUsagePercentage() {
        String percentageText = memoryPercentageLabel.getText();
        if (percentageText != null && !percentageText.isEmpty()) {
            try {
                return Double.parseDouble(percentageText.replace("%", ""));
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    // Method to get disk usage percentage
    public double getCurrentDiskUsagePercentage() {
        if (totalSystemDiskSpace > 0) {
            return (totalSystemUsedSpace / totalSystemDiskSpace) * 100;
        }
        return 0.0;
    }
}