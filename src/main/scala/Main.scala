import cats.effect.IO
import cats.effect.unsafe.implicits.global
import doobie._
import doobie.implicits._
import io.circe._
import io.circe.jawn.decode
import requests.Response
import sttp.client4.quick._
import sttp.model.Uri

import scala.sys.process._

case class GithubApiResponse(repositories: List[GithubRepository])

object GithubApiResponse {
  implicit val decoder: Decoder[GithubApiResponse] = (hCursor: HCursor) => {
    for {
      repos <- hCursor.get[List[GithubRepository]]("items")
    } yield GithubApiResponse(repos)
  }
}

case class GithubRepository(name: String, url: String, sshUrl: String, starsCount: Int)

object GithubRepository {
  implicit val decoder: Decoder[GithubRepository] = (hCursor: HCursor) => {
    for {
      name <- hCursor.get[String]("name")
      url <- hCursor.get[String]("html_url")
      starsCount <- hCursor.get[Int]("stargazers_count")
      sshUrl <- hCursor.get[String]("ssh_url")
    } yield GithubRepository(name, url, sshUrl, starsCount)
  }
}

object Persistence {
  def insertRepo(repo: GithubRepository): ConnectionIO[Int] = {
    sql"""insert into repository (name, url, stars)
         values (${repo.name}, ${repo.url}, ${repo.starsCount})
         """.update.run
  }

  def checkExistance(repo: GithubRepository): ConnectionIO[Boolean] = {
    sql"""select exists(select 1 from repository where name = ${repo.name})
       """.query[Boolean].unique
  }
}


object Main {
  def main(args: Array[String]): Unit = {
    implicit val xa = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://master.f94318ce-b33a-4cdb-a291-6a10fd934f15.c.dbaas.selcloud.ru:5432/metrics",
      user = "yaroslav",
      password = args.head,
      logHandler = None
    )
    LazyList.from(1).foreach(processGithubRequest(_))
  }

  def processGithubRequest(page: Int)(implicit xa: Transactor[IO]): Unit = {
    val prefix: Uri = uri"https://api.github.com/search/repositories"
    val queryParams = Map(
      "q" -> "language:java",
      "sort" -> "stars",
      "order" -> "desc",
      "page" -> page.toString
    )
    val r: Response = requests.get(prefix.withParams(queryParams).toString())
    val jsonData = decode[GithubApiResponse](r.data.toString())
    jsonData match {
      case Right(response) =>
        response.repositories.foreach(repo => {
          if (Persistence.checkExistance(repo).transact(xa).unsafeRunSync()) {
            ()
          } else {
            Persistence.insertRepo(repo)
              .flatMap(_ => processRepository(repo))
              .transact(xa)
              .unsafeRunSync()
          }
        })
    }
  }

  private def processRepository(repository: GithubRepository): ConnectionIO[Int] = {
    val hook = sys.addShutdownHook({
      s"rm -rf ${repository.name}".!
    })
    s"git clone ${repository.sshUrl}".!
    val connectionIO = CKMetrics.save(repository.name, repository.name)
    s"rm -rf ${repository.name}".!
    hook.remove()
    connectionIO
  }

}