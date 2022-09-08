/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */
package org.opensearch.sdk.sample.helloworld;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExecuteCommandLine {

    private static final String[] DEFAULT_ENV = getDefaultEnv();
    private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("windows");

    private ExecuteCommandLine() {}

    private static String[] getDefaultEnv() {
        if (IS_WINDOWS) {
            return new String[] { "LANGUAGE=C" };
        } else {
            return new String[] { "LC_ALL=C" };
        }
    }

    /**
     * Executes a command on the native command line and returns the result. This is
     * a convenience method to call {@link java.lang.Runtime#exec(String)} and
     * capture the resulting output in a list of Strings. On Windows, built-in
     * commands not associated with an executable program may require
     * {@code cmd.exe /c} to be prepended to the command.
     *
     * @param cmdToRun
     *            Command to run
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> exec(String cmdToRun) {
        String[] cmd = cmdToRun.split(" ");
        return exec(cmd);
    }

    /**
     * Executes a command on the native command line and returns the result line by
     * line. This is a convenience method to call
     * {@link java.lang.Runtime#exec(String[])} and capture the resulting output in
     * a list of Strings. On Windows, built-in commands not associated with an
     * executable program may require the strings {@code cmd.exe} and {@code /c} to
     * be prepended to the array.
     *
     * @param cmdToRunWithArgs
     *            Command to run and args, in an array
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> exec(String[] cmdToRunWithArgs) {
        return exec(cmdToRunWithArgs, DEFAULT_ENV);
    }

    /**
     * Executes a command on the native command line and returns the result line by
     * line. This is a convenience method to call
     * {@link java.lang.Runtime#exec(String[])} and capture the resulting output in
     * a list of Strings. On Windows, built-in commands not associated with an
     * executable program may require the strings {@code cmd.exe} and {@code /c} to
     * be prepended to the array.
     *
     * @param cmdToRunWithArgs
     *            Command to run and args, in an array
     * @param envp
     *            array of strings, each element of which has environment variable
     *            settings in the format name=value, or null if the subprocess
     *            should inherit the environment of the current process.
     * @return A list of Strings representing the result of the command, or empty
     *         string if the command failed
     */
    public static List<String> exec(String[] cmdToRunWithArgs, String[] envp) {
        Process p = null;
        try {
            p = Runtime.getRuntime().exec(cmdToRunWithArgs, envp);
            return getProcessOutput(p, cmdToRunWithArgs);
        } catch (SecurityException | IOException e) {
            // TODO handle
        } finally {
            // Ensure all resources are released
            if (p != null) {
                // Windows doesn't close descriptors on destroy, so we must handle separately
                if (IS_WINDOWS) {
                    try {
                        p.getOutputStream().close();
                    } catch (IOException e) {
                        // do nothing on failure
                    }
                    try {
                        p.getInputStream().close();
                    } catch (IOException e) {
                        // do nothing on failure
                    }
                    try {
                        p.getErrorStream().close();
                    } catch (IOException e) {
                        // do nothing on failure
                    }
                }
                p.destroy();
            }
        }
        return Collections.emptyList();
    }

    private static List<String> getProcessOutput(Process p, String[] cmd) {
        ArrayList<String> sa = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sa.add(line);
            }
            p.waitFor();
        } catch (IOException e) {
            // TODO handle
        } catch (InterruptedException ie) {
            // TODO handle
            Thread.currentThread().interrupt();
        }
        return sa;
    }
}
