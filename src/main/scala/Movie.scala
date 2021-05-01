package pradip.demo_future_akka_rest

/****
 * This is a case class as the model for the REST API output Movie object.
 * @param id: The ID
 * @param name: Movie name
 * @param genre: Movie genre (thriller, suspense)
 ***/
final case class Movie(id: Int = 0, name: String, genre: String)