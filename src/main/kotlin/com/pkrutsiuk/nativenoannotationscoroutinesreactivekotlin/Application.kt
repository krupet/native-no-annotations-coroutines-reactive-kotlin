package com.pkrutsiuk.nativenoannotationscoroutinesreactivekotlin

import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.ClassPathResource
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.fu.kofu.configuration
import org.springframework.fu.kofu.r2dbc.r2dbc
import org.springframework.fu.kofu.reactiveWebApplication
import org.springframework.fu.kofu.templating.mustache
import org.springframework.fu.kofu.webflux.webFlux
import org.springframework.http.MediaType
import org.springframework.r2dbc.connection.init.ConnectionFactoryInitializer
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.web.reactive.function.server.*
import org.springframework.web.reactive.function.server.ServerResponse.ok

val app = reactiveWebApplication {
  configurationProperties<SampleProperties>(prefix = "sample")
  enable(dataConfig)
  enable(webConfig)
}

fun main() {
  app.run()
}

val dataConfig = configuration {
  beans {
    bean<UserRepository>()
    bean {
      ConnectionFactoryInitializer().apply {
        setConnectionFactory(ref())
        setDatabasePopulator(ResourceDatabasePopulator(ClassPathResource("tables.sql")))
      }
    }
  }
  r2dbc {
    url = "r2dbc:postgresql://localhost:5432/postgres"
    password = "postgres"
    username = "postgres"
  }
}

val webConfig = configuration {
  beans {
    bean<UserHandler>()
    bean(::routes)
  }
  webFlux {
    port = if (profiles.contains("test")) 8181 else 8080
    mustache()
    codecs {
      string()
      jackson()
    }
  }
}

@Table("users")
data class User(
  @Id
  val login: String,
  val firstname: String,
  val lastname: String
)

class SampleProperties(val message: String)

@Suppress("UNUSED_PARAMETER")
class UserHandler(
  private val repository: UserRepository,
  private val configuration: SampleProperties
) {

  suspend fun listApi(request: ServerRequest) =
    ok().contentType(MediaType.APPLICATION_JSON).bodyAndAwait<User>(repository.findAll())

  suspend fun userApi(request: ServerRequest) =
    ok().contentType(MediaType.APPLICATION_JSON).run {
      repository.findOne(request.pathVariable("login"))
        ?.let { bodyValueAndAwait(it) }
        ?: buildAndAwait()
    }


  suspend fun listView(request: ServerRequest) =
    ok().renderAndAwait("users", mapOf("users" to repository.findAll()))

  suspend fun conf(request: ServerRequest) =
    ok().bodyValueAndAwait(configuration.message)

}

fun routes(userHandler: UserHandler) = coRouter {
  GET("/", userHandler::listView)
  GET("/api/user", userHandler::listApi)
  GET("/api/user/{login}", userHandler::userApi)
  GET("/conf", userHandler::conf)
}

class UserRepository(private val client: DatabaseClient) {

  suspend fun count() =
    client.sql("SELECT COUNT(login) FROM users")
      .map { row -> (row.get(0) as Long).toInt() }
      .first().awaitSingle()


  suspend fun findAll() =
    client.sql("SELECT login, firstname, lastname from users")
      .map { row ->
        User(
          login = row.get("login", String::class.java)!!,
          firstname = row.get("firstname", String::class.java)!!,
          lastname = row.get("lastname", String::class.java)!!
        )
      }
      .all()
      .asFlow()

  suspend fun findOne(id: String?) =
    id?.let {
      client.sql("SELECT login, firstname, lastname from users where login = :id")
        .bind("id", it)
        .map { row ->
          User(
            login = row.get("login", String::class.java)!!,
            firstname = row.get("firstname", String::class.java)!!,
            lastname = row.get("lastname", String::class.java)!!
          )
        }
        .first().awaitSingle()
    }


  suspend fun deleteAll() =
    client.sql("DELETE FROM users").then().awaitSingle()


  suspend fun insert(user: User) =
    client.sql("INSERT INTO users(login, firstname, lastname) values(:login, :firstname, :lastname)")
      .bind("login", user.login)
      .bind("firstname", user.firstname)
      .bind("lastname", user.lastname)
      .then()
      .awaitSingle()

}