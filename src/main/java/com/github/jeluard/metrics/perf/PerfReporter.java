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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import sun.management.counter.Units;
import sun.management.counter.Variability;
import sun.misc.Perf;

public class PerfReporter extends AbstractPollingReporter implements MetricsRegistryListener, MetricProcessor<String> {

  private static final String REPORTER_NAME = "perf-reporter";

  private final Map<MetricName, ByteBuffer> metrics = new ConcurrentHashMap<MetricName, ByteBuffer>();

  public PerfReporter(final MetricsRegistry metricsRegistry) {
    super(metricsRegistry, PerfReporter.REPORTER_NAME);
  }

  protected final Perf getperf() {
    return (Perf) AccessController.doPrivileged(new Perf.GetPerfAction());
  }

  @Override
  public void start(long period, TimeUnit unit) {
    getMetricsRegistry().addListener(this);

    super.start(period, unit);
  }

  @Override
  public void run() {
    for (final Map.Entry<MetricName, Metric> entry : Metrics.defaultRegistry().allMetrics().entrySet()) {
      final ByteBuffer buffer = this.metrics.get(entry.getKey());
      final Metric metric = entry.getValue();
      final long value;
      if (metric instanceof Counter) {
        value = ((Counter) metric).count();
      } else if (metric instanceof Timer) {
        value = ((Timer) metric).count();
      } else {
        //Unrecognized Metric, skip it
        continue;
      }

      buffer.putLong(value);
      buffer.rewind();
    }
  }

  @Override
  public void onMetricAdded(final MetricName name, final Metric metric) {
    if (metric != null) {
      try {
          metric.processWith(this, name, "");
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

  protected final ByteBuffer createByteBuffer(final MetricName name, final Variability variability, final Units units, final long value) {
     final Perf perf = getperf();
     final ByteBuffer buffer = perf.createLong("metric.test."+name.getName(), variability.intValue(), units.intValue(), value);
     buffer.order(ByteOrder.nativeOrder());
     buffer.rewind();
     return buffer;
  }

  private void initializePerfCounter(final MetricName name, final Variability variability, final Units units, final long value) {
    this.metrics.put(name, createByteBuffer(name, variability, units, value));
  }

  @Override
  public void processMeter(final MetricName name, final Metered metered, final String t) throws Exception {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void processCounter(final MetricName name, final Counter counter, final String string) throws Exception {
     initializePerfCounter(name, Variability.MONOTONIC, Units.EVENTS, counter.count());
  }

  @Override
  public void processHistogram(final MetricName name, final Histogram histogram, final String t) throws Exception {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public void processTimer(final MetricName name, final Timer timer, final String t) throws Exception {
    initializePerfCounter(name, Variability.MONOTONIC, Units.EVENTS, timer.count());
  }

  @Override
  public void processGauge(final MetricName name, final Gauge<?> gauge, final String t) throws Exception {
    final Object value = gauge.value();
    if (value instanceof Long) {
      initializePerfCounter(name, Variability.CONSTANT, Units.EVENTS, (Long) value);
    }
    //Only support long type
  }

  @Override
  public void shutdown() {
    getMetricsRegistry().removeListener(this);

    super.shutdown();
  }

}