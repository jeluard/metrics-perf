/*
 * Copyright 2012 Julien Eluard.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.jeluard.metrics.perf;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Gauge;
import com.yammer.metrics.core.Histogram;
import com.yammer.metrics.core.Metered;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;
import com.yammer.metrics.core.MetricProcessor;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.MetricsRegistryListener;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.reporting.AbstractPollingReporter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link AbstractPollingReporter} that exports all metrics as Hotspot perf counters.
 */
public class PerfReporter extends AbstractPollingReporter implements MetricsRegistryListener, MetricProcessor<Void> {

  private static final String REPORTER_NAME = "perf-reporter";
  static final Logger LOGGER = Logger.getLogger(PerfReporter.class.getPackage().getName());

  private final Map<MetricName, PerfWrapper> perfWrappers = new ConcurrentHashMap<MetricName, PerfWrapper>();

  public PerfReporter(final MetricsRegistry metricsRegistry) {
    super(metricsRegistry, PerfReporter.REPORTER_NAME);
  }

  @Override
  public void start(final long period, final TimeUnit unit) {
    if (!Perfs.isPlatformSupported()) {
      if (PerfReporter.LOGGER.isLoggable(Level.WARNING)) {
        PerfReporter.LOGGER.warning("Current platform does not support perf counters. Metrics won't be exported as perf counters.");
      }

      return;
    }
    
    getMetricsRegistry().addListener(this);

    super.start(period, unit);
  }

  @Override
  public void run() {
    for (final Map.Entry<MetricName, Metric> entry : Metrics.defaultRegistry().allMetrics().entrySet()) {
      final MetricName name = entry.getKey();
      final PerfWrapper wrapper = this.perfWrappers.get(name);
      if (wrapper == null) {
        //TODO add to blacklist so that we don't print this in a loop
        if (PerfReporter.LOGGER.isLoggable(Level.WARNING)) {
          PerfReporter.LOGGER.log(Level.WARNING, "No perf counter found for <{0}>; skipping.", name.getName());
        }

        continue;
      }

      final Metric metric = entry.getValue();
      wrapper.update(metric);
    }
  }

  @Override
  public void onMetricAdded(final MetricName name, final Metric metric) {
    if (metric != null) {
      try {
          metric.processWith(this, name, null);
      } catch (Exception e) {
        if (PerfReporter.LOGGER.isLoggable(Level.WARNING)) {
          PerfReporter.LOGGER.log(Level.WARNING, "Failed to process <"+name.getName()+">.", e);
        }
      }
    }
  }

  @Override
  public void onMetricRemoved(final MetricName name) {
    //Once added a perf counter can't be removed.
  }

  @Override
  public void processMeter(final MetricName name, final Metered metered, final Void context) throws Exception {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void processCounter(final MetricName name, final Counter counter, final Void context) throws Exception {
     this.perfWrappers.put(name, new PerfWrapper.Counter(name, counter));
  }

  @Override
  public void processHistogram(final MetricName name, final Histogram histogram, final Void context) throws Exception {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void processTimer(final MetricName name, final Timer timer, final Void context) throws Exception {
    this.perfWrappers.put(name, new PerfWrapper.Timer(name, timer));
  }

  @Override
  public void processGauge(final MetricName name, final Gauge<?> gauge, final Void context) throws Exception {
    final Object value = gauge.value();
    if (!(value instanceof Number)) {
      if (PerfReporter.LOGGER.isLoggable(Level.FINE)) {
        PerfReporter.LOGGER.log(Level.FINE, "Skipping <{0}> as its type is not supported.", name.getName());
      }

      return;
    }

    this.perfWrappers.put(name, new PerfWrapper.Gauge(name, gauge));
  }

  @Override
  public void shutdown() {
    getMetricsRegistry().removeListener(this);

    super.shutdown();
  }

}