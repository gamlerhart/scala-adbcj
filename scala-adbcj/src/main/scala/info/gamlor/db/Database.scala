package info.gamlor.db

import org.adbcj._
import concurrent.{Promise, Future, ExecutionContext}
import java.util
import util.concurrent.atomic.AtomicReference

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.03.12
 */

/**
 * Entry point for using ADBCJ with Scala.
 *
 * <pre></pre>
 *
 * You also can provide a manually created connection manager.
 * <pre>
 * val connectionManager =
 *    ConnectionManagerProvider.createConnectionManager(config.url, config.userName, config.passWord)
 * implicit val executionContext = ... // Your execution context
 *
 * val dbInstance = Database.createDatabaseAccessObject(connectionManager);
 *
 * </pre>
 *
 *
 */
object Database {
  /**
   * Create a complete new instance for database access,
   * with the given connectionManager and context
   * @param connectionManager source for new connections
   * @param context  dispatcher context for the returned futures
   * @return database access
   */
  def apply(connectionManager: ConnectionManager)(implicit context: ExecutionContext):DatabaseAccess ={
    new DatabaseAccess(connectionManager,context)
  }


  /**
   * Creates a complete new instance for database access.
   * @param url The url to the database. The url to the database. It has the usual format: adbcj:your-database-type://host:port/database. Example adbcj:mysql:localhost:3306/database
   * @param userName The database user
   * @param password The password
   * @param context  dispatcher context for the returned futures
   * @return database access
   */
  def apply(url:String,userName:String,password:String,settings:(String, String)*)(implicit context: ExecutionContext):DatabaseAccess ={
    val javaSettingsMap = new util.HashMap[String,String]()
    for ((key,value)<-settings){
      javaSettingsMap.put(key,value)
    }
    val connectionManager = ConnectionManagerProvider.createConnectionManager(url,userName,password,javaSettingsMap)
    new DatabaseAccess(connectionManager,context)
  }


}

/**
 * Start interface for working with the database.
 *
 * Use withConnection() to run a few operations and close that connection
 * when the returned future is completed.
 *
 * Use connect() to get a regular database connection
 * @param connectionManager source for new connections
 * @param context  dispatcher context for the returned futures
 */
class DatabaseAccess(val connectionManager: ConnectionManager,
                     implicit val context: ExecutionContext
                      ) extends FutureConversions {
/**
 * Opens a connection to the database and runs the code of the given closure with it. It will close the connection
 * when the future which the closure returns finishes.
 *
 * This is intended for doing multiple read operations and then close the file: for example:
 * <pre>Database(akkaSystem).withConnection{
 *      connection=>{
 *       connection.executeQuery("SELECT first_name,last_name,hire_date FROM employees")
 *        }
 *     }
 *    }</pre>
 * @param operation the closure to execute
 */
  def withConnection[T](operation: DBConnection => Future[T]): Future[T] = {
    connect().flatMap(conn => {
      val operationFuture = try {
        operation(conn)
      } catch {
        case error: Throwable => {
          conn.close().flatMap(u => Promise.failed(error).future)
        }
      }
      operationFuture
        .flatMap(result => conn.close().map(_ => result))
        .recoverWith({
        case error: Throwable => {
          conn.close().flatMap(u => Promise.failed(error).future)
        }
      })
    }

    )
  }


  /**
   * Connects to the database and returns the connection in a future
   *
   * In case of a failure the closure finishes with a [[org.adbcj.DbException]]
   * @return future which completes with the connection or a [[org.adbcj.DbException]]
   */
  def connect(): Future[DBConnection] = {
    completeWithAkkaFuture[Connection, DBConnection](() => connectionManager.connect(), c => DBConnection(c))
  }

  def close():Future[Unit] = {
    completeWithAkkaFuture[Void, Unit](() => connectionManager.close(), _ => ())
  }

}
