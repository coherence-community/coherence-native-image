/*
 * Copyright (c) 28/05/2025, 17:57, Oracle and/or its affiliates.
 * Licensed under the Universal Permissive License v 1.0 as shown at
 * https://oss.oracle.com/licenses/upl.
 */

package com.oracle.coherence.graal;

import com.oracle.bedrock.options.Timeout;
import com.oracle.bedrock.runtime.LocalPlatform;
import com.oracle.bedrock.runtime.java.ClassPath;
import com.oracle.bedrock.runtime.java.options.ClassName;
import com.oracle.bedrock.runtime.options.Arguments;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.deferred.Eventually;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.graal.testing.NativeApplication;
import com.tangosol.net.CacheService;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;
import com.tangosol.net.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasicNamedCacheIT
    {
    @BeforeAll
    static void startCoherence() throws Exception
        {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.distributed.localstorage", "false");

        // Start Coherence in this test JVM
        coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);

        // Spawn two cluster members that will join the test JVM
        LocalPlatform platform = LocalPlatform.get();

        server1 = platform.launch(NativeApplication.class,
                        Arguments.of("-Djava.net.preferIPv4Stack=true",
                                "-Dcoherence.cluster=" + CLUSTER_NAME,
                                "-Dcoherence.localhost=127.0.0.1",
                                "-Dcoherence.wka=127.0.0.1"),
                        ClassName.of(Server.class),
                        ClassPath.automatic(),
                        DisplayName.of("server-1"),
                        testLogs);

        server2 = platform.launch(NativeApplication.class,
                        Arguments.of("-Djava.net.preferIPv4Stack=true",
                                "-Dcoherence.cluster=" + CLUSTER_NAME,
                                "-Dcoherence.localhost=127.0.0.1",
                                "-Dcoherence.wka=127.0.0.1"),
                        ClassName.of(Server.class),
                        ClassPath.automatic(),
                        DisplayName.of("server-2"),
                        testLogs);

        Cluster cluster = coherence.getCluster();
        Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3), Timeout.of(5, TimeUnit.MINUTES));

        Enumeration<String> serviceNames = cluster.getServiceNames();
        while (serviceNames.hasMoreElements())
            {
            String name = serviceNames.nextElement();
            Service service = cluster.getService(name);
            if (service instanceof PartitionedService)
                {
                Eventually.assertDeferred(() -> ((PartitionedService) service).getOwnershipEnabledMembers().size(),
                        is(2),
                        Timeout.of(5, TimeUnit.MINUTES));
                }
            }
        }

    @AfterAll
    static void shutdown()
        {
        if (server1 != null)
            {
            server1.close();
            }
        if (server2 != null)
            {
            server2.close();
            }
        Coherence.closeAll();
        }

    @AfterEach
    void removeCaches()
        {
        for (NamedCache<?, ?> cache : caches.values())
            {
            cache.destroy();
            }
        caches.clear();
        }

    @Test
    public void shouldPutIntoCache()
        {
        NamedCache<String, String> cache = getRandomCache();
        String previous = cache.put("key-1", "value-1");
        assertThat(previous, is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
        }


    <K, V> NamedCache<K, V> getRandomCache()
        {
        Session session = coherence.getSession();
        String name = "test-" + random.nextInt();
        return session.getCache(name);
        }

    @RegisterExtension
    static TestLogsExtension testLogs = new TestLogsExtension(ServerIT.class);

    /**
     * The Coherence cluster name to use.
     */
    static final String CLUSTER_NAME = "BasicNamedCacheIT";

    /**
     * The Coherence instance started in this test JVM.
     */
    static Coherence coherence;

    /**
     * A Coherence server started by Bedrock.
     */
    static NativeApplication server1;

    /**
     * A Coherence server started by Bedrock.
     */
    static NativeApplication server2;

    /**
     * A random generator to create random cache names.
     */
    final Random random = new Random();

    /**
     * A map of caches created by the {@link #getRandomCache()} method
     * so that the caches can be cleaned up between tests.
     */
    final Map<String, NamedCache<?, ?>> caches = new HashMap<>();
    }
