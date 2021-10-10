/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_DIR_IN;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_BOTH;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_FALLING;
import static com.codeferm.periphery.Gpio.GPIO_EDGE_RISING;
import static com.codeferm.periphery.Gpio.GPIO_POLL_EVENT;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Blocking event using button. A thread is used, so other processing can occur.
 *
 * Should work on any board with a button built in or wire one up. Just change device and line arguments as needed.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "buttonthread", mixinStandardHelpOptions = true, version = "buttonthread 1.0.0",
        description = "Uses edge detection to wait for button press while other processing occurs.")
public class ButtonThread implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(ButtonThread.class);
    /**
     * Device option.
     */
    @Option(names = {"-d", "--device"}, description = "GPIO device defaults to /dev/gpiochip1")
    private String device = "/dev/gpiochip1";
    /**
     * Line option.
     */
    @Option(names = {"-l", "--line"}, description = "GPIO line defaults to 3 for NanoPi Duo built-in button")
    private int line = 3;

    /**
     * Wait for edge thread.
     *
     * @param executor Executor service.
     */
    public void submitWaitForEdge(final ExecutorService executor) {
        // Submit lambda
        executor.submit(() -> {
            try (final var gpio = new Gpio(device, line, GPIO_DIR_IN)) {
                final var formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
                final var edge = new int[1];
                final var timestamp = new long[1];
                Gpio.gpioSetEdge(gpio.getHandle(), GPIO_EDGE_BOTH);
                logger.info("Press button, stop pressing button for 10 seconds to exit");
                // Poll for event and timeout in 10 seconds if no event
                while (Gpio.gpioPoll(gpio.getHandle(), 10000) == GPIO_POLL_EVENT) {
                    Gpio.gpioReadEvent(gpio.getHandle(), edge, timestamp);
                    final var date = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp[0] / 1000000), ZoneId.systemDefault());
                    if (edge[0] == GPIO_EDGE_RISING) {
                        logger.info(String.format("Edge rising, %s", date.format(formatter)));
                    } else if (edge[0] == GPIO_EDGE_FALLING) {
                        logger.info(String.format("Edge falling %s", date.format(formatter)));
                    } else {
                        logger.info(String.format("Invalid edge %d, %s", edge[0], date.format(formatter)));
                    }
                }
            }
        });
    }

    /**
     * Use blocking edge detection.
     *
     * @return Exit code.
     */
    @Override
    public Integer call() {
        var exitCode = 0;
        final var executor = Executors.newSingleThreadExecutor();
        submitWaitForEdge(executor);
        try {
            // Initiate shutdown
            executor.shutdown();
            int count = 0;
            while (count <= 30 && !executor.isTerminated()) {
                logger.info("Main program doing stuff, press button");
                TimeUnit.SECONDS.sleep(1);
                count++;
            }
            // Wait for thread to finish
            if (!executor.isTerminated()) {
                logger.info("Waiting for thread to finish");
                executor.awaitTermination(Long.MAX_VALUE, NANOSECONDS);
            }
        } catch (InterruptedException e) {
            logger.error("Tasks interrupted");
        } finally {
            executor.shutdownNow();
        }
        return exitCode;
    }
    
    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        System.exit(new CommandLine(new ButtonThread()).execute(args));
    }
}
