/*
 * Copyright (c) 28/05/2025, 13:40, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal.model.java;

import java.io.Serializable;

public record Country(String countryCode, String name, long population) implements Serializable {
}
