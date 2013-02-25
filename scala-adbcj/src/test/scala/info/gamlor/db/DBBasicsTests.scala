package info.gamlor.db

import org.adbcj.ConnectionManagerProvider
import concurrent.{ExecutionContext, Await}
import concurrent.ExecutionContext.Implicits.global
import concurrent.duration._
import util.{Failure, Success}

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.03.12
 */

class DBBasicsTests extends SpecBaseWithDB {


  describe("Basic DB operations") {
    it("can get connection ") {

      val connection = dbConnection.connect()
      connection must not be (null)

      val result = Await.result(connection, 5 seconds)
      connection must not be (result)
    }
    it("fail to get connection is reported in future") {
      val notExistingServer: String = "adbcj:jdbc:h2:tcp://not.existing.localhost:8084/~/sample"
      val noExistingConnection = ConnectionManagerProvider.createConnectionManager(notExistingServer, "sa", "")
      val connection = new DatabaseAccess(noExistingConnection, ExecutionContext.global).connect()
      connection must not be (null)



      val result = Await.ready(connection, 60 seconds)
      val resultUnpacked = result.value.get.asInstanceOf[Failure[Throwable]]
      resultUnpacked.exception.getMessage must include("not.existing.localhost")
    }
    it("can select 1") {
      val selectedOne = for {
        connection <- dbConnection.connect()
        r <- connection.executeQuery("SELECT 1 As count")

      } yield r.get(0).get(0).getInt


      val result = Await.result(selectedOne, 5 seconds)
      result must be(1)
    }
    it("fail with invalid select") {
      val selectedOne = for {
        connection <- dbConnection.connect()
        r <- connection.executeQuery("SELECT this is  Not Valid, or is it?")

      } yield r.get(0).get(0).getInt


      val result = Await.ready(selectedOne, 5 seconds)
      val resultUnpacked = result.value.get.asInstanceOf[Failure[Throwable]]
      resultUnpacked.exception.getMessage must include("this is  Not Valid")
    }
    it("can create schema") {
      val insertTable = for {
        connection <- dbConnection.connect()
        create <- connection.executeUpdate("CREATE TABLE IF NOT EXISTS simpleTable (id INT)")
        insert <- connection.executeUpdate("INSERT INTO simpleTable VALUES(1)")
        closed <- connection.close()
      } yield insert.affectedRows;

      val createResult = Await.result(insertTable, 5 seconds)
      createResult must be(1L)

      val dropTable = for {
        connection <- dbConnection.connect()
        create <- connection.executeUpdate("DROP TABLE simpleTable")
        closed <- connection.close()
      } yield create.affectedRows;

      val dropResult = Await.result(dropTable, 5 seconds)
      assert(0<=dropResult)
    }
    it("can close connection") {
      val future = for {
        connection <- dbConnection.connect()
        closeFuture <- connection.close()
      } yield connection;


      val connection = Await.result(future, 5 seconds)
      connection.isClosed must be (true)
    }
  }


}
