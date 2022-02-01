package zhttp.service.client.experimental

import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio.{App, ExitCode, URIO, ZIO}

/**
 * Self contained ZClient demo with server.
 */
object ZClientTestWithServer extends App {

  private val PORT = 8081

  private val fooBar: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> !! / "foo" / int(id) => Response.text("bar ----- " + id)
    case Method.GET -> !! / "bar" / int(id) => Response.text("foo foo foo foo foo foo foo foo foo  " + id)
  }

  private val server =
    Server.port(PORT) ++                     // Setup port
      Server.app(fooBar) ++ Server.keepAlive // Setup the Http app

  implicit val e = new Exception("")
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    server
      .make
      .use (_ => clientTest)
      .provideCustomLayer(ServerChannelFactory.auto ++ EventLoopGroup.auto(2))
      .exitCode
  }

  def clientTest = {
    for {
      client <- (
        ZClient.threads(2) ++
          ZClient.maxConnectionsPerRequestKey(10) ++
          ZClient.maxTotalConnections(20)
        ).make
      _  <- triggerClientSequential(client)
    } yield ()

  }

  // sequential execution test
  def triggerClientSequential(cl: DefaultZClient) = for {
    req1 <- ZIO.effect("http://localhost:8081/foo/1")
    resp <- cl.run(req1)
    rval <- resp.getBodyAsString
    _    <- ZIO.effect(println(s"Response Content from $req1 ${rval.length} "))

    req3 <- ZIO.effect("http://www.google.com")
    resp3 <- cl.run(req3)
    //    resp3 <- cl.run("http://sports.api.decathlon.com/groups/water-aerobics")
    r3    <- resp3.getBodyAsString
    _     <- ZIO.effect(println(s"Response Content from $req3  ${r3.length}"))

    req2 <- ZIO.effect("http://localhost:8081/bar/2")
    resp2 <- cl.run(req2)
    r2    <- resp2.getBodyAsString
    _     <- ZIO.effect(println(s"Response Content  ${r2.length}"))

    resp4 <- cl.run("http://www.google.com")
    r4    <- resp4.getBodyAsString
    _     <- ZIO.effect(println(s"Response Content  : ${r4.length}"))

    activeCon <- cl.connectionManager.getActiveConnections
    _ <- ZIO.effect(println(s"Number of active connections for four requests: $activeCon \n\n connections map ${cl.connectionManager.connRef}"))
  } yield ()

  // multiple client shared resources
  // different conn pool for diff host port
  // pool should correspond to host / port combo
  // have one client and
  // optimizations in the background
  // just one client serves all and sundry

  // use cases like pipelining ... httpclient document

  import zhttp.http._

}