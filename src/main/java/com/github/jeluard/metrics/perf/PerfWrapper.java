/*
 * Copyright 2012 julien.
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

import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.Metric;
import com.yammer.metrics.core.MetricName;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Perf counter wrapper for {@link Metric}.
 * 
 * @param <T> 
 */
public abstract class PerfWrapper<T extends Metric> {

  public PerfWrapper(final MetricName name, final T metric) {
  }

  /**
   * Update this perf counter with provided `meter`.
   * 
   * @param meter 
   */
  public abstract void update(T meter);

  protected final String createName(final MetricName name, final String suffix) {
    return "metric."+name.getName()+"."+suffix;
  }

  protected final ByteBuffer createBuffer(final MetricName name, final String suffix, final int variability, final int units, final double value) {
    return createBuffer(name, suffix, variability, units, (long) value);
  }

  protected final ByteBuffer createBuffer(final MetricName name, final String suffix, final int variability, final int units, final long value) {
     final ByteBuffer buffer = Perfs.createBuffer(createName(name, suffix), variability, units, value);
     buffer.order(ByteOrder.nativeOrder());
     buffer.rewind();
     return buffer;
  }

  protected final void update(final ByteBuffer buffer, final double value) {
    update(buffer, (long) value);
  }

  protected final void update(final ByteBuffer buffer, final long value) {
    buffer.putLong(value);
    buffer.rewind();
  }

  /**
   * Wraps {@link Gauge}.
   */
  public static class Gauge extends PerfWrapper<com.yammer.metrics.core.Gauge> {

    private final ByteBuffer buffer;

    public Gauge(final MetricName name, final com.yammer.metrics.core.Gauge gauge) {
      super(name, gauge);

      this.buffer = createBuffer(name, "gauge", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, Number.class.cast(gauge.value()).longValue());//We know it's a Number
    }

    @Override
    public void update(final com.yammer.metrics.core.Gauge gauge) {
      if (gauge.value() instanceof Number) {
        update(this.buffer, Number.class.cast(gauge.value()).longValue());
      }
    }

  }

  /**
   * Wraps {@link Counter}.
   */
  public static class Counter extends PerfWrapper<com.yammer.metrics.core.Counter> {

    private final ByteBuffer buffer;

    public Counter(final MetricName name, final com.yammer.metrics.core.Counter metric) {
      super(name, metric);

      this.buffer = createBuffer(name, "counter", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, metric.count());
    }

    @Override
    public void update(final com.yammer.metrics.core.Counter meter) {
      update(this.buffer, meter.count());
    }

  }

  /**
   * Wraps {@link Timer}.
   */
  public static class Timer extends PerfWrapper<com.yammer.metrics.core.Timer> {

    private final ByteBuffer countBuffer;
    private final ByteBuffer minBuffer;
    private final ByteBuffer maxBuffer;
    private final ByteBuffer meanBuffer;
    private final ByteBuffer sumBuffer;
    private final ByteBuffer stdDevBuffer;
    private final ByteBuffer meanRateBuffer;
    private final ByteBuffer oneMinRateBuffer;
    private final ByteBuffer fiveMinRateBuffer;
    private final ByteBuffer fifteenMinRateBuffer;
    private final ByteBuffer snapshotMedianBuffer;
    private final ByteBuffer snapshot75thPercentileBuffer;
    private final ByteBuffer snapshot95thPercentileBuffer;
    private final ByteBuffer snapshot98thPercentileBuffer;
    private final ByteBuffer snapshot99thPercentileBuffer;
    private final ByteBuffer snapshot999thPercentileBuffer;

    public Timer(final MetricName name, final com.yammer.metrics.core.Timer timer) {
      super(name, timer);

      this.countBuffer = createBuffer(name, "count", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.count());
      this.minBuffer = createBuffer(name, "min", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.min());
      this.maxBuffer = createBuffer(name, "max", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.max());
      this.meanBuffer = createBuffer(name, "mean", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.mean());
      this.sumBuffer = createBuffer(name, "sum", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.sum());
      this.stdDevBuffer = createBuffer(name, "std-dev", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.stdDev());
      this.meanRateBuffer = createBuffer(name, "mean-rate", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.meanRate());
      this.oneMinRateBuffer = createBuffer(name, "1min-rate", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.oneMinuteRate());
      this.fiveMinRateBuffer = createBuffer(name, "5min-rate", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.fiveMinuteRate());
      this.fifteenMinRateBuffer = createBuffer(name, "15min-rate", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.fifteenMinuteRate());
      this.snapshotMedianBuffer = createBuffer(name, "snapshot.median", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.getSnapshot().getMedian());
      this.snapshot75thPercentileBuffer = createBuffer(name, "snapshot.75th", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.getSnapshot().get75thPercentile());
      this.snapshot95thPercentileBuffer = createBuffer(name, "snapshot.95th", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.getSnapshot().get95thPercentile());
      this.snapshot98thPercentileBuffer = createBuffer(name, "snapshot.98th", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.getSnapshot().get98thPercentile());
      this.snapshot99thPercentileBuffer = createBuffer(name, "snapshot.99th", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.getSnapshot().get99thPercentile());
      this.snapshot999thPercentileBuffer = createBuffer(name, "snapshot.999th", Perfs.VARIABILITY_MONOTONIC, Perfs.UNITS_EVENTS, timer.getSnapshot().get999thPercentile());
    }

    @Override
    public void update(final com.yammer.metrics.core.Timer timer) {
      update(this.countBuffer, timer.count());
      update(this.minBuffer, timer.min());
      update(this.maxBuffer, timer.max());
      update(this.meanBuffer, timer.mean());
      update(this.sumBuffer, timer.sum());
      update(this.stdDevBuffer, timer.stdDev());
      update(this.meanRateBuffer, timer.meanRate());
      update(this.oneMinRateBuffer, timer.oneMinuteRate());
      update(this.fiveMinRateBuffer, timer.fiveMinuteRate());
      update(this.fifteenMinRateBuffer, timer.fifteenMinuteRate());
      update(this.snapshotMedianBuffer, timer.getSnapshot().getMedian());
      update(this.snapshot75thPercentileBuffer, timer.getSnapshot().get75thPercentile());
      update(this.snapshot95thPercentileBuffer, timer.getSnapshot().get95thPercentile());
      update(this.snapshot98thPercentileBuffer, timer.getSnapshot().get98thPercentile());
      update(this.snapshot99thPercentileBuffer, timer.getSnapshot().get99thPercentile());
      update(this.snapshot999thPercentileBuffer, timer.getSnapshot().get999thPercentile());
    }

  }

}