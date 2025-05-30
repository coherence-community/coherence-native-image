/*
 * Copyright (c) 28/05/2025, 14:17, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal.testing;

import com.oracle.bedrock.Option;
import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.Application;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.options.ApplicationClosingBehavior;
import com.oracle.bedrock.runtime.options.Argument;
import com.oracle.bedrock.runtime.options.Executable;
import com.oracle.bedrock.testsupport.MavenProjectFileUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A platform and location independent mechanism to represent, access and
 * control a running GraalVM native application.
 */
public interface NativeApplication
        extends Application {

    static <A extends NativeApplication> Predicate<A> withName(String name) {
        String regex = Pattern.quote(name);
        Pattern pattern = Pattern.compile(regex);
        return a -> pattern.matcher(a.getName()).matches();
    }

    static <A extends NativeApplication> Predicate<A> withNameMatching(String regex) {
        Pattern pattern = Pattern.compile(regex);
        return a -> pattern.matcher(a.getName()).matches();
    }

    static <A extends NativeApplication> Predicate<A> withNamePrefix(String name) {
        String regex = Pattern.quote(name);
        Pattern pattern = Pattern.compile(regex);
        return a -> pattern.matcher(a.getName()).lookingAt();
    }

    static <A extends NativeApplication> Predicate<A> withNameContaining(String name) {
        String regex = Pattern.quote(name);
        Pattern pattern = Pattern.compile(regex);
        return a -> pattern.matcher(a.getName()).find();
    }

    /**
     * Obtains the name of the {@link NativeApplication}.
     *
     * @return The name of the {@link NativeApplication}
     */
    String getName();

    /**
     * Obtains the {@link Platform} that this {@link NativeApplication}
     * is running on.
     *
     * @return the {@link Platform} that this {@link NativeApplication}
     * is running on
     */
    Platform getPlatform();

    /**
     * Attempts to close and terminate the running {@link NativeApplication}.
     * <p>
     * Upon returning it is safe to assume that the {@link NativeApplication}
     * is no longer running.   All resources, including input and output streams
     * used by the {@link NativeApplication} are no longer available to be used.
     * <p>
     * To determine the exit value of the terminated application use {@link #exitValue()}.
     */
    @Override
    void close();

    /**
     * Attempts to close and terminate the running {@link NativeApplication} using the
     * specified {@link Option}s.  When no {@link Option}s are specified
     * a regular {@link #close} is performed.
     * <p>
     * Upon returning it is safe to assume that the {@link NativeApplication}
     * is no longer running.   All resources, including input and output streams
     * used by the {@link NativeApplication} are no longer available to be used.
     * <p>
     * To determine the exit value of the terminated application use {@link #exitValue()}.
     *
     * @param options the {@link Option}s indicating how to close the application.
     * @see ApplicationClosingBehavior
     */
    void close(Option... options);

    /**
     * Determines if the {@link NativeApplication} is operational, has not been closed
     * and closing has not been commenced.
     *
     * @return <code>true</code>  if operational,
     * <code>false</code> if the {@link NativeApplication} has been closed or
     * has commenced closing
     */
    boolean isOperational();

    /**
     * Causes the calling {@link Thread} to block and wait until the
     * {@link NativeApplication} has terminated, either naturally or due to a call
     * to {@link #close()} by another {@link Thread}.
     * <p>
     * This method returns immediately if the {@link NativeApplication} has already
     * been terminated.
     *
     * @param options the {@link Option}s for waiting, including possible
     *                {@link Timeout}
     * @return the exit value of the {@link NativeApplication}
     * (by convention <code>0</code> indicates normal termination)
     * @throws RuntimeException if it wasn't possible to wait for the
     *                          termination.
     */
    int waitFor(Option... options);

    /**
     * Obtains the exit value for the terminated {@link NativeApplication}.
     *
     * @return the exit value of the {@link NativeApplication}
     * @throws IllegalThreadStateException if the {@link NativeApplication} has not
     *                                     yet terminated
     */
    int exitValue();

    /**
     * Obtain the identity for the {@link NativeApplication}.  This is typically
     * the underlying process id (pid), but in some circumstances this may be
     * an environment specific identifier.  eg: in a cloud/container/multi-tenant
     * environment this may not be an operating system pid.
     *
     * @return The unique identity or -1 if it can't be determined.
     */
    long getId();

    /**
     * Obtains the default duration to used by the {@link NativeApplication}
     * for timeouts.
     *
     * @return a {@link Timeout}
     */
    Timeout getDefaultTimeout();

    /**
     * Obtains the configured {@link OptionsByType} for the {@link NativeApplication}.
     * <p>
     * <strong>Changes to the {@link OptionsByType} may not be recognized
     * or used by the {@link NativeApplication} after it was realized.</strong>
     *
     * @return the {@link OptionsByType}
     */
    OptionsByType getOptions();

    /**
     * The {@link com.oracle.bedrock.runtime.MetaClass} for generic {@link NativeApplication}s.
     */
    class MetaClass implements com.oracle.bedrock.runtime.MetaClass<NativeApplication> {
        private static final Pattern VERSION_PATTERN = Pattern.compile("(.*)-\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?(\\.jar)$");

        /**
         * Constructs a {@link MetaClass} for a {@link NativeApplication}.
         */
        @OptionsByType.Default
        public MetaClass() {
        }

        @Override
        public Class<? extends NativeApplication> getImplementationClass(Platform platform, OptionsByType options) {
            return SimpleNativeApplication.class;
        }

        @Override
        public void onLaunching(Platform platform, OptionsByType options) {
            boolean isNativeTest = Boolean.getBoolean("coherence.native.tests");
            String nativeImage = System.getProperty("coherence.native.image");
            ClassName className = options.get(ClassName.class);

            if (isNativeTest) {
                if (nativeImage == null || nativeImage.isBlank()) {
                    // no native image set, so try to work it out from the class name
                    Class<?> clz;
                    try {
                        clz = Class.forName(className.getName());

                        File buildFolder = MavenProjectFileUtils.locateBuildFolder(clz);

                        if (buildFolder == null) {
                            throw new IllegalStateException("Unable to locate build folder for " + clz);
                        }

                        if (!buildFolder.exists()) {
                            throw new IllegalStateException("Non-existent build folder for " + clz);
                        }

                        if (!buildFolder.isDirectory()) {
                            throw new IllegalStateException("Build folder " + buildFolder + " is not a directory");
                        }


                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(buildFolder.toPath())) {
                            for (Path path : stream) {
                                File file = path.toFile();
                                String fileName = file.getAbsolutePath();
                                if (fileName.endsWith(".jar")) {
                                    String stripped = stripVersion(fileName);
                                    File imageFile = new File(stripped);
                                    if (imageFile.exists() && imageFile.isFile()) {
                                        nativeImage = imageFile.getAbsolutePath();
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                    if (nativeImage == null) {
                        throw new RuntimeException("Could not find native image from class  " + clz);
                    }
                }

                options.add(Executable.named(nativeImage));
                options.remove(ClassName.class);
                options.remove(ClassPath.class);
            } else {
                options.add(Executable.named("java"));

                ClassPath cp = options.get(ClassPath.class);
                if (cp != null) {
                    String path = Arrays.stream(cp.getURLs())
                            .map(URL::getPath)
                            .collect(Collectors.joining(File.pathSeparator));
                    if (!path.isEmpty()) {
                        options.add(Argument.of("-cp", path));
                    }
                }

                if (className != null) {
                    options.add(Argument.of(className.getName()));
                }
            }
        }

        @Override
        public void onLaunch(Platform platform, OptionsByType options) {
        }

        @Override
        public void onLaunched(Platform platform, NativeApplication application, OptionsByType options) {
        }

        private String stripVersion(String jar) {
            Matcher matcher = VERSION_PATTERN.matcher(jar);
            if (matcher.matches()) {
                return matcher.group(1);
            }
            throw new IllegalArgumentException("Cannot strip version from jar name " + jar);
        }

    }
}
