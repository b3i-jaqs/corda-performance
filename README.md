# Demonstration of Corda performance issue

Start the Corda nodes by running from the root directory:

`./gradlew deployNodes`
`./kotlin_source/build/nodes/runnodes`

Bulk load data by running:

`for i in {1..1000}; do curl -X PUT 'http://localhost:10009/api/example/create-iou?iouValue=10&partyName=O=PartyB,L=New%20York,C=US'; done;`

Run the kotlin-source/src/test/kotlin/Main class, this will generate a log file in {root_dir}/log which can then be analysed.

This test program runs 9 threads that do a vault query retrieving all the entities that we have previously generated. It also runs 1 additional thread which does a vault query retrieving nothing. This thread waits for 2 seconds before running the vault query.

The expectation is that the last thread would return after roughly 2 seconds, but it takes much longer.

These are the results from a test run:

~~~~
[13:39:51.035] TRACE [pool-5-thread-7                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT5.712S
[13:39:51.933] TRACE [pool-5-thread-6                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT6.61S
[13:39:52.824] TRACE [pool-5-thread-5                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT7.501S
[13:39:53.497] TRACE [pool-5-thread-1                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT8.175S
[13:39:55.276] TRACE [pool-5-thread-8                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT9.953S
[13:39:55.811] TRACE [pool-5-thread-2                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT10.489S
[13:39:56.330] TRACE [pool-5-thread-3                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT11.008S
[13:39:56.865] TRACE [pool-5-thread-9                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT11.542S
[13:39:56.867] INFO  [pool-5-thread-10                                  ] Main.invoke - [provider2] Retrieving 0 records took PT11.543S
[13:39:57.317] TRACE [pool-5-thread-4                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT11.994S
~~~~
