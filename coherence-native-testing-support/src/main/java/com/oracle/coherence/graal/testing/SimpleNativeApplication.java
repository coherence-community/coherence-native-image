/*
 * Copyright (c) 28/05/2025, 14:21, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal.testing;


import com.oracle.bedrock.OptionsByType;
import com.oracle.bedrock.runtime.AbstractApplication;
import com.oracle.bedrock.runtime.ApplicationProcess;
import com.oracle.bedrock.runtime.Platform;
import com.oracle.bedrock.runtime.SimpleApplication;

/**
 * A {@link SimpleNativeApplication} is a simple implementation
 * of a {@link NativeApplication}.
 */
public class SimpleNativeApplication
        extends AbstractApplication<ApplicationProcess>
        implements NativeApplication
    {
    /**
     * Constructs a {@link SimpleApplication}
     *
     * @param platform  the {@link Platform} on which the {@link NativeApplication} was launched
     * @param process   the underlying {@link ApplicationProcess} representing the {@link NativeApplication}
     * @param options   the {@link OptionsByType} used to launch the {@link NativeApplication}
     */
    public SimpleNativeApplication(Platform platform, ApplicationProcess process, OptionsByType options)
        {
        super(platform, process, options);
        }
    }
