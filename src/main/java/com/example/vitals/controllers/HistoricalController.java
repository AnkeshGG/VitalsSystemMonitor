package com.example.vitals.controllers;

import com.example.vitals.dao.HistoricalDataDAO;
import com.example.vitals.dao.HistoricalDataDAO.MetricRecord;
import com.example.vitals.models.CPUUsage;
import com.example.vitals.models.DiskUsage;
import com.example.vitals.models.MemoryUsage;
import com.example.vitals.models.NetworkActivity;
import com.example.vitals.utils.ReportExporter;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoricalController {

    @FXML private LineChart<String, Number> cpuHistoryChart;
    @FXML private LineChart<String, Number> memoryHistoryChart;
    @FXML private ComboBox<TimePeriod> timePeriodComboBox;
    @FXML private MenuButton exportMenuButton;
    @FXML private MenuItem exportPdfMenuItem;
    @FXML private MenuItem exportCsvMenuItem;

    private final HistoricalDataDAO historicalDataDAO = new HistoricalDataDAO();

    // Enum to represent time periods with display names and SQLite modifiers
    public enum TimePeriod {
        HOUR("Past Hour", "-1 hour"),
        DAY("Past Day", "-1 day"),
        WEEK("Past Week", "-7 days");

        private final String displayName;
        private final String sqliteModifier;

        TimePeriod(String displayName, String sqliteModifier) {
            this.displayName = displayName;
            this.sqliteModifier = sqliteModifier;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getSqliteModifier() {
            return sqliteModifier;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @FXML
    public void initialize() {
        // Initialize ComboBox with time period options
        timePeriodComboBox.setItems(FXCollections.observableArrayList(TimePeriod.values()));
        timePeriodComboBox.setValue(TimePeriod.HOUR); // Default to past hour

        // Add listener for ComboBox selection changes
        timePeriodComboBox.setOnAction(event -> loadChartsForSelectedPeriod());

        // Initialize export menu button actions
        exportPdfMenuItem.setOnAction(event -> exportToPdf());
        exportCsvMenuItem.setOnAction(event -> exportToCsv());

        // Load initial data for the default selection
        loadChartsForSelectedPeriod();
    }

    private void loadChartsForSelectedPeriod() {
        TimePeriod selectedPeriod = timePeriodComboBox.getValue();
        if (selectedPeriod == null) {
            return;
        }

        // Clear existing chart data
        cpuHistoryChart.getData().clear();
        memoryHistoryChart.getData().clear();

        // Load data for the selected period
        List<MetricRecord> records = historicalDataDAO.getMetricsForPeriod(selectedPeriod.getSqliteModifier());

        if (records.isEmpty()) {
            // Update chart titles to reflect no data
            cpuHistoryChart.setTitle("CPU Usage (" + selectedPeriod.getDisplayName() + ") - No Data");
            memoryHistoryChart.setTitle("Memory Usage (" + selectedPeriod.getDisplayName() + ") - No Data");
            return;
        }

        // Update chart titles
        cpuHistoryChart.setTitle("CPU Usage (" + selectedPeriod.getDisplayName() + ")");
        memoryHistoryChart.setTitle("Memory Usage (" + selectedPeriod.getDisplayName() + ")");

        // Create new data series
        XYChart.Series<String, Number> cpuSeries = new XYChart.Series<>();
        cpuSeries.setName("CPU Usage");
        XYChart.Series<String, Number> memorySeries = new XYChart.Series<>();
        memorySeries.setName("Memory Used (GB)");

        // Populate series with data
        for (MetricRecord record : records) {
            String timeLabel = formatTimeLabel(record.timestamp(), selectedPeriod);
            cpuSeries.getData().add(new XYChart.Data<>(timeLabel, record.cpuUsage()));
            memorySeries.getData().add(new XYChart.Data<>(timeLabel, record.memoryUsed()));
        }

        // Add series to charts
        cpuHistoryChart.getData().add(cpuSeries);
        memoryHistoryChart.getData().add(memorySeries);
    }

    /**
     * Formats the timestamp for display on the chart based on the selected time period.
     * For hour and day views: HH:mm
     * For week view: MM-dd HH:mm
     */
    private String formatTimeLabel(String timestamp, TimePeriod period) {
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return switch (period) {
                case HOUR, DAY -> dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
                case WEEK -> dateTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            };
        } catch (DateTimeParseException e) {
            if (timestamp.length() >= 16) {
                return timestamp.substring(11, 16); // Fallback: Extract HH:mm
            }
            return timestamp;
        }
    }

    /**
     * Exports a system snapshot report as a PDF.
     * The snapshot is obtained from the OSHI-based SystemInfoController.
     */
    private void exportToPdf() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        // Use the current time period (if available) for default filename
        TimePeriod selectedPeriod = timePeriodComboBox.getValue();
        String defaultFileName = String.format("system_snapshot_%s_%s.pdf",
                selectedPeriod != null ? selectedPeriod.name().toLowerCase() : "snapshot",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        );
        fileChooser.setInitialFileName(defaultFileName);
        Stage stage = (Stage) exportMenuButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Map<String, String> snapshot = buildSystemSnapshot();
            try {
                ReportExporter.exportToPDF(snapshot, file);
                System.out.println("PDF export successful: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                // Optionally display an error dialog
            }
        }
    }

    /**
     * Exports a system snapshot report as a CSV.
     * The snapshot is obtained from the OSHI-based SystemInfoController.
     */
    private void exportToCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export to CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        TimePeriod selectedPeriod = timePeriodComboBox.getValue();
        String defaultFileName = String.format("system_snapshot_%s_%s.csv",
                selectedPeriod != null ? selectedPeriod.name().toLowerCase() : "snapshot",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        );
        fileChooser.setInitialFileName(defaultFileName);
        Stage stage = (Stage) exportMenuButton.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            Map<String, String> snapshot = buildSystemSnapshot();
            try {
                ReportExporter.exportToCSV(snapshot, file);
                System.out.println("CSV export successful: " + file.getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                // Optionally display an error dialog
            }
        }
    }

    /**
     * Gathers a snapshot of the current system vitals using OSHI-based SystemInfoController.
     * The snapshot includes:
     * - CPU Usage (with temperature, clock speed, process count)
     * - Memory Usage (used and available)
     * - Disk Usage (used and total)
     * - Network Activity (upload and download speeds)
     *
     * @return A map with system vital names as keys and their corresponding values as strings.
     */
    private Map<String, String> buildSystemSnapshot() {
        Map<String, String> snapshot = new HashMap<>();
        // Create an instance of SystemInfoController
        SystemInfoController systemInfo = new SystemInfoController();

        // Fetch metrics from OSHI
        CPUUsage cpu = systemInfo.getLatestCPUUsage();
        MemoryUsage mem = systemInfo.getLatestMemoryUsage();
        DiskUsage disk = systemInfo.getLatestDiskUsage();
        NetworkActivity net = systemInfo.getLatestNetworkActivity();

        // Populate the snapshot map with formatted values
        snapshot.put("CPU Usage", cpu.usagePercentage() + "% (Temp: " + cpu.temperature() + "°C, Clock: " + cpu.clockSpeed() + " MHz, Processes: " + cpu.processCount() + ")");
        snapshot.put("Memory Used", String.format("%.2f GB", mem.usedGB()));
        snapshot.put("Memory Available", String.format("%.2f GB", mem.availableGB()));
        snapshot.put("Disk Usage", String.format("Used: %.2f GB / Total: %.2f GB", disk.usedGB(), disk.totalGB()));
        snapshot.put("Network Activity", "Upload: " + net.uploadSpeedKbps() + " Kbps, Download: " + net.downloadSpeedKbps() + " Kbps");

        return snapshot;
    }

    /**
     * Method to refresh the charts (can be called externally if needed)
     */
    public void refreshCharts() {
        loadChartsForSelectedPeriod();
    }

    /**
     * Get the currently selected time period.
     */
    public TimePeriod getSelectedTimePeriod() {
        return timePeriodComboBox.getValue();
    }

    /**
     * Set the selected time period programmatically.
     */
    public void setSelectedTimePeriod(TimePeriod period) {
        timePeriodComboBox.setValue(period);
    }
}
