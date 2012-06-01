`metrics-perf` is a tiny extension to yammer's [metrics](https://github.com/codahale/metrics) library exporting all metrics as Hotspot [perf counters](http://openjdk.java.net/groups/hotspot/docs/Serviceability.html#bjvmstat).

You can safely use this on a non-Hotspot VM as all Hotspot specific calls are done via reflection (i.e. no static import) and only executed if a compliant VM is detected.

## Usage

To expert all your metrics simply add this code:

```java
final PerfReporter reporter = new PerfReporter(Metrics.defaultRegistry());
reporter.start(1, TimeUnit.SECONDS);
```
All perf counters values will then be updated every seconds.

### Maven dependency

Simply add following to your pom.xml:

```xml
...
  <dependency>
    <groupId>com.github.jeluard</groupId>
    <artifactId>metrics-perf</artifactId>
    <version>0.9</version>
  </dependency>
...
```

## Visualizing

Perf counters can be visualized by mainly 2 tools: [VisualVM](http://visualvm.java.net/) and [jstat](http://docs.oracle.com/javase/6/docs/technotes/tools/share/jstat.html).

### Using VisualVM

VisualVM can visualize perf counters via the tracer [plugin](http://visualvm.java.net/plugins.html).

![Tracer plugin](http://visualvm.java.net/images/tracer.jpg)

### Using JStat

[jstat](http://docs.oracle.com/javase/6/docs/technotes/tools/share/jstat.html) is a command line tool allowing to track perf counter values.

List all perf counters:

```
jstat -J-Djstat.showUnsupported=true -list <vmid>
```

List all exported metrics:

```
jstat -J-Djstat.showUnsupported=true -snap -name metric.\* <vmid>
```

Print current value of a particular metric:

```
jstat -J-Djstat.showUnsupported=true -snap -name metric.metric-name.15min-rate <vmid>
```

Sample value of a specific metric every 1 second:

```
jstat -J-Djstat.showUnsupported=true  -name metric.metric-name.15min-rate <vmid> 1s
```

You can find all jstat options (including non-documented) by browsing its [source code](http://www.opensourcejavaphp.net/java/openjdk/sun/tools/jstat/Arguments.java.html).
