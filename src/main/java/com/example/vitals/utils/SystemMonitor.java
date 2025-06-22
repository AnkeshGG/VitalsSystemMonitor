package com.example.vitals.utils;

import com.example.vitals.controllers.DashboardController;
import com.example.vitals.controllers.SystemInfoController;
import com.example.vitals.dao.HistoricalDataDAO;
import com.example.vitals.models.CPUUsage;
import com.example.vitals.models.DiskUsage;
import com.example.vitals.models.MemoryUsage;
import com.example.vitals.models.NetworkActivity;
import javafx.application.Platform;

public class SystemMonitor implements Runnable {

    private final DashboardController dashboardController;
    private final SystemInfoController infoController;
    private final HistoricalDataDAO historicalDataDAO;

    public SystemMonitor(DashboardController dashboardController) {
        this.dashboardController = dashboardController;
        this.infoController = new SystemInfoController();
        this.historicalDataDAO = new HistoricalDataDAO();
    }

    @Override
    public void run() {
        while (true) {
            // Fetch live stats
            CPUUsage cpu = infoController.getLatestCPUUsage();
            MemoryUsage memory = infoController.getLatestMemoryUsage();
            DiskUsage disk = infoController.getLatestDiskUsage();
            NetworkActivity network = infoController.getLatestNetworkActivity();

            // Update UI on the JavaFX Application Thread
            Platform.runLater(() -> {
                dashboardController.updateCPUUsage(cpu.usagePercentage());
                dashboardController.updateMemoryUsage(memory.totalGB(), memory.usedGB(), memory.availableGB());
                double availableDiskGB = disk.totalGB() - disk.usedGB();
                dashboardController.updateDiskUsage(disk.totalGB(), disk.usedGB(), availableDiskGB);
                dashboardController.updateNetworkActivity(network.uploadSpeedKbps(), network.downloadSpeedKbps());
            });

            // Log metrics into the SQLite DB
            historicalDataDAO.logMetrics(cpu.usagePercentage(), memory.usedGB(), memory.totalGB(), memory.availableGB());

            try {
                Thread.sleep(2000); // update every 2 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}