# Demonstration of Corda performance issue

Configure access to a Corda Enterprise repository by setting the following Gradle properties:

* repo_enterprise_url=
* repo_enterprise_username=
* repo_enterprise_password=


Start the Corda nodes by running from the root directory:

```bash
./gradlew deployNodes
./kotlin_source/build/nodes/runnodes
```

Bulk load data by running:

```bash
for i in {1..1000}; do curl -X PUT 'http://localhost:10009/api/example/create-iou?iouValue=10&partyName=O=PartyB,L=New%20York,C=US'; done;
```

Run the kotlin-source/src/test/kotlin/Main class, this will generate a log file in {root_dir}/log which can then be analysed.

This test program runs 20 threads divided as:

1. 10 threads that query the vault to retrieve the full data set.
1. 10 threads that query the vault to retrieve 0 records. These threads are themselves divided into 2 groups:
    1. 5 threads which query the vault immediately
    1. 5 threads which wait for 2 seconds before querying the vault

The expectation is that:

* The 10 threads that retrieve the full data set would run all in roughly the same time (about 5 seconds)
* The 5 threads that return 0 records and run immediately would run in about 0 seconds
* The 5 threads that return 0 records and wait for 2 seconds would return in about 2 seconds

These are the results from a test run:

~~~~
[10:51:07.382] INFO  [pool-5-thread-1                                   ] Main.invoke - Start 2
[10:51:07.382] INFO  [pool-5-thread-2                                   ] Main.invoke - Start 1
[10:51:07.383] INFO  [pool-5-thread-4                                   ] Main.invoke - Start 1
[10:51:07.383] INFO  [pool-5-thread-5                                   ] Main.invoke - Start 2
[10:51:07.383] INFO  [pool-5-thread-6                                   ] Main.invoke - Start 1
[10:51:07.383] INFO  [pool-5-thread-8                                   ] Main.invoke - Start 1
[10:51:07.384] INFO  [pool-5-thread-9                                   ] Main.invoke - Start 2
[10:51:07.386] INFO  [pool-5-thread-10                                  ] Main.invoke - Start 1
[10:51:07.395] INFO  [pool-5-thread-12                                  ] Main.invoke - Start 1
[10:51:07.399] INFO  [pool-5-thread-13                                  ] Main.invoke - Start 2
[10:51:07.403] INFO  [pool-5-thread-14                                  ] Main.invoke - Start 1
[10:51:07.415] INFO  [pool-5-thread-5                                   ] Main.invoke - [provider2(false)] Retrieving 0 records took PT0.032S
[10:51:07.425] INFO  [pool-5-thread-16                                  ] Main.invoke - Start 1
[10:51:07.426] INFO  [pool-5-thread-17                                  ] Main.invoke - Start 2
[10:51:07.427] INFO  [pool-5-thread-18                                  ] Main.invoke - Start 1
[10:51:07.435] INFO  [pool-5-thread-20                                  ] Main.invoke - Start 1
[10:51:07.440] INFO  [pool-5-thread-1                                   ] Main.invoke - [provider2(false)] Retrieving 0 records took PT0.058S
[10:51:09.383] INFO  [pool-5-thread-3                                   ] Main.invoke - Start 2
[10:51:09.383] INFO  [pool-5-thread-7                                   ] Main.invoke - Start 2
[10:51:09.391] INFO  [pool-5-thread-11                                  ] Main.invoke - Start 2
[10:51:09.415] INFO  [pool-5-thread-15                                  ] Main.invoke - Start 2
[10:51:09.431] INFO  [pool-5-thread-19                                  ] Main.invoke - Start 2
[10:51:12.523] TRACE [pool-5-thread-12                                  ] Main.invoke - [provider1] Retrieving 1000 records took PT5.128S
[10:51:13.435] TRACE [pool-5-thread-6                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT6.052S
[10:51:14.230] TRACE [pool-5-thread-2                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT6.848S
[10:51:14.915] TRACE [pool-5-thread-8                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT7.532S
[10:51:14.920] INFO  [pool-5-thread-13                                  ] Main.invoke - [provider2(false)] Retrieving 0 records took PT7.521S
[10:51:14.921] INFO  [pool-5-thread-9                                   ] Main.invoke - [provider2(false)] Retrieving 0 records took PT7.537S
[10:51:16.758] TRACE [pool-5-thread-10                                  ] Main.invoke - [provider1] Retrieving 1000 records took PT9.372S
[10:51:16.762] INFO  [pool-5-thread-17                                  ] Main.invoke - [provider2(false)] Retrieving 0 records took PT9.336S
[10:51:17.347] TRACE [pool-5-thread-4                                   ] Main.invoke - [provider1] Retrieving 1000 records took PT9.964S
[10:51:17.919] TRACE [pool-5-thread-16                                  ] Main.invoke - [provider1] Retrieving 1000 records took PT10.494S
[10:51:18.527] TRACE [pool-5-thread-14                                  ] Main.invoke - [provider1] Retrieving 1000 records took PT11.124S
[10:51:18.528] INFO  [pool-5-thread-3                                   ] Main.invoke - [provider2(true)] Retrieving 0 records took PT11.145S
[10:51:18.529] INFO  [pool-5-thread-7                                   ] Main.invoke - [provider2(true)] Retrieving 0 records took PT11.146S
[10:51:18.530] INFO  [pool-5-thread-11                                  ] Main.invoke - [provider2(true)] Retrieving 0 records took PT11.139S
[10:51:18.530] INFO  [pool-5-thread-15                                  ] Main.invoke - [provider2(true)] Retrieving 0 records took PT11.115S
[10:51:18.531] INFO  [pool-5-thread-19                                  ] Main.invoke - [provider2(true)] Retrieving 0 records took PT11.1S
[10:51:19.219] TRACE [pool-5-thread-18                                  ] Main.invoke - [provider1] Retrieving 1000 records took PT11.792S
[10:51:19.678] TRACE [pool-5-thread-20                                  ] Main.invoke - [provider1] Retrieving 1000 records took PT12.243S
~~~~

The results are not as expected, with a 2 of the fast running threads being able to run in 0 seconds, but not the other 3 ones which had to wait for 3 of the long running threads before being able to proceed.

Also, the threads that should have taken 2 seconds took all about 11 seconds. This suggests that they where all waiting for other threads to end their run before they could complete their own run.

All this makes it seem like there is some undesired queueing going on.
