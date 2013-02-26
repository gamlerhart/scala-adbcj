package info.gamlor.adbcj.scalademo

import info.gamlor.db.{DBResultList, DBResult, DatabaseAccess, Database}
import _root_.scala.concurrent.future
import _root_.scala.concurrent.ExecutionContext.Implicits.global

/**
 * @author roman.stoffel@gamlor.info
 */
object MainDemo extends App{

  // We assume we have a MySQL server running on localhost
  // Database name: adbcj-demo
  // User: adbcj
  // Password: adbc-pwd

  // A DatabaseAccess object creates new connections to your database
  // Usually you have one instance in your system.
  // when you close the connection-manager, all associated connections are closed to.
  val database: DatabaseAccess = Database("adbcj:pooled:mysql://localhost/adbcj-demo",
    "adbcj",
    "adbc-pwd")

  // connect() will return a future, which contains the conneciton
  // You need to close the connection yourself.
  val simpleConnectionFuture = database.connect()

  // We can use the Scala for construct to deal nicely with futures
  val futureForWholeOperation = for {
     connection <-simpleConnectionFuture
     closeDone <- connection.close()
  } yield "Done"


  database.withConnection{
    connection =>
      // do something with the connection
      // you need to return a future, because everything is asynchrous
      // so the connection can only be closed when everything is done
      connection.executeQuery("SELECT 1")
  }

  val txDoneFuture = database.withTransaction{
    connection =>
      // Same goes for transactions
      connection.executeQuery("""CREATE TABLE IF NOT EXISTS posts(\n
                                id int NOT NULL AUTO_INCREMENT,\n
                                title varchar(255) NOT NULL,\n
                                ontent TEXT NOT NULL,\n
                                PRIMARY KEY (id)\n
                                ) ENGINE = INNODB;""")
  }

  txDoneFuture andThen {
    case _ =>{
      continueWithInserting()
    }

  }


  def continueWithInserting(){

    val txDoneFuture = database.withTransaction{
      connection =>
      // Try to send all queries, statements etc in one go.
      // Then collect the results at the end, or when needed for a intermediate step
        val firstPost =connection.executeUpdate("INSERT INTO posts(title,content) VALUES('The Title','TheContent')")
        val secondPost =connection.executeUpdate("INSERT INTO posts(title,content) VALUES('Second Title','More Content')")
        val thirdPost =connection.executeUpdate("INSERT INTO posts(title,content) VALUES('Third Title','Even More Content')")

        val allDone = for {
          postOne <- firstPost
          postTwo <- secondPost
          postThree <- thirdPost
        } yield "All DONE"

        allDone
    }

    txDoneFuture andThen {
      case _ =>{
        continueWithSelect()
      }

    }

  }

  def continueWithSelect(){

    val txDoneFuture = database.withTransaction{
      connection =>
        val postsFuture =connection.executeQuery("SELECT * FROM posts")

        postsFuture onSuccess {
          case rs:DBResultList => {
            for (row <- rs){
              System.out.println("ID: "+row("ID").getLong()+" with title "+row("title").getString());
            }
          }

        }

        postsFuture
    }

  }

}
