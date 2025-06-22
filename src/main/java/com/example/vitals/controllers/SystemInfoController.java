package com.example.vitals.controllers;

import com.example.vitals.models.CPUUsage;
import com.example.vitals.models.DiskUsage;
import com.example.vitals.models.MemoryUsage;
import com.example.vitals.models.NetworkActivity;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.Sensors;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.hardware.NetworkIF;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SystemInfoController {

    private final HardwareAbstractionLayer hal;
    private final OperatingSystem os;
    private long[] prevTicks; // To store previous CPU ticks for load calculation

    // Network tracking variables
    private final Map<String, Long> prevBytesSent = new HashMap<>();
    private final Map<String, Long> prevBytesRecv = new HashMap<>();
    private long lastNetworkMeasurement = 0;

    public SystemInfoController() {
        SystemInfo systemInfo = new SystemInfo();
        hal = systemInfo.getHardware();
        os = systemInfo.getOperatingSystem();

        // Initialize previous ticks for the first measurement
        CentralProcessor processor = hal.getProcessor();
        prevTicks = processor.getSystemCpuLoadTicks();

        // Initialize network tracking
        initializeNetworkTracking();
    }

    public Map<String, String> buildSystemSnapshot() {
        // Create an instance of SystemInfoController if not already available
        SystemInfoController systemInfo = new SystemInfoController();

        // Retrieve data from OSHI-based methods
        CPUUsage cpu = systemInfo.getLatestCPUUsage();
        MemoryUsage mem = systemInfo.getLatestMemoryUsage();
        DiskUsage disk = systemInfo.getLatestDiskUsage();
        NetworkActivity net = systemInfo.getLatestNetworkActivity();

        // Format values as strings
        Map<String, String> snapshot = new HashMap<>();
        snapshot.put("CPU Usage", cpu.usagePercentage() + "% (Temp: " + cpu.temperature() + "°C, Clock: " + cpu.clockSpeed() + " MHz, Processes: " + cpu.processCount() + ")");
        snapshot.put("Memory Used", String.format("%.2f GB", mem.usedGB()));
        snapshot.put("Memory Available", String.format("%.2f GB", mem.availableGB()));
        snapshot.put("Disk Usage", String.format("Used: %.2f GB / Total: %.2f GB", disk.usedGB(), disk.totalGB()));
        snapshot.put("Network Activity", "Upload: " + net.uploadSpeedKbps() + " Kbps, Download: " + net.downloadSpeedKbps() + " Kbps");

        return snapshot;
    }

    private void initializeNetworkTracking() {
        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();
            prevBytesSent.put(net.getName(), net.getBytesSent());
            prevBytesRecv.put(net.getName(), net.getBytesRecv());
        }
        lastNetworkMeasurement = System.currentTimeMillis();
    }

    public CPUUsage getLatestCPUUsage() {
        CentralProcessor processor = hal.getProcessor();

        // Retrieve current ticks and compute load between the previous and current ticks
        long[] currentTicks = processor.getSystemCpuLoadTicks();
        double load = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100.0;

        // Update prevTicks for the next computation
        prevTicks = currentTicks;

        Sensors sensors = hal.getSensors();
        double temperature = sensors.getCpuTemperature();
        long maxFreq = processor.getMaxFreq();  // in Hz
        int processCount = os.getProcessCount();

        // Optionally convert max frequency from Hz to MHz, if desired:
        int clockSpeed = (int) (maxFreq / 1_000_000);

        return new CPUUsage((int) Math.min(100, Math.max(0, load)), (int) temperature, clockSpeed, processCount);
    }

    public MemoryUsage getLatestMemoryUsage() {
        GlobalMemory memory = hal.getMemory();
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        double totalGB = total / (1024.0 * 1024 * 1024);
        double usedGB = used / (1024.0 * 1024 * 1024);
        double availableGB = available / (1024.0 * 1024 * 1024);
        // Windows typically does not provide cache memory via OSHI
        double cacheGB = 0.0;
        return new MemoryUsage(totalGB, usedGB, cacheGB, availableGB);
    }

    public List<DiskUsage> getAllDiskUsage() {
        List<DiskUsage> diskUsages = new ArrayList<>();
        FileSystem fs = os.getFileSystem();
        List<OSFileStore> fileStores = fs.getFileStores();

        for (OSFileStore store : fileStores) {
            String mount = store.getMount().toUpperCase();
            // Filter for common drive letters on Windows
            if (mount.length() >= 2 && mount.charAt(1) == ':') {
                char driveLetter = mount.charAt(0);
                if (driveLetter >= 'A' && driveLetter <= 'Z') {
                    long total = store.getTotalSpace();
                    long usable = store.getUsableSpace();
                    long used = total - usable;
                    double totalGB = total / (1024.0 * 1024 * 1024);
                    double usedGB = used / (1024.0 * 1024 * 1024);
                    diskUsages.add(new DiskUsage(usedGB, driveLetter + ":", totalGB));
                }
            }
        }

        // If no drives found, return at least C: with dummy data
        if (diskUsages.isEmpty()) {
            diskUsages.add(new DiskUsage(0, "C:", 0));
        }

        return diskUsages;
    }

    // Legacy method for backward compatibility
    public DiskUsage getLatestDiskUsage() {
        List<DiskUsage> allDisks = getAllDiskUsage();
        // Return C: drive if available, otherwise first drive
        for (DiskUsage disk : allDisks) {
            if (disk.driveName().equals("C:")) {
                return disk;
            }
        }
        return allDisks.isEmpty() ? new DiskUsage(0, "C:", 0) : allDisks.getFirst();
    }

    public NetworkActivity getLatestNetworkActivity() {
        long currentTime = System.currentTimeMillis();
        long timeDiff = currentTime - lastNetworkMeasurement;

        // If less than 1 second has passed, return previous values
        if (timeDiff < 1000) {
            return new NetworkActivity(0, 0);
        }

        long totalBytesSent = 0;
        long totalBytesRecv = 0;
        long totalPrevBytesSent = 0;
        long totalPrevBytesRecv = 0;

        List<NetworkIF> networkIFs = hal.getNetworkIFs();
        for (NetworkIF net : networkIFs) {
            net.updateAttributes();

            String netName = net.getName();
            long currentSent = net.getBytesSent();
            long currentRecv = net.getBytesRecv();

            totalBytesSent += currentSent;
            totalBytesRecv += currentRecv;

            // Get previous values
            Long prevSent = prevBytesSent.get(netName);
            Long prevRecv = prevBytesRecv.get(netName);

            if (prevSent != null && prevRecv != null) {
                totalPrevBytesSent += prevSent;
                totalPrevBytesRecv += prevRecv;
            }

            // Update previous values
            prevBytesSent.put(netName, currentSent);
            prevBytesRecv.put(netName, currentRecv);
        }

        // Calculate speeds in bytes per second
        double timeInSeconds = timeDiff / 1000.0;
        long uploadBytesPerSec = (long) ((totalBytesSent - totalPrevBytesSent) / timeInSeconds);
        long downloadBytesPerSec = (long) ((totalBytesRecv - totalPrevBytesRecv) / timeInSeconds);

        // Convert to Kbps (kilobits per second)
        int uploadKbps = Math.max(0, (int) (uploadBytesPerSec * 8 / 1024));
        int downloadKbps = Math.max(0, (int) (downloadBytesPerSec * 8 / 1024));

        lastNetworkMeasurement = currentTime;

        return new NetworkActivity(uploadKbps, downloadKbps);
    }
}