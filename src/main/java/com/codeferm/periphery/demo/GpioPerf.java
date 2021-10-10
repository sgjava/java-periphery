/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import com.codeferm.periphery.Gpio;
import static com.codeferm.periphery.Gpio.GPIO_DIR_IN;
import static com.codeferm.periphery.Gpio.GPIO_DIR_OUT;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * GPIO performance.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "gpioperf", mixinStandardHelpOptions = true, version = "gpioperf 1.0.0",
        description = "Test GPIO performance.")
public class GpioPerf implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(GpioPerf.class);
    /**
     * Device option.
     */
    @Option(names = {"-d", "--device"}, description = "GPIO device defaults to /dev/gpiochip0")
    private String device = "/dev/gpiochip0";
    /**
     * Line option.
     */
    @Option(names = {"-l", "--line"}, description = "GPIO line defaults to 203 IOG11 for NanoPi Duo")
    private int line = 203;
    /**
     * Line option.
     */
    @Option(names = {"-s", "--samples"}, description = "Samples to run defaults to 10M")
    private int samples = 10000000;

    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        var exitCode = new CommandLine(new GpioPerf()).execute(args);
        System.exit(exitCode);
    }

    /**
     * Test GPIO performance.
     *
     * @return Exit code.
     * @throws InterruptedException Possible exception.
     */
    @Override
    public Integer call() throws InterruptedException {
        var exitCode = 0;
        // Write test
        try (final var gpio = new Gpio(device, line, GPIO_DIR_OUT)) {
            var handle = gpio.getHandle();
            logger.info(String.format("Running write test with %d samples", samples));
            final var start = Instant.now();
            // Turn pin on and off, so we can see on a scope
            for (var i = 0; i < samples; i++) {
                Gpio.gpioWrite(handle, true);
                Gpio.gpioWrite(handle, false);
            }
            final var finish = Instant.now();
            // Elapsed milliseconds
            final var timeElapsed = Duration.between(start, finish).toMillis();
            logger.info(String.format("%.2f writes per second (on/off)", ((double) samples / (double) timeElapsed) * 1000));
        }
        // Read test
        try (final var gpio = new Gpio(device, line, GPIO_DIR_IN)) {
            var handle = gpio.getHandle();
            logger.info(String.format("Running read test with %d samples", samples));
            final var value = new boolean[1];
            final var start = Instant.now();
            // Read pin
            for (var i = 0; i < samples; i++) {
                Gpio.gpioRead(handle, value);
            }
            final var finish = Instant.now();
            // Elapsed milliseconds
            final var timeElapsed = Duration.between(start, finish).toMillis();
            logger.info(String.format("%.2f reads per second", ((double) samples / (double) timeElapsed) * 1000));
        }
        return exitCode;
    }
}
