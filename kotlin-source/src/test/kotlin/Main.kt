
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
        logger.info("[provider1] Retrieving ${states.states.size} records took $duration")
        states
    }

    val provider2 = { doSleep: Boolean ->
        val criteria2 = QueryCriteria.LinearStateQueryCriteria(
            status = Vault.StateStatus.UNCONSUMED
            , uuid = emptyList()
        )
        val before = Instant.now()
        if (doSleep) Thread.sleep(2000)
        logger.info("Start 2")
        val states = proxy.vaultQueryByWithPagingSpec(
            contractStateType = IOUState::class.java,
            criteria = criteria2,
            paging = pageSpec
        )
        val after = Instant.now()
        val duration = Duration.between(before, after)
        logger.info("[provider2($doSleep)] Retrieving ${states.states.size} records took $duration")
        states
    }

    val executor = Executors.newFixedThreadPool(20)

    val futures = mutableListOf<Future<*>>()
    for(i in 1..10) {
        val doSleep = i.rem(2) == 0
        futures.add(executor.submit {provider2(doSleep)})
        futures.add(executor.submit(provider1))
    }

    futures.forEach { it.get() }

    executor.shutdown()

    conn.close()
}

