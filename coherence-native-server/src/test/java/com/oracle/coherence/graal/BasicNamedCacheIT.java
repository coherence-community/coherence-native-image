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
import com.oracle.coherence.graal.model.java.Country;

import com.oracle.coherence.graal.model.pof.Address;
import com.oracle.coherence.graal.model.pof.Customer;
import com.oracle.coherence.graal.testing.NativeApplication;

import com.tangosol.internal.util.invoke.Lambdas;
import com.tangosol.net.Cluster;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedCache;
import com.tangosol.net.PartitionedService;
import com.tangosol.net.Service;
import com.tangosol.net.Session;

import com.tangosol.util.Aggregators;
import com.tangosol.util.Extractors;
import com.tangosol.util.Filters;
import com.tangosol.util.MapListener;
import com.tangosol.util.Processors;
import com.tangosol.util.ValueExtractor;
import com.tangosol.util.ValueUpdater;
import com.tangosol.util.aggregator.DoubleSum;
import com.tangosol.util.aggregator.GroupAggregator;
import com.tangosol.util.function.Remote;
import com.tangosol.util.listener.SimpleMapListener;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class BasicNamedCacheIT {

    private static final String GOLD = "GOLD";
    private static final String SILVER = "SILVER";
    private static final String BRONZE = "BRONZE";

    private static final String[] TYPES = new String[]{GOLD, SILVER, BRONZE};

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

    @BeforeAll
    static void startCoherence() throws Exception {
        String lambdas = "static";
//        String lambdas = "dynamic";

        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.lambdas", lambdas);

        // Start Coherence in this test JVM
        coherence = Coherence.clusterMember().start().get(5, TimeUnit.MINUTES);

        // Spawn two cluster members that will join the test JVM
        LocalPlatform platform = LocalPlatform.get();

        ClassPath cp = ClassPath.automatic().excluding(ClassPath.ofClass(BasicNamedCacheIT.class));

        server1 = platform.launch(NativeApplication.class,
                Arguments.of("-Djava.net.preferIPv4Stack=true",
                        "-Dcoherence.lambdas=" + lambdas,
                        "-Dcoherence.cluster=" + CLUSTER_NAME,
                        "-Dcoherence.localhost=127.0.0.1",
                        "-Dcoherence.wka=127.0.0.1"),
                ClassName.of(Server.class),
                cp,
                DisplayName.of("server-1"),
                testLogs);

        server2 = platform.launch(NativeApplication.class,
                Arguments.of("-Djava.net.preferIPv4Stack=true",
                        "-Dcoherence.lambdas=" + lambdas,
                        "-Dcoherence.cluster=" + CLUSTER_NAME,
                        "-Dcoherence.localhost=127.0.0.1",
                        "-Dcoherence.wka=127.0.0.1"),
                ClassName.of(Server.class),
                cp,
                DisplayName.of("server-2"),
                testLogs);

        Cluster cluster = coherence.getCluster();
        Eventually.assertDeferred(() -> cluster.getMemberSet().size(), is(3), Timeout.of(5, TimeUnit.MINUTES));

        Enumeration<String> serviceNames = cluster.getServiceNames();
        while (serviceNames.hasMoreElements()) {
            String name = serviceNames.nextElement();
            Service service = cluster.getService(name);
            if (service instanceof PartitionedService) {
                Eventually.assertDeferred(() -> ((PartitionedService) service).getOwnershipEnabledMembers().size(),
                        is(2),
                        Timeout.of(5, TimeUnit.MINUTES));
            }
        }
    }

    @AfterAll
    static void shutdown() {
        if (server1 != null) {
            server1.close();
        }
        if (server2 != null) {
            server2.close();
        }
        Coherence.closeAll();
    }

    @AfterEach
    void removeCaches() {
        for (NamedCache<?, ?> cache : caches.values()) {
            cache.destroy();
        }
        caches.clear();
    }

    @Test
    public void shouldPutIntoCache() {
        NamedCache<String, String> cache = getRandomCache();
        String previous = cache.put("key-1", "value-1");
        assertThat(previous, is(nullValue()));
        assertThat(cache.get("key-1"), is("value-1"));
    }

    @Test
    public void shouldUseRecords() {
        final int countryCount = 200;
        NamedCache<Integer, Country> countries = getRandomCache();
        for (int i = 1; i <= countryCount; i++) {
            countries.put(i, new Country("C" + i, "Country-" + i, random.nextInt(10_000, 1_000_000)));
        }
        assertThat(countries.size(), is(countryCount));
    }

    @Test
    public void shouldUsePOF() {
        final int maxCustomers = 10_000;

        Map<Integer, Customer> buffer = new HashMap<>();
        NamedCache<Integer, Customer> customers = coherence.getSession().getCache("pof-customers");
        customers.addIndex(Customer::getCustomerType);

        // add map listener
        final AtomicInteger insertCounter = new AtomicInteger();

        MapListener<Integer, Customer> listener = new SimpleMapListener<Integer, Customer>()
                .addInsertHandler((e-> insertCounter.incrementAndGet()));
        customers.addMapListener(listener);

        for (int i = 1; i <= maxCustomers; i++) {
            buffer.put(i, new Customer(i, "name-" + i, random.nextDouble(), getRandomAddress(), getRandomAddress(), getRandomCustomerType()));
            if (i % 100 == 0) {
                customers.putAll(buffer);
                buffer.clear();
            }
        }
        if (!buffer.isEmpty()) {
            customers.putAll(buffer);
        }

        assertThat(customers.size(), is(maxCustomers));

        Eventually.assertDeferred(insertCounter::get, is(maxCustomers), Timeout.of(1, TimeUnit.MINUTES));

        // aggregate
        // These do not work with static lambdas:
        // Aggregators.sum(Customer::getBalance)
        DoubleSum<Customer> aggregator = new DoubleSum<>(ValueExtractor.of(Customer::getBalance));
        Map<String, Double> balanceByType = customers.aggregate(
                GroupAggregator.createInstance(ValueExtractor.of(Customer::getCustomerType), aggregator));
        assertThat(balanceByType.isEmpty(), is(false));

        // entry processor to update all balances for GOLD customers to 0
        // This does not work with static lambdas:
//        customers.invokeAll(Filters.equal(ValueExtractor.of(Customer::getCustomerType), GOLD), Processors.update(Customer::setBalance, 0.0d));

        // return values
        assertThat(customers.values(Filters.equal(ValueExtractor.of(Customer::getCustomerType), GOLD)).size(), not(0));

        // stream
        assertThat(customers.stream(Filters.equal(ValueExtractor.of(Customer::getCustomerType), SILVER)).findFirst(), is(notNullValue()));
    }

    private Address getRandomAddress() {
        return new Address("Address line 1", "address line 2", "city-" + random.nextInt(),
                "state-" + random.nextInt(), "zip" + random.nextInt(), "country-" + random.nextInt());
    }

    private String getRandomCustomerType() {
        return TYPES[random.nextInt(TYPES.length)];
    }

    <K, V> NamedCache<K, V> getRandomCache() {
        Session session = coherence.getSession();
        String name = "test-" + random.nextInt();
        NamedCache<K, V> cache = session.getCache(name);
        caches.put(name, cache);
        return cache;
    }
}
