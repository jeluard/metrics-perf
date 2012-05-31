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

import java.nio.ByteBuffer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;

/**
 * Helper methods for perf stuff.
 */
public final class Perfs {

  static int VARIABILITY_MONOTONIC;
  static int VARIABILITY_VARIABLE;
  static int UNITS_EVENTS;
  static Object PERF;

  static {
    try {
      VARIABILITY_MONOTONIC = loadValue("Variability", "MONOTONIC");
      VARIABILITY_VARIABLE = loadValue("Variability", "VARIABLE");
      UNITS_EVENTS = loadValue("Units", "EVENTS");
      PERF = AccessController.doPrivileged((PrivilegedAction) Class.forName("sun.misc.Perf$GetPerfAction").newInstance());
    } catch (Throwable t) {
      if (PerfReporter.LOGGER.isLoggable(Level.FINE)) {
        PerfReporter.LOGGER.log(Level.FINE, "Failed to initialize perf internals.", t);
      }
      t.printStackTrace();
    }
  }

  private static int loadValue(final String type, final String field) throws Exception {
    final Object object = Class.forName("sun.management.counter."+type).getField(field).get(null);
    return (Integer) object.getClass().getMethod("intValue").invoke(object);
  }

  private Perfs() {
  }

  public static boolean isPlatformSupported() {
    return Perfs.PERF != null;
  }

  public static ByteBuffer createBuffer(final String name, final int variability, final int units, final long value) {
    try {
      return (ByteBuffer) Perfs.PERF.getClass().getMethod("createLong", String.class, int.class, int.class, long.class).invoke(Perfs.PERF, name, variability, units, value);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}