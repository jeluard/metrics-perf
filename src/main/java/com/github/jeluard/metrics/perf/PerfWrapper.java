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
import java.security.AccessController;
import sun.management.counter.Units;
import sun.management.counter.Variability;
import sun.misc.Perf;

public abstract class PerfWrapper<T extends Metric> {

  public PerfWrapper(final MetricName name, final T metric) {
  }

  public abstract void update(T meter);

  protected final Perf getperf() {
    return (Perf) AccessController.doPrivileged(new Perf.GetPerfAction());
  }

  protected final String createName(final MetricName name, final String suffix) {
    return "metric."+name.getName()+"."+suffix;
  }

  protected final ByteBuffer createByteBuffer(final MetricName name, final String suffix, final Variability variability, final Units units, final long value) {
     final Perf perf = getperf();
     final ByteBuffer buffer = perf.createLong(createName(name, suffix), variability.intValue(), units.intValue(), value);
     buffer.order(ByteOrder.nativeOrder());
     buffer.rewind();
     return buffer;
  }

  protected final void update(final ByteBuffer buffer, final long value) {
    buffer.putLong(value);
    buffer.rewind();
  }

  protected final void update(final ByteBuffer buffer, final double value) {
    buffer.putDouble(value);
    buffer.rewind();
  }

  public static class Gauge extends PerfWrapper<com.yammer.metrics.core.Gauge> {

    private final ByteBuffer buffer;

    public Gauge(final MetricName name, final com.yammer.metrics.core.Gauge gauge) {
      super(name, gauge);

      this.buffer = createByteBuffer(name, "value", Variability.MONOTONIC, Units.EVENTS, Number.class.cast(gauge.value()).longValue());//We know it's a Number
    }

    @Override
    public void update(final com.yammer.metrics.core.Gauge gauge) {
      if (gauge.value() instanceof Number) {
        update(this.buffer, Number.class.cast(gauge.value()).longValue());
      }
    }

  }

  /**
   * 
   */
  public static class Counter extends PerfWrapper<com.yammer.metrics.core.Counter> {

    private final ByteBuffer buffer;

    public Counter(final MetricName name, final com.yammer.metrics.core.Counter metric) {
      super(name, metric);

      this.buffer = createByteBuffer(name, "count", Variability.MONOTONIC, Units.EVENTS, metric.count());
    }

    @Override
    public void update(final com.yammer.metrics.core.Counter meter) {
      update(this.buffer, meter.count());
    }

  }

  public static class Timer extends PerfWrapper<com.yammer.metrics.core.Timer> {

    private final ByteBuffer countBuffer;
    private final ByteBuffer minBuffer;
    private final ByteBuffer maxBuffer;
    private final ByteBuffer meanBuffer;
    private final ByteBuffer meanRateBuffer;
    private final ByteBuffer sumBuffer;
    private final ByteBuffer stdDevBuffer;

    public Timer(final MetricName name, final com.yammer.metrics.core.Timer timer) {
      super(name, timer);

      this.countBuffer = createByteBuffer(name, "count", Variability.MONOTONIC, Units.EVENTS, timer.count());
      this.minBuffer = createByteBuffer(name, "min", Variability.VARIABLE, Units.EVENTS, timer.count());
      this.maxBuffer = createByteBuffer(name, "max", Variability.VARIABLE, Units.EVENTS, timer.count());
      this.meanBuffer = createByteBuffer(name, "mean", Variability.VARIABLE, Units.EVENTS, timer.count());
      this.meanRateBuffer = createByteBuffer(name, "mean-rate", Variability.VARIABLE, Units.EVENTS, timer.count());
      this.sumBuffer = createByteBuffer(name, "sum", Variability.VARIABLE, Units.TICKS, timer.count());
      this.stdDevBuffer = createByteBuffer(name, "stdDev", Variability.VARIABLE, Units.EVENTS, timer.count());
    }

    @Override
    public void update(final com.yammer.metrics.core.Timer timer) {
      update(this.countBuffer, timer.count());
      update(this.minBuffer, timer.min());
      update(this.maxBuffer, timer.max());
      update(this.meanBuffer, timer.mean());
      update(this.meanRateBuffer, timer.meanRate());
      update(this.sumBuffer, timer.sum());
      update(this.stdDevBuffer, timer.stdDev());
    }

  }

}