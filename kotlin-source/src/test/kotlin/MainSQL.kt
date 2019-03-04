
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.sql.SQLException;
import java.sql.Connection;

import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

fun main(args: Array<String>) {

    val dbFile = args[0]

    val queryString = """
        select vaultschem0_.output_index as output_i1_24_0_,
            vaultschem0_.transaction_id as transact2_24_0_,
            vaultschem1_.output_index as output_i1_22_1_,
            vaultschem1_.transaction_id as transact2_22_1_,
            vaultschem0_.consumed_timestamp as consumed3_24_0_,
            vaultschem0_.contract_state_class_name as contract4_24_0_,
            vaultschem0_.lock_id as lock_id5_24_0_,
            vaultschem0_.lock_timestamp as lock_tim6_24_0_,
            vaultschem0_.notary_name as notary_n7_24_0_,
            vaultschem0_.recorded_timestamp as recorded8_24_0_,
            vaultschem0_.state_status as state_st9_24_0_,
            vaultschem1_.external_id as external3_22_1_,
            vaultschem1_.uuid as uuid4_22_1_
        from vault_states vaultschem0_
        cross join vault_linear_states vaultschem1_
        where vaultschem0_.output_index=vaultschem1_.output_index
        and vaultschem0_.transaction_id=vaultschem1_.transaction_id
        and vaultschem0_.state_status=0
        and (vaultschem0_.contract_state_class_name in ('com.example.state.IOUState'))
        limit 2000
    """.trimIndent()
    val queryStringZeroData = """
        select vaultschem0_.output_index as output_i1_24_0_,
            vaultschem0_.transaction_id as transact2_24_0_,
            vaultschem1_.output_index as output_i1_22_1_,
            vaultschem1_.transaction_id as transact2_22_1_,
            vaultschem0_.consumed_timestamp as consumed3_24_0_,
            vaultschem0_.contract_state_class_name as contract4_24_0_,
            vaultschem0_.lock_id as lock_id5_24_0_,
            vaultschem0_.lock_timestamp as lock_tim6_24_0_,
            vaultschem0_.notary_name as notary_n7_24_0_,
            vaultschem0_.recorded_timestamp as recorded8_24_0_,
            vaultschem0_.state_status as state_st9_24_0_,
            vaultschem1_.external_id as external3_22_1_,
            vaultschem1_.uuid as uuid4_22_1_
        from vault_states vaultschem0_
        cross join vault_linear_states vaultschem1_
        where vaultschem0_.output_index=vaultschem1_.output_index
        and vaultschem0_.transaction_id=vaultschem1_.transaction_id
        and (vaultschem1_.uuid in ())
        and vaultschem0_.state_status=0
        and (vaultschem0_.contract_state_class_name in ('com.example.state.IOUState'))
        limit 2000
    """.trimIndent()
    val queryDeserializer = """
        select dbtransact0_.tx_id as tx_id1_19_0_,
            dbtransact0_.transaction_value as transact2_19_0_
        from node_transactions dbtransact0_
        where dbtransact0_.tx_id=?
    """.trimIndent()
    var dataSource = HikariDataSource()
    try {
        dataSource.setDriverClassName("org.h2.Driver")
        dataSource.setJdbcUrl("jdbc:h2:$dbFile")
        dataSource.setUsername("sa")
        dataSource.setPassword("")
        dataSource.setMinimumIdle(100)
        dataSource.setMaximumPoolSize(2000)
        dataSource.setAutoCommit(false)
        dataSource.setLoginTimeout(3)
    } catch (e: SQLException) {
        e.printStackTrace()
    }

    for (i in 1..20) {
        dataSource.getConnection().close()
    }

    val outputDir = File("/tmp/corda-performance")
    outputDir.mkdirs()
    outputDir.listFiles().all { it.delete() }

    val deserializer = { conn: Connection, txId: String ->
        val statement = conn.prepareStatement(queryDeserializer)
        statement.setString(1, txId)
        val rs = statement.executeQuery()
        if (rs.next()) {
            val blob = rs.getBytes("transact2_19_0_")!!
            val writer = BufferedOutputStream(FileOutputStream(File(outputDir, "$txId")))
            writer.write(blob)
            writer.close()
        }
        rs.close()
        statement.close()
    }

    val provider = { name: String, query: String, doSleep: Boolean ->
        try{
            val before = Instant.now()
            if (doSleep) Thread.sleep(2000)
            print("[$name] start - ${doSleep} \n")
            val con = dataSource.getConnection()
            val statement = con.prepareStatement(query)
            val rs = statement.executeQuery()
            var len=0
            while (rs.next()) {
//                Thread.sleep(5) // Simulate deserialization
                val txId = rs.getString("transact2_24_0_")
                deserializer(con, txId)
                len++
            }
            val after = Instant.now()
            val duration = Duration.between(before, after)
            print("[$name] (${doSleep}) SQL Select for ${len} records took $duration\n")
            rs.close()
            statement.close()
            con.close()
        } catch (e: Exception) {
            e.printStackTrace();
        }
    }

    val executor = Executors.newFixedThreadPool(20)

    val futures = mutableListOf<Future<*>>()
    for(i in 1..10) {
        val doSleep = i.rem(2) == 0
        futures.add(executor.submit {provider("ZeroData", queryStringZeroData, doSleep)})
        futures.add(executor.submit {provider("FullData", queryString, doSleep)})
    }

    futures.forEach { it.get() }

    executor.shutdown()
}


