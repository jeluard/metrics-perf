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

public class PerfReporter extends AbstractPollingReporter implements MetricsRegistryListener, MetricProcessor<Void> {

  private static final String REPORTER_NAME = "perf-reporter";

  private final Map<MetricName, PerfWrapper> perfWrappers = new ConcurrentHashMap<MetricName, PerfWrapper>();

  public PerfReporter(final MetricsRegistry metricsRegistry) {
    super(metricsRegistry, PerfReporter.REPORTER_NAME);
  }

  @Override
  public void start(final long period, final TimeUnit unit) {
    getMetricsRegistry().addListener(this);

    super.start(period, unit);
  }

  @Override
  public void run() {
    for (final Map.Entry<MetricName, Metric> entry : Metrics.defaultRegistry().allMetrics().entrySet()) {
      final PerfWrapper wrapper = this.perfWrappers.get(entry.getKey());
      if (wrapper == null) {
        //TODO log
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
        e.printStackTrace();
          //LOGGER.warn("Error processing {}", name, e);
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
    if (value instanceof Number) {
      this.perfWrappers.put(name, new PerfWrapper.Gauge(name, gauge));
    }
    //Only support Numbers
  }

  @Override
  public void shutdown() {
    getMetricsRegistry().removeListener(this);

    super.shutdown();
  }

}