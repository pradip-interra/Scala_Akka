package pradip.demo_future_akka_rest

// scala related exports

import pradip.demo_future_akka_rest.RestInvoker.request

import java.lang.RuntimeException
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

// akka related exports
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.marshalling.Marshalling
import akka.http.scaladsl.unmarshalling.Unmarshal

// Formatters
import spray.json._
import spray.json.DefaultJsonProtocol._

/**
 * The Rest Tested class
 * We have a simple Node.js + Express server running in http://localhost:3001/api/movies endpoint that
 * supports POST, GET, PATCH, PUT, DELETE of Movie object consists of Id(int), Name(String) and Genre(String).
 */
object RestInvoker extends App {
  // Boilerplate Akka code
  implicit val system : ActorSystem = ActorSystem()         // Akka Actor
  implicit val materializer : ActorMaterializer = ActorMaterializer()   // Akka Streams
  import system.dispatcher      // thread pool

  // JSON marshall / unmarshall
  implicit val dataObjectFormatter = jsonFormat3(Movie)

  private val endPoint = "http://localhost:3000/api/movies"

  // Create POST REST Request
  private val movie = Movie(name = "3-idiots", genre = "satire")
  val request = HttpRequest(
    method = HttpMethods.POST,
    uri = endPoint,
    entity = HttpEntity(
      ContentTypes.`application/json`,
      movie.toJson.toString()   // this is how the movie object is marshalled to JSON for passing to Entity.
    )
  )

  /**
   * We are going to create(POST) a Movie.
   * Then we shall retrieve the ID of the newly created Movie.
   * Then we retrieve(GET) the movie and print it.
   * At last, we will get rid of (DELETE) of the movie!
   * All CRUD operation .. well .. almost.
   */

  // retrieve the value: Elaborate. Later on we used pretty standard way.
  val responseFuture: Future[HttpResponse] = Http().singleRequest(request)
//  responseFuture.foreach(println) // the total Futures data structure
  val resEntityFuture: Future[HttpEntity.Strict] = responseFuture.flatMap(res => res.entity.toStrict(5 seconds))
//  resEntityFuture.foreach(println)
  val actResponse: Future[String] = resEntityFuture.map(entity => entity.data.utf8String)
  // actResponse.foreach(println)


  // Sticking the next Future all: GET on the :id of the newly
  actResponse.onComplete ({
    case Success(response: String) => {
      // get the ID of the newly created object
      val createdMovie = JsonParser(response).convertTo[Movie]
      val createdMovieId = createdMovie.id

      val getRequest = HttpRequest(
        method = HttpMethods.GET,
        uri = endPoint + "/" + createdMovieId
      )
      // make the next GET call
      Http().singleRequest(getRequest).onComplete({
        case Success(getResFuture) => {
          // Unmarshall response body (of type response.entity) in a movie object . It's actually a Future of Movie.
          val futureMovie: Future[Movie]= Unmarshal(getResFuture.entity).to[Movie]
          // When the Future is fulfilled
          futureMovie.onComplete({
            case Success(getResponse) => {
              // print the JSON string as is.
              // Pattern Matching: type
              getResponse match {
                case m:Movie => {
                  println("Instance GET API Output:" + m.toString)

                  // Now let me delete it please :)
                  val deleteReq = HttpRequest(method = HttpMethods.DELETE, uri = endPoint + "/" + createdMovieId)
                  Http().singleRequest(deleteReq).onComplete({
                    case Success(deletedFuture) => {
                      val deleytedMovieFuture : Future[Movie]= Unmarshal(deletedFuture.entity).to[Movie]
                      deleytedMovieFuture.onComplete({
                        case Success(value) => println("Deleted movie: " + value.toJson)
                        case Failure(exception) => {
                          deletedFuture.discardEntityBytes(materializer);
                          throw new RuntimeException()
                        }
                      })
                    }
                    case Failure(exception) => {
                        getResFuture.discardEntityBytes(materializer);
                        throw new RuntimeException();
                    }
                  })
                }
                case _ => {
                  getResFuture.discardEntityBytes(materializer);
                  throw new RuntimeException();
                }
              }
            }
            case Failure(e) => {
              getResFuture.discardEntityBytes(materializer);
              throw new RuntimeException(e);
            }
          })
        }
        case Failure(e) => {
          getRequest.discardEntityBytes(materializer);
          throw new RuntimeException(e);
        }
      })
    }
    case Failure(e) => {
      throw new RuntimeException(e);
    }
  })
}
