package uk.gov.hmrc.bindingtariffclassification.model

import uk.gov.hmrc.bindingtariffclassification.model.Role.Role
import play.api.mvc.QueryStringBindable

case class UserSearch(role: Option[Role], team: Option[String])

object UserSearch {

  private val roleKey = "role"
  private val teamKey = "team"

  implicit def bindable(
    implicit stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[UserSearch] =
    new QueryStringBindable[UserSearch] {

      private def bindRole(key: String): Option[Role] =
        Role.values.find(_.toString.equalsIgnoreCase(key))

      override def bind(
        key: String,
        requestParams: Map[String, Seq[String]]
      ): Option[Either[String, UserSearch]] = {
        import uk.gov.hmrc.bindingtariffclassification.model.utils.BinderUtil._
        implicit val rp: Map[String, Seq[String]] = requestParams

        Some(
          Right(
            UserSearch(
              role = param(roleKey).flatMap(bindRole),
              team = param(teamKey)
            )
          )
        )
      }

      override def unbind(key: String, filter: UserSearch): String = {
        Seq(
          filter.role.map(r => stringBinder.unbind(roleKey, r.toString).mkString("&")),
          filter.team.map(r => stringBinder.unbind(teamKey, r))
        ).filter(_.isDefined).map(_.get).mkString("&")
      }
    }
}
