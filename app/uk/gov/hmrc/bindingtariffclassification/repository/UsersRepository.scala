package uk.gov.hmrc.bindingtariffclassification.repository

import javax.inject.{Inject, Singleton}
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._

import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UsersRepository {

  def getById(id: String): Future[Option[Operator]]

  def allUsers: Future[Paged[Operator]]

}

@Singleton
class UsersMongoRepository @Inject()(mongoDbProvider: MongoDbProvider)
    extends ReactiveRepository[Operator, BSONObjectID](
      collectionName = "users",
      mongo = mongoDbProvider.mongo,
      domainFormat = MongoFormatters.formatOperator
    )
    with UsersRepository
    with MongoCrudHelper[Operator] {

  override protected val mongoCollection: JSONCollection = collection

  override def indexes = Seq(
      createSingleFieldAscendingIndex(indexFieldKey = "id", isUnique = true),
      createSingleFieldAscendingIndex(indexFieldKey = "name", isUnique = false),
      createSingleFieldAscendingIndex(indexFieldKey = "role", isUnique = false)
    )

  private val defaultSortBy = Json.obj("timestamp" -> -1)

  override def getById(id: String): Future[Option[Operator]] = {
      getOne(byId(id))
    }

  override def allUsers(pagination: Pagination): Future[Paged[Operator]] = {
      getMany()
    }

  private def byId(id: String): JsObject =
      Json.obj("id" -> id)

  private def selector(search: UserSearch): JsObject = {
      val queries = Seq[JsObject]()
        .++(search.role.map(r => Json.obj("role" -> in(Set(r)))))
        .++(search.team.map(t => Json.obj("team" -> mappingNoneOrSome(t))))
    /*.++(search.timestampMin.map(t => Json.obj("timestamp" -> Json.obj("$gte" -> t))))
   .++(search.timestampMax.map(t => Json.obj("timestamp" -> Json.obj("$lte" -> t))))
    */
/*
*
*
* .find({team: {$elemMatch: { $in: ['value'] } })

  .find({team: { $gt: [] } }) (edited)
*
*  .find({team: { $eq: [] } })
* */


      queries match {
        case Nil           => Json.obj()
        case single :: Nil => single
        case many          => JsObject(Seq("$and" -> JsArray(many)))
      }
    }

  private def in[T](set: Set[T])(implicit fmt: Format[T]): JsValue =
      Json.obj("$in" -> JsArray(set.map(elm => Json.toJson(elm)).toSeq))

  private def mappingNoneOrSome: String => JsValue = {
    case "none" => Json.obj("$eq" -> JsArray.empty)
    case "some" => Json.obj("$gt" -> JsArray.empty)
    case v      => in(Set(v))
  }

}
