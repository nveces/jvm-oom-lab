package tech.nveces.jvmoom;

import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class JvmOomMain {

  private static final String HELP_FLAG = "--help";
  private static final String LIST_FLAG = "--list";
  private static final ScenarioType DEFAULT_SCENARIO = ScenarioType.HEAP_SPACE;

  private static final Map<ScenarioType, Scenario> SCENARIOS = buildScenarios();

  public static void main(String[] args) {
    String inputScenario = (args == null || args.length == 0) ? DEFAULT_SCENARIO.cliName() : args[0];

    if (args != null && args.length > 0 && HELP_FLAG.equalsIgnoreCase(args[0])) {
      printHelp();
      return;
    }

    if (args != null && args.length > 0 && LIST_FLAG.equalsIgnoreCase(args[0])) {
      printScenarioList();
      return;
    }

    ScenarioType scenarioType = ScenarioType.fromCliName(inputScenario);

    if (scenarioType == null) {
      System.err.printf("[ERROR] Unknown scenario: '%s'%n%n", inputScenario);
      printScenarioList();
      System.exit(2);
      return;
    }

    Scenario scenario = SCENARIOS.get(scenarioType);

    System.out.printf("[INFO] Running OOM scenario: %s%n", scenario.name());
    System.out.printf("[INFO] Description: %s%n", scenario.description());

    try {
      scenario.run();
      System.out.println("[WARN] The scenario finished without OutOfMemoryError.");
    } catch (OutOfMemoryError oom) {
      // If we use -XX:+ExitOnOutOfMemoryError, the JVM will exit immediately on OOM
      // without printing the stack trace or message.
      System.err.printf("%n[OOM CAPTURED] Scenario: %s%n", scenario.name());
      System.err.printf("[OOM CAPTURED] Message: %s%n", Objects.toString(oom.getMessage(), "<no message>"));
      System.err.println("[OOM CAPTURED] Hint: tune JVM flags to speed up/reproduce this case.");
      System.exit(1);
    }
  }

  private static Map<ScenarioType, Scenario> buildScenarios() {
    Map<ScenarioType, Scenario> map = new EnumMap<>(ScenarioType.class);
    register(map, new Scenario(ScenarioType.HEAP_SPACE, JvmOomMain::heapSpace));
    register(map, new Scenario(ScenarioType.GC_OVERHEAD, JvmOomMain::gcOverhead));
    register(map, new Scenario(ScenarioType.METASPACE, JvmOomMain::metaspace));
    register(map, new Scenario(ScenarioType.DIRECT_BUFFER, JvmOomMain::directBuffer));
    register(map, new Scenario(ScenarioType.UNABLE_CREATE_THREAD, JvmOomMain::unableCreateThread));
    register(map, new Scenario(ScenarioType.REQUESTED_ARRAY_SIZE, JvmOomMain::requestedArraySize));
    return Collections.unmodifiableMap(map);
  }

  private static void register(Map<ScenarioType, Scenario> map, Scenario scenario) {
    map.put(scenario.type(), scenario);
  }

  private static void printHelp() {
    System.out.println("Use: java -jar jvm-oom-lab.jar [scenario]");
    System.out.println("     mvn exec:java -Dexec.args=\"[scenario]\"");
    System.out.printf("Default scenario: %s%n", DEFAULT_SCENARIO.cliName());
    System.out.println();
    printScenarioList();
    System.out.println();
    System.out.println("Options:");
    System.out.println("  --help   Show this help");
    System.out.println("  --list   List available scenarios");
  }

  private static void printScenarioList() {
    String scenarios = Arrays.stream(ScenarioType.values())
        .sorted(Comparator.comparing(ScenarioType::cliName))
        .map(type -> String.format("  - %s: %s", type.cliName(), type.description()))
        .collect(Collectors.joining(System.lineSeparator()));

    System.out.println("Available scenarios:");
    System.out.println(scenarios);
  }

  private static void heapSpace() {
    List<byte[]> holderleak = new ArrayList<>();
    while (true) {
      byte[] block = new byte[1024 * 1024];
      Arrays.fill(block, (byte) 1);
      holderleak.add(block);
    }
  }

  private static void gcOverhead() {
    System.out.println("This scenario may take a long time to trigger the OOM, as it relies on GC overhead limit.");
    List<String> holder = new ArrayList<>();
    long counter = 0;
    while (true) {
      holder.add("x" + counter++);
      if (holder.size() > 100_000) {
        holder.subList(0, 99_999).clear();
        if (counter % 5_000 == 0) {
          System.out.println("GC Overhead. Counter: " + counter / 1024 + " MB");
        }
      }
    }
    // System.out.println("This scenario may take a long time to trigger the OOM, as
    // it relies on GC overhead limit.");
  }

  private static void metaspace() {
    List<ClassLoader> holders = new ArrayList<>();
    while (true) {
      ClassLoader loader = new ClassLoader(JvmOomMain.class.getClassLoader()) {
      };
      Proxy.newProxyInstance(loader, new Class<?>[] { Runnable.class }, (proxy, method, args) -> null);
      holders.add(loader);
    }
  }

  private static void directBuffer() {
    List<ByteBuffer> holder = new ArrayList<>();
    int count = 0;
    while (true) {
      holder.add(ByteBuffer.allocateDirect(1024 * 1024));
      count++;
      if (count % 100 == 0) {
        System.out.println("Memory direct assigned: " + count + " MB");
      }
    }
  }

  private static void unableCreateThread() {
    List<Thread> threads = new ArrayList<>();
    int count = 0;
    while (true) {
      Thread t = new Thread(() -> {
        try {
          while (true) {
            Thread.sleep(Long.MAX_VALUE);
          }
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
        }
      });
      t.setDaemon(true);
      t.start();
      threads.add(t);
      count++;
      if (count % 500 == 0) {
        System.out.println("Threads created: " + count);
      }
    }
  }

  private static void requestedArraySize() {
    int[] impossible = new int[Integer.MAX_VALUE];
    System.out.println(impossible.length);
  }

  @FunctionalInterface
  private interface ScenarioAction {
    void execute();
  }

  // Using record for simplicity, as it is a simple data holder with behavior.
  private record Scenario(ScenarioType type, ScenarioAction action) {
    String name() {
      return type.cliName();
    }

    String description() {
      return type.description();
    }

    void run() {
      action.execute();
    }
  }

  private enum ScenarioType {
    HEAP_SPACE("heap-space", "java.lang.OutOfMemoryError: Java heap space"),
    GC_OVERHEAD("gc-overhead", "java.lang.OutOfMemoryError: GC overhead limit exceeded"),
    METASPACE("metaspace", "java.lang.OutOfMemoryError: Metaspace"),
    DIRECT_BUFFER("direct-buffer", "java.lang.OutOfMemoryError: Direct buffer memory"),
    UNABLE_CREATE_THREAD("unable-create-thread", "java.lang.OutOfMemoryError: unable to create new native thread"),
    REQUESTED_ARRAY_SIZE("requested-array-size", "java.lang.OutOfMemoryError: Requested array size exceeds VM limit");

    private final String cliName;
    private final String description;

    ScenarioType(String cliName, String description) {
      this.cliName = cliName;
      this.description = description;
    }

    String cliName() {
      return cliName;
    }

    String description() {
      return description;
    }

    static ScenarioType fromCliName(String value) {
      if (value == null) {
        return null;
      }

      String normalized = value.toLowerCase(Locale.ROOT).trim();
      for (ScenarioType scenarioType : values()) {
        if (scenarioType.cliName.equals(normalized)) {
          return scenarioType;
        }
      }

      return null;
    }
  }
}