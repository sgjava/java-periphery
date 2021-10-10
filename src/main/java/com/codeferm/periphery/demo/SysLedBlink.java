/*
 * Copyright (c) Steven P. Goldsmith. All rights reserved.
 */
package com.codeferm.periphery.demo;

import com.codeferm.periphery.Led;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * LED blink using Linux userspace sysfs.
 *
 * @author Steven P. Goldsmith
 * @version 1.0.0
 * @since 1.0.0
 */
@Command(name = "sysledblink", mixinStandardHelpOptions = true, version = "sysledblink 1.0.0",
        description = "Turn LED on and off.")
public class SysLedBlink implements Callable<Integer> {

    /**
     * Logger.
     */
    private final org.apache.logging.log4j.Logger logger = LogManager.getLogger(SysLedBlink.class);
    /**
     * Device option.
     */
    @Option(names = {"-n", "--name"}, description = "System LED defaults to nanopi:green:pwr")
    private String name = "nanopi:green:pwr";

    /**
     * Blink system LED.
     *
     * @return Exit code.
     * @throws InterruptedException Possible exception.
     */
    @Override
    public Integer call() throws InterruptedException {
        var exitCode = 0;
        try (final var led = new Led(name)) {
            var value = new boolean[1];
            // Get current value
            Led.ledRead(led.getHandle(), value);
            logger.info("Blinking LED");
            var i = 0;
            while (i < 10) {
                Led.ledWrite(led.getHandle(), true);
                TimeUnit.SECONDS.sleep(1);
                Led.ledWrite(led.getHandle(), false);
                TimeUnit.SECONDS.sleep(1);
                i++;
            }
            // Restore led value
            Led.ledWrite(led.getHandle(), value[0]);
        } catch (RuntimeException e) {
            logger.error(e.getMessage());
            exitCode = 1;
        }
        return exitCode;
    }
    
    /**
     * Main parsing, error handling and handling user requests for usage help or version help are done with one line of code.
     *
     * @param args Argument list.
     */
    public static void main(String... args) {
        System.exit(new CommandLine(new SysLedBlink()).execute(args));
    }    
}
