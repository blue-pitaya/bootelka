package appname.web

import appname.auth.HttpModel._
import com.raquo.airstream.core.EventStream
import io.laminext.fetch.circe._
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits._

object HttpClient {
  val authApiBaseUrl = "http://localhost:8080"

  def requestLogin(data: Login_IN): EventStream[FetchResponse[String]] = Fetch
    .post(s"$authApiBaseUrl/user/login", body = data)
    .text

  def requestUserInfo(authToken: String): EventStream[FetchResponse[String]] =
    Fetch
      .get(s"$authApiBaseUrl/user/info")
      .addHeaders("Authorization" -> s"Bearer $authToken")
      .text

}
