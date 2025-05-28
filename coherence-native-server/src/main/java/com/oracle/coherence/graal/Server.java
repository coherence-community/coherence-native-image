/*
 * Copyright (c) 28/05/2025, 13:40, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal;

import com.tangosol.net.Coherence;

/**
 * A simple wrapper around {@link Coherence} that starts
 * a Coherence server.
 */
public class Server
    {
    public static void main(String[] args)
        {
        Coherence.main(args);
        }
    }
