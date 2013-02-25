package info.gamlor.db

import concurrent.{Future, Await, Promise}
import concurrent.duration._
import concurrent.ExecutionContext.Implicits.global

/**
 * @author roman.stoffel@gamlor.info
 * @since 11.05.12
 */

class TransactionSpec extends SpecBaseWithDB {

  describe("Trasaction Support") {
    it("can rollback transcation") {
      val dbOperationResult = for {
        connection <- dbConnection.connect()
        txBeforeBeginTx <- Promise.successful(connection.isInTransaction).future
        _ <- connection.beginTransaction()
        txAfterBeginTx <- Promise.successful(connection.isInTransaction).future
        _ <- connection.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsRollback')")
        _ <- connection.rollback()
        dataAfterRollback <- connection.executeQuery("SELECT * FROM insertTable WHERE data LIKE 'transactionsRollback'")
        _ <- connection.close()
      } yield (txBeforeBeginTx, txAfterBeginTx, dataAfterRollback)

      val (txBeforeBeginTx, txAfterBeginTx, dataAfterRollback) = Await.result(dbOperationResult, 5 seconds)


      txBeforeBeginTx must be(false)
      txAfterBeginTx must be(true)
      dataAfterRollback.size must be(0)
    }
    it("can commit transcation") {
      val dbOperationResult = for {
        connection <- dbConnection.connect()
        _ <- connection.beginTransaction()
        _ <- connection.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
        _ <- connection.commit()
        data <- connection.executeQuery("SELECT * FROM insertTable WHERE data LIKE 'transactionsCommit';")
        _ <- connection.close()
      } yield (data)

      val (dataAfterCommit) = Await.result(dbOperationResult, 5 seconds)


      dataAfterCommit.size must be(1)
    }

  }
  describe("The withTransaction operations") {
    it("commits transaction") {
      val con = Await.result(dbConnection.connect(), 5 seconds)
      val dataFuture =
        con.withTransaction {
          tx =>
            val selectedData = for {
              _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
              data <- tx.executeQuery("SELECT * FROM insertTable WHERE data LIKE 'transactionsCommit';")

            } yield data
            selectedData
        }
      val data = Await.result(dataFuture, 5 seconds)

      data.size must be(1)
      con.isInTransaction() must be(false)

      val hasCommitted = Await.result(con.executeQuery("SELECT * FROM insertTable" +
        " WHERE data LIKE 'transactionsCommit';"), 5 seconds)


      hasCommitted.size must be(1)
    }
    it("rollbacks transaction") {
      val con = Await.result(dbConnection.connect(), 5 seconds)
      val dataFuture =
        con.withTransaction {
          tx =>
            val selectedData = for {
              _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
              _ <- tx.rollback()

            } yield ""
            selectedData
        }
      Await.result(dataFuture, 5 seconds)
      con.isInTransaction() must be(false)

      val hasNotCommitted = Await.result(con.executeQuery("SELECT * FROM insertTable" +
        " WHERE data LIKE 'transactionsCommit';"), 5 seconds)

      hasNotCommitted.size must be(0)
    }
    it("propagates error and rolls back") {
      val con = Await.result(dbConnection.connect(), 5 seconds)
      val dataFuture =
        con.withTransaction {
          tx =>
            val selectedData = for {
              _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
              _ <- {
                throw new SimulatedErrorException("Simulated error")
              }: Future[Unit]

            } yield ""
            selectedData
        }
      intercept[SimulatedErrorException](Await.result(dataFuture, 5 seconds))

      con.isInTransaction() must be(false)

      val hasNotCommitted = Await.result(con.executeQuery("SELECT * FROM insertTable" +
        " WHERE data LIKE 'transactionsCommit';"), 5 seconds)

      hasNotCommitted.size must be(0)
    }
    it("can nest transaction") {
      val con = Await.result(dbConnection.connect(), 5 seconds)
      val dataFuture =
        con.withTransaction {
          tx =>
            val stillInTransaction = for {
              _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
              _ <- nestedInsert(tx)
              stillInTransaction <- Promise.successful(tx.isInTransaction()).future
              _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")

            } yield stillInTransaction
            stillInTransaction
        }
      val stillInTransaction = Await.result(dataFuture, 5 seconds)
      stillInTransaction should be(true)
      val hasCommitted = Await.result(con.executeQuery("SELECT * FROM insertTable" +
        " WHERE data LIKE 'transactionsCommit';"), 5 seconds)
      hasCommitted.size must be(3)

    }
    it("propagates error in nested transaction: composition code fails") {
      failANestedTransaction(tx=> compositionCodeFailsInNestedTransaction(tx))
    }
    it("propagates error in nested transaction: future fails") {
      failANestedTransaction(tx=> futureFailsInNestedTransaction(tx))
    }

  }


  private def failANestedTransaction(failureMode:DBConnection=>Future[Unit]) {
    val con = Await.result(dbConnection.connect(), 5 seconds)
    val dataFuture =
      con.withTransaction {
        tx =>
          val selectedData = for {
            _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
            _ <- failureMode(tx)

          } yield ""
          selectedData
      }
    intercept[SimulatedErrorException](Await.result(dataFuture, 5 seconds))

    con.isInTransaction() must be(false)

    val hasNotCommitted = Await.result(con.executeQuery("SELECT * FROM insertTable" +
      " WHERE data LIKE 'transactionsCommit';"), 5 seconds)

    hasNotCommitted.size must be(0)


    Await.result(con.withTransaction {
      tx =>
        val selectedData = for {
          _ <- tx.executeUpdate("INSERT INTO insertTable(data) VALUES('new-transaction-data')")
        } yield ""
        selectedData
    }, 5 seconds)

    val hasCommittedOnNextTx = Await.result(con.executeQuery("SELECT * FROM insertTable" +
      " WHERE data LIKE 'new-transaction-data';"), 5 seconds)
    hasCommittedOnNextTx.size must be(1)
  }

  private def nestedInsert(connection: DBConnection) = connection.withTransaction {
    tx => tx.executeUpdate("INSERT INTO insertTable(data) VALUES('transactionsCommit')")
  }

  private def compositionCodeFailsInNestedTransaction(connection: DBConnection) = connection.withTransaction {
    tx => {
      throw new SimulatedErrorException("Simulated error")
    }: Future[Unit]
  }
  private def futureFailsInNestedTransaction(connection: DBConnection) = connection.withTransaction {
    tx => Promise.failed(new SimulatedErrorException("Simulated error")).future
  }


}
