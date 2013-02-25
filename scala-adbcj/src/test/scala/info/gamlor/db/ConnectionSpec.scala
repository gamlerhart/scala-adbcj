package info.gamlor.db

import java.util.concurrent.atomic.AtomicReference
import concurrent.{Await, Promise}
import concurrent.duration._



/**
 * @author roman.stoffel@gamlor.info
 * @since 12.05.12
 */

class ConnectionSpec extends SpecBaseWithDB{

  describe("Trasaction Support") {
    it("closes connection"){
      val resultFuture = dbConnection.withConnection{
        conn=>{
          Promise.successful((conn.isOpen,conn)).future
        }
      }
      val (wasConnectionOpen,conn) = Await.result(resultFuture,5 seconds)

      wasConnectionOpen should be(true)
      conn.isClosed should be(true)
    }
    it("closes on error in future"){
      val connectionExtraction = new AtomicReference[DBConnection]
      val resultFuture = dbConnection.withConnection{
        conn=>{
          connectionExtraction.set(conn)
          Promise.failed(new SimulatedErrorException("Simulated error")).future
        }
      }
      intercept[SimulatedErrorException](Await.result(resultFuture,5 seconds))
      connectionExtraction.get().isClosed should be(true)
    }
    it("closes on error in composing fuction"){
      val connectionExtraction = new AtomicReference[DBConnection]
      val resultFuture = dbConnection.withConnection{
        conn=>{
          connectionExtraction.set(conn)
          throw new SimulatedErrorException("Simulated error")
        }
      }
      intercept[SimulatedErrorException](Await.result(resultFuture,5 seconds))
      connectionExtraction.get().isClosed should be(true)
    }
  }

}
