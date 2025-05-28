# Coherence GraalVM Native Image Tests

This project is for testing various [Coherence](https://github.com/oracle/coherence) features running as a [GraalVM native image](https://www.graalvm.org)

## Build

To build the project use the following Maven command
```
mvn clean package -DskipTests
```

To build the project including the native image the JVM must be GraalVM.
Use the following Maven command that enables the `native` profile
```
mvn clean package -DskipTests -Pnative
```

## Testing

It is possible to run the tests in two modes, plain Java or using the native image.

The tests have been written using JUnit 5 and the Coherence Bedrock test framework.
The project contains some Bedrock customizations to allow processes that Bedrock spawns to run 
a normal Java command or a native image based on a simple System property.


To run the tests using Java, use the following command:
```
mvn clean verify
```

To run the tests using the native image, enable both the `native` and `native-test` profiles.
Use the following command:
```
mvn clean verify -Pnative,native-test
```
       
### IDE Testing

It is possible to run the tests inside an IDE as they are all simple JUnit tests.
By default, the tests will run a normal Java commands when they spawn other Coherence processes.
This can be changed by setting the system property `-Dcoherence.native.tests=true`. 
When running native image tests the native image build must have been run first so that the test can locate the 
native image to execute.

### How Do The Tests Work
         
The tests use the Coherence Bedrock test framework to spawn other processes as part of the test.
This project contains some Bedrock customizations in the [coherence-native-testing-support](coherence-native-testing-support)
module that allow switching between Java and native image testing with just a system property, or Maven profile.
                                                                          
The code below shows one of the tests that simply starts a Coherence cache server and waits for the 
"Started Coherence server" message to appear in the logs.

```java
    @Test
    void shouldStartSimpleClusterMember() throws Exception
        {
        CountDownLatch latch = new CountDownLatch(1);
        ApplicationConsole console = testLogs.builder()
                .addStdErrListener(s -> s.contains("Started Coherence server"), s -> latch.countDown())
                .build("server");

        try (NativeApplication server = LocalPlatform.get().launch(NativeApplication.class,
                Arguments.of("-Djava.net.preferIPv4Stack=true",
                        "-Dcoherence.cluster=native-image-test-1",
                        "-Dcoherence.localhost=127.0.0.1",
                        "-Dcoherence.wka=127.0.0.1"),
                ClassName.of(Server.class),
                ClassPath.automatic(),
                DisplayName.of("server"),
                Console.of(console)))
            {
            boolean awaitMessage = latch.await(1, TimeUnit.MINUTES);
            assertThat(awaitMessage, is(true));
            }
        }
```

Bedrock captures the console output from the processes that it spawns using an `ApplicationConsole`. 
In this case the logs are captured to files under the `target/test-output` folder.
Listeners can be added to the console to listen for log lines containing certain text values, in the test above we listen for the "Started Coherence server" message and when it is seen call a `CountdownLatch`  

The try with resources block is where Bedrock actually starts the Coherence server. The `LocalPlatform.get().launch` method
will spawn a process on the local machine.

The values passed to `Arguments.of()` are used as command line arguments used to start the process.

The `ClassName.of(Server.class)` tells Bedrock the Java main class to execute. 
If running with `-Dcoherence.native.tests=true` the class name is used to locate the native image to execute.
Bedrock understands the Maven project directory structure. It locates the class and then finds its parent Maven build folder.
Bedrock will look in this folder for the artifact jar file and then strip the version from the jar file to give the native image name. This could be fragile if using other names for the native image but in our simple test cases this should always work fine.

The `ClassPath.automatic()` tells Bedrock how to configure the Java class path for the command.
If running with `-Dcoherence.native.tests=true` the class path will be ignored.

When running in the default mode as a normal Java server, the parameters passed to `LocalPlatform.get().launch()` will be used to create a Java command line, something like:
```
java -Djava.net.preferIPv4Stack=true \
    -Dcoherence.cluster=native-image-test-1 \
    -Dcoherence.localhost=127.0.0.1 \
    -Dcoherence.wka=127.0.0.1 \
    -cp <class path> \
    com.oracle.coherence.graal.Server
```
               
When running the test with the system property `-Dcoherence.native.tests=true` Bedrock will run the native image.
In this case the class path is ignored and the main class will not be added to the command line.
The command Bedrock runs will be something like:
```
coherence-native-server/target/coherence-native-server \
    -Djava.net.preferIPv4Stack=true \
    -Dcoherence.cluster=native-image-test-1 \
    -Dcoherence.localhost=127.0.0.1 -Dcoherence.wka=127.0.0.1
```


## Contributing

This project welcomes contributions from the community. Before submitting a pull request, please [review our contribution guide](./CONTRIBUTING.md)

## Security

Please consult the [security guide](./SECURITY.md) for our responsible security vulnerability disclosure process

## License

Copyright (c) 2025 Oracle and/or its affiliates.

Released under the Universal Permissive License v1.0 as shown at
<https://oss.oracle.com/licenses/upl/>.
