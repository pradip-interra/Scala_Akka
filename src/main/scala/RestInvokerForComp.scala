package pradip.demo_future_akka_rest

// scala related exports

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

// akka related exports
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.ActorMaterializer

// Formatters
import spray.json.DefaultJsonProtocol._
import spray.json._

/**
 * The Rest Tested class
 * We have a simple Node.js + Express server running in http://localhost:3001/api/movies endpoint that
 * supports POST, GET, PATCH, PUT, DELETE of Movie object consists of Id(int), Name(String) and Genre(String).
 */

/**
 * This is using Akka http client for making the API calls, BUT using for comprehensions by stiching the API calls.
 */
object RestInvokerForComp extends App {
  // Boilerplate Akka code
  implicit val system: ActorSystem = ActorSystem() // Akka Actor
  implicit val materializer: ActorMaterializer = ActorMaterializer() // Akka Streams

  import system.dispatcher // thread pool

  // JSON marshall / unmarshall
  implicit val dataObjectFormatter = jsonFormat3(Movie)

  private val endPoint = "http://localhost:3000/api/movies"

  // Create POST REST Request
  private val movie = Movie(name = "3-idiots", genre = "satire")
  val postRequest = HttpRequest(
    method = HttpMethods.POST,
    uri = endPoint,
    entity = HttpEntity(
      ContentTypes.`application/json`,
      movie.toJson.toString() // this is how the movie object is marshalled to JSON for passing to Entity.
    )
  )

  /**
   * We are going to create(POST) a Movie.
   * Then we shall retrieve the ID of the newly created Movie.
   * Then we retrieve(GET) the movie and print it.
   * At last, we will get rid of (DELETE) of the movie!
   * All CRUD operation .. well .. almost.
   */
  val client = Http();

  /**
   * In this case we are making the Response Futures inside the For comprehension, that's why they are executing one after another.
   * If we would have created the Response Futures outside of the for loop and then just use the Response Futures inside, then those calls will be
   * executed in parallel.
   */
  val seriesOfHttpCalls = for {
    /** Creating a new Movie. */
    postResponse: HttpResponse <- client.singleRequest(postRequest).recover({
      // individual error check on a particular API
      case NonFatal(e) => throw new RuntimeException(e)
    })
    postResponseOutput: String <- Unmarshal(postResponse.entity).to[String]
    createdMovie: Movie = JsonParser(postResponseOutput).convertTo[Movie]

    /** Chaining the instance GET call to read the newly created movie. */
    // Pass the respone of previous API to the next GET API
    getRequest: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = endPoint + "/" + createdMovie.id
    )
    getResponse: HttpResponse <- client.singleRequest(getRequest)
    getResponseOutput: String <- Unmarshal(getResponse.entity).to[String]
    getMovie: Movie = JsonParser(getResponseOutput).convertTo[Movie]

    /** Chaining the instance DELETE call to delete the newly created movie. */
    // Now lets delete it! The movie ID is the newly created one
    deleteRequest: HttpRequest = HttpRequest(
      method = HttpMethods.DELETE,
      uri = endPoint + "/" + getMovie.id
    )
    deleteResponse: HttpResponse <- client.singleRequest(deleteRequest)
    deleteResponseOutput: String <- Unmarshal(deleteResponse.entity).to[String]
  } yield Array(postResponseOutput, getResponseOutput, deleteResponseOutput)

  // overall error handling, if anyone will fail, will come here.
  seriesOfHttpCalls.recover {
    case NonFatal(e) => {
      println("Failed in the final response.");
      throw new RuntimeException(e);
    }
  }
  // Final success
  seriesOfHttpCalls.onComplete {
    case Success(responses) => {
      println(s"POST Response: ${responses(0).toJson}")
      println(s"GET Response: ${responses(1).toString}")
      println(s"DELETE Response: ${responses(2).toString}")
    }
    case Failure(e) => {
      e.printStackTrace
      throw new RuntimeException(e);
    }
  }
}

