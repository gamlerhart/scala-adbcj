package info.gamlor.db

import org.adbcj.{FutureState, DbListener, DbFuture}
import java.util.concurrent.CancellationException
import concurrent.{Future, promise, ExecutionContext}

/**
 * @author roman.stoffel@gamlor.info
 * @since 29.03.12
 */

trait FutureConversions {

  implicit def context: ExecutionContext

  protected def completeWithAkkaFuture[TOrignalData, TResult]
  (futureProducingOperation: () => DbFuture[TOrignalData],
   transformation: TOrignalData => TResult) :Future[TResult] = {
    val akkaPromise = promise[TResult]()
    futureProducingOperation().addListener(new DbListener[TOrignalData] {
      def onCompletion(future: DbFuture[TOrignalData]) {
        future.getState match {
          case FutureState.SUCCESS => {
            akkaPromise.success(transformation(future.getResult))
          }
          case FutureState.FAILURE => {
            akkaPromise.failure(future.getException)
          }
          case FutureState.CANCELLED => {
            akkaPromise.failure(new CancellationException("Operation was cancelled"))
          }
          case FutureState.NOT_COMPLETED => {
            throw new Error("This state should be impossible")
          }
        }
      }
    })
    akkaPromise.future
  }

}
