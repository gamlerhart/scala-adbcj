package info.gamlor.db

import org.scalatest.matchers.{ShouldMatchers, MustMatchers}
import org.scalatest.{FunSpec, BeforeAndAfter}
import concurrent.{ExecutionContext, Await}
import concurrent.ExecutionContext.Implicits.global;
import java.util.concurrent.TimeUnit
import concurrent.duration._

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.03.12
 */
object SpecBaseWithDB {


  val dbConnection = Database("adbcj:jdbc:h2:mem:inMemDb;DB_CLOSE_DELAY=-1","sa","pwd")(ExecutionContext.global)
}

class SpecBaseWithDB extends FunSpec
 with BeforeAndAfter
  with MustMatchers
  with ShouldMatchers {


  Class.forName("org.h2.Driver")

  def dbConnection = SpecBaseWithDB.dbConnection

  before {
      val createdSchema = for {
        connection <- SpecBaseWithDB.dbConnection.connect()
        _ <- connection.executeUpdate("CREATE TABLE IF NOT EXISTS testTable " +
          "(id INT IDENTITY PRIMARY KEY, firstname VARCHAR(255), name VARCHAR(255) , bornInYear INT)")
        _ <- connection.executeUpdate("CREATE TABLE IF NOT EXISTS insertTable " +
          "(id INT IDENTITY PRIMARY KEY, data VARCHAR(255))")
        insert <- connection.executeUpdate("INSERT INTO testTable(firstname,name,bornInYear)" +
          " VALUES('Roman','Stoffel',1986)," +
          "('Joe','Average',1990)," +
          "('Jim','Fun',1984)," +
          "('Joanna','von Anwesome',1980)")
        closed <- connection.close()
      } yield closed
      Await.ready(createdSchema, Duration(5,TimeUnit.SECONDS))
    }

    after{
      val truncateTable = for {
        connection <- SpecBaseWithDB.dbConnection.connect()
        _ <- connection.executeUpdate("TRUNCATE TABLE testTable")
        truncated <- connection.executeUpdate("TRUNCATE TABLE insertTable")
        closed <- connection.close()
      } yield closed
      Await.ready(truncateTable, 5 seconds)
    }

}
