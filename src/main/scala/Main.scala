import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.jawn.decode
import requests.Response
import sttp.client4.quick._
import sttp.model.Uri

case class GithubApiResponse(repositories: List[GithubRepository])

object GithubApiResponse {
  implicit val decoder: Decoder[GithubApiResponse] = (hCursor: HCursor) => {
    for {
      repos <- hCursor.get[List[GithubRepository]]("items")
    } yield GithubApiResponse(repos)
  }
}

case class GithubRepository(name: String, url: String, starsCount: Int)

object GithubRepository {
  implicit val decoder: Decoder[GithubRepository] = (hCursor: HCursor) => {
    for {
      name <- hCursor.get[String]("name")
      url <- hCursor.get[String]("html_url")
      starsCount <- hCursor.get[Int]("stargazers_count")
    } yield GithubRepository(name, url, starsCount)
  }
}

object Persistence {
  def insertMultipleRepositories(repos: List[GithubRepository]): ConnectionIO[Int] = {
    Update[GithubRepository](
      """insert into repository (name, url, stars)
         values (?, ?, ?)
         """)
      .updateMany(repos)
  }
}


object Msain {
  def main(args: Array[String]): Unit = {
    val xa = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://master.f94318ce-b33a-4cdb-a291-6a10fd934f15.c.dbaas.selcloud.ru:5432/metrics",
      user = "yaroslav",
      password = "",
      logHandler = None
    )
    println("Hello world!")
    val prefix: Uri = uri"https://api.github.com/search/repositories"
    val queryParams = Map(
      "q" -> "language:java",
      "sort" -> "stars",
      "order" -> "desc",
      "page" -> "1"
    )
    val r: Response = requests.get(prefix.withParams(queryParams).toString())
    val jsonData = decode[GithubApiResponse](r.data.toString())
    println(jsonData match {
      case Right(response) =>
        Persistence.insertMultipleRepositories(response.repositories)
          .transact(xa)
        .unsafeRunSync()
    })
  }
}