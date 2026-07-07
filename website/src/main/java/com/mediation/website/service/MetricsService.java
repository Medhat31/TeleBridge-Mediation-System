package com.mediation.website.service;

import com.mediation.website.repository.MetricsRepository;

/**
 * Service layer for aggregating and retrieving system metrics.
 * Provides data to the dashboard to show system performance.
 */
public class MetricsService {

    private final MetricsRepository metricsRepository;

    public MetricsService() {
        this.metricsRepository = new MetricsRepository();
    }

    /**
     * Retrieves the total count of CDRs processed by the core engine today.
     *
     * @return The number of processed CDRs.
     */
    public int getCdrsProcessedToday() {
        return metricsRepository.getCdrsProcessedToday();
    }
}
