
import com.example.state.IOUState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.DEFAULT_PAGE_NUM
import net.corda.core.node.services.vault.PageSpecification
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.NetworkHostAndPort
import net.corda.core.utilities.loggerFor
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.slf4j.Logger
import java.util.concurrent.Future


private val logger: Logger = loggerFor<Main>()

class Main

fun main(args: Array<String>) {

    val hostAndPort = NetworkHostAndPort("localhost", 10008)
    val client = CordaRPCClient(
        hostAndPort
    )
    val conn = client.start("user1", "test")
    val proxy = conn.proxy

    val criteria = QueryCriteria.LinearStateQueryCriteria(
        status = Vault.StateStatus.UNCONSUMED
    )
    val pageSpec = PageSpecification(DEFAULT_PAGE_NUM, 2000)

    val provider1 = {
        val before = Instant.now()
        logger.info("Start 1")
        val states = proxy.vaultQueryByWithPagingSpec(
            contractStateType = IOUState::class.java,
            criteria = criteria,
            paging = pageSpec
        )
        val after = Instant.now()
        val duration = Duration.between(before, after)
        logger.trace("[provider1] Retrieving ${states.states.size} records took $duration")
        states
    }

    val provider2 = {
        val criteria2 = QueryCriteria.LinearStateQueryCriteria(
            status = Vault.StateStatus.UNCONSUMED
            , uuid = emptyList()
        )
        val before = Instant.now()
        Thread.sleep(2000)
        logger.info("Start 2")
        val states = proxy.vaultQueryByWithPagingSpec(
            contractStateType = IOUState::class.java,
            criteria = criteria2,
            paging = pageSpec
        )
        val after = Instant.now()
        val duration = Duration.between(before, after)
        logger.info("[provider2] Retrieving ${states.states.size} records took $duration")
        states
    }

    val executor = Executors.newFixedThreadPool(10)

    val futures = mutableListOf<Future<*>>()
    for(i in 1..9) {
        futures.add(executor.submit(provider1))
    }
    val future2 = executor.submit(provider2)
    futures.forEach { it.get() }

//    val future1 = executor.submit(provider1)
//    val result1 = future1.get()
    val result2 = future2.get()



//    executor.awaitTermination(20, TimeUnit.SECONDS)
    executor.shutdown()

//    val states = provider()

    conn.close()
}
