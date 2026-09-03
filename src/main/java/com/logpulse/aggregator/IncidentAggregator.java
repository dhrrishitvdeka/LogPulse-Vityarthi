package com.logpulse.aggregator;

import com.logpulse.model.AnomalyType;
import com.logpulse.model.Incident;
import com.logpulse.model.SeverityLevel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class IncidentAggregator {

    private final ConcurrentLinkedQueue<Incident> incidents = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<String, List<Incident>> ipIncidentMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<AnomalyType, Integer> anomalyTypeCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SeverityLevel, Integer> severityCounts = new ConcurrentHashMap<>();

    public void record(Incident incident) {
        if (incident == null) return;

        incidents.add(incident);
        ipIncidentMap.computeIfAbsent(incident.getClientIp(), k -> Collections.synchronizedList(new ArrayList<>()))
                     .add(incident);

        anomalyTypeCounts.merge(incident.getAnomalyType(), 1, Integer::sum);
        severityCounts.merge(incident.getSeverity(), 1, Integer::sum);
    }

    public List<Incident> getAllIncidents() {
        return new ArrayList<>(incidents);
    }

    public int getTotalIncidentCount() {
        return incidents.size();
    }

    public Map<AnomalyType, Integer> getAnomalyTypeCounts() {
        return Collections.unmodifiableMap(anomalyTypeCounts);
    }

    public Map<SeverityLevel, Integer> getSeverityCounts() {
        return Collections.unmodifiableMap(severityCounts);
    }

    public List<IpOffenseSummary> getTopOffenders(int k) {
        if (k <= 0 || ipIncidentMap.isEmpty()) {
            return Collections.emptyList();
        }

        PriorityQueue<IpOffenseSummary> minHeap = new PriorityQueue<>(
                Comparator.comparingInt(IpOffenseSummary::incidentCount)
        );

        for (Map.Entry<String, List<Incident>> entry : ipIncidentMap.entrySet()) {
            String ip = entry.getKey();
            List<Incident> ipIncidents = entry.getValue();

            SeverityLevel highestSev = ipIncidents.stream()
                    .map(Incident::getSeverity)
                    .max(Comparator.comparingInt(SeverityLevel::getRank))
                    .orElse(SeverityLevel.INFO);

            Map<AnomalyType, Integer> breakdown = new EnumMap<>(AnomalyType.class);
            for (Incident inc : ipIncidents) {
                breakdown.merge(inc.getAnomalyType(), 1, Integer::sum);
            }

            IpOffenseSummary summary = new IpOffenseSummary(ip, ipIncidents.size(), highestSev, breakdown);

            if (minHeap.size() < k) {
                minHeap.offer(summary);
            } else if (minHeap.peek() != null && summary.incidentCount() > minHeap.peek().incidentCount()) {
                minHeap.poll();
                minHeap.offer(summary);
            }
        }

        List<IpOffenseSummary> result = new ArrayList<>(minHeap);
        result.sort((a, b) -> Integer.compare(b.incidentCount(), a.incidentCount()));
        return result;
    }

    public record IpOffenseSummary(
            String ip,
            int incidentCount,
            SeverityLevel maxSeverity,
            Map<AnomalyType, Integer> typeBreakdown
    ) {}
}
