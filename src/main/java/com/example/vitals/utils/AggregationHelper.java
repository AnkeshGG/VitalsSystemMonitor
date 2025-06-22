package com.example.vitals.utils;

import com.example.vitals.dao.HistoricalDataDAO;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Helper class for aggregating historical metric records into time buckets.
 */
public class AggregationHelper {

    public static List<HistoricalDataDAO.MetricRecord> aggregateRecords(List<HistoricalDataDAO.MetricRecord> records, long bucketMinutes) {
        // Use a TreeMap to keep the buckets sorted by time
        Map<LocalDateTime, List<HistoricalDataDAO.MetricRecord>> buckets = new TreeMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        // Group the records into time buckets
        for (HistoricalDataDAO.MetricRecord record : records) {
            // Parse the timestamp (assumed to be in ISO_LOCAL_DATE_TIME format)
            LocalDateTime dateTime = LocalDateTime.parse(record.timestamp(), formatter);
            // Determine the bucket starting time by rounding down the minutes
            int minuteBucket = (dateTime.getMinute() / (int) bucketMinutes) * (int) bucketMinutes;
            LocalDateTime bucketStart = dateTime.truncatedTo(ChronoUnit.HOURS).plusMinutes(minuteBucket);

            buckets.computeIfAbsent(bucketStart, k -> new ArrayList<>()).add(record);
        }

        // Prepare the aggregated records list
        List<HistoricalDataDAO.MetricRecord> aggregatedRecords = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<HistoricalDataDAO.MetricRecord>> entry : buckets.entrySet()) {
            List<HistoricalDataDAO.MetricRecord> group = entry.getValue();

            // Compute averages for each metric
            double avgCpu = group.stream().mapToDouble(r -> r.cpuUsage()).average().orElse(0.0);
            double avgMemUsed = group.stream().mapToDouble(r -> r.memoryUsed()).average().orElse(0.0);
            double avgMemTotal = group.stream().mapToDouble(r -> r.memoryTotal()).average().orElse(0.0);
            double avgMemAvailable = group.stream().mapToDouble(r -> r.memoryAvailable()).average().orElse(0.0);

            // Format the bucket's start time as a string (you can adjust the pattern if needed)
            String aggregatedTimestamp = entry.getKey().format(formatter);
            aggregatedRecords.add(new HistoricalDataDAO.MetricRecord(aggregatedTimestamp, avgCpu, avgMemUsed, avgMemTotal, avgMemAvailable));
        }

        return aggregatedRecords;
    }
}
