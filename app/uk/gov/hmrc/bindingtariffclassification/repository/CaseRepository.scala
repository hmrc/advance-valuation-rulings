/*
 * Copyright 2021 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.bindingtariffclassification.repository

import java.time.Instant
import javax.inject.{Inject, Singleton}

import cats.syntax.all._
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.libs.json.Json.JsValueWrapper
import reactivemongo.api.indexes.Index
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.ImplicitBSONHandlers._
import reactivemongo.play.json.collection.JSONCollection
import uk.gov.hmrc.bindingtariffclassification.config.AppConfig
import uk.gov.hmrc.bindingtariffclassification.crypto.Crypto
import uk.gov.hmrc.bindingtariffclassification.model.MongoFormatters._
import uk.gov.hmrc.bindingtariffclassification.model._
import uk.gov.hmrc.bindingtariffclassification.model.reporting.v2._
import uk.gov.hmrc.bindingtariffclassification.model.reporting.CaseReportGroup.CaseReportGroup
import uk.gov.hmrc.bindingtariffclassification.model.reporting.{CaseReport => OldReport, CaseReportField, CaseReportGroup, InstantRange, ReportResult}
import uk.gov.hmrc.bindingtariffclassification.sort.SortDirection
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CaseRepository {

  def insert(c: Case): Future[Case]

  def update(c: Case, upsert: Boolean): Future[Option[Case]]

  def update(reference: String, caseUpdate: CaseUpdate): Future[Option[Case]]

  def getByReference(reference: String): Future[Option[Case]]

  def get(search: CaseSearch, pagination: Pagination): Future[Paged[Case]]

  def deleteAll(): Future[Unit]

  def delete(reference: String): Future[Unit]

  def generateReport(report: OldReport): Future[Seq[ReportResult]]

  def summaryReport(report: SummaryReport, pagination: Pagination): Future[Paged[ResultGroup]]

  def caseReport(report: CaseReport, pagination: Pagination): Future[Paged[Map[String, ReportResultField[_]]]]
}

@Singleton
class EncryptedCaseMongoRepository @Inject() (repository: CaseMongoRepository, crypto: Crypto) extends CaseRepository {

  private def encrypt: Case => Case = crypto.encrypt

  private def decrypt: Case => Case = crypto.decrypt

  override def insert(c: Case): Future[Case] = repository.insert(encrypt(c)).map(decrypt)

  override def update(c: Case, upsert: Boolean): Future[Option[Case]] =
    repository.update(encrypt(c), upsert).map(_.map(decrypt))

  override def update(reference: String, caseUpdate: CaseUpdate): Future[Option[Case]] =
    repository.update(reference, caseUpdate).map(_.map(decrypt))

  override def getByReference(reference: String): Future[Option[Case]] =
    repository.getByReference(reference).map(_.map(decrypt))

  override def get(search: CaseSearch, pagination: Pagination): Future[Paged[Case]] =
    repository.get(enryptSearch(search), pagination).map(_.map(decrypt))

  override def deleteAll(): Future[Unit] = repository.deleteAll()

  override def delete(reference: String): Future[Unit] = repository.delete(reference)

  private def enryptSearch(search: CaseSearch) = {
    val eoriEnc: Option[String] = search.filter.eori.map(crypto.encryptString)
    search.copy(filter = search.filter.copy(eori = eoriEnc))
  }

  override def generateReport(report: OldReport): Future[Seq[ReportResult]] = repository.generateReport(report)

  override def summaryReport(report: SummaryReport, pagination: Pagination): Future[Paged[ResultGroup]] =
    repository.summaryReport(report, pagination)

  override def caseReport(
    report: CaseReport,
    pagination: Pagination
  ): Future[Paged[Map[String, ReportResultField[_]]]] =
    repository.caseReport(report, pagination)
}

@Singleton
class CaseMongoRepository @Inject() (
  appConfig: AppConfig,
  mongoDbProvider: MongoDbProvider,
  mapper: SearchMapper,
  updateMapper: UpdateMapper
) extends ReactiveRepository[Case, BSONObjectID](
      collectionName = "cases",
      mongo          = mongoDbProvider.mongo,
      domainFormat   = MongoFormatters.formatCase
    )
    with CaseRepository
    with MongoCrudHelper[Case] {

  override val mongoCollection: JSONCollection = collection

  lazy private val uniqueSingleFieldIndexes = Seq("reference")
  lazy private val nonUniqueSingleFieldIndexes = Seq(
    "assignee.id",
    "queueId",
    "status",
    "application.holder.eori",
    "application.agent.eoriDetails.eori",
    "decision.effectiveEndDate",
    "decision.bindingCommodityCode",
    "daysElapsed",
    "keywords"
  )

  override def indexes: Seq[Index] =
    // TODO: We need to add relevant indexes for each possible search
    // TODO: We should add compound indexes for searches involving multiple fields
    uniqueSingleFieldIndexes.map(createSingleFieldAscendingIndex(_, isUnique      = true)) ++
      nonUniqueSingleFieldIndexes.map(createSingleFieldAscendingIndex(_, isUnique = false))

  override def insert(c: Case): Future[Case] =
    createOne(c)

  override def update(c: Case, upsert: Boolean): Future[Option[Case]] =
    updateDocument(
      selector = mapper.reference(c.reference),
      update   = c,
      upsert   = upsert
    )

  override def update(reference: String, caseUpdate: CaseUpdate): Future[Option[Case]] =
    collection
      .findAndUpdate(
        selector       = mapper.reference(reference),
        update         = updateMapper.updateCase(caseUpdate),
        fetchNewObject = true
      )
      .map(_.value.map(_.as[Case]))

  override def getByReference(reference: String): Future[Option[Case]] =
    getOne(selector = mapper.reference(reference))

  override def get(search: CaseSearch, pagination: Pagination): Future[Paged[Case]] =
    getMany(
      filterBy = mapper.filterBy(search.filter),
      sortBy   = search.sort.map(mapper.sortBy).getOrElse(Json.obj()),
      pagination
    )

  override def deleteAll(): Future[Unit] =
    removeAll().map(_ => ())

  override def delete(reference: String): Future[Unit] =
    remove("reference" -> reference).map(_ => ())

  override def generateReport(report: OldReport): Future[Seq[ReportResult]] = {
    import MongoFormatters.formatInstant
    import collection.BatchCommands.AggregationFramework._

    def groupField: CaseReportGroup => (String, String) = {
      case CaseReportGroup.QUEUE            => (CaseReportGroup.QUEUE.toString, "queueId")
      case CaseReportGroup.APPLICATION_TYPE => (CaseReportGroup.APPLICATION_TYPE.toString, "application.type")
    }

    val reportField = report.field match {
      case CaseReportField.ACTIVE_DAYS_ELAPSED   => "daysElapsed"
      case CaseReportField.REFERRED_DAYS_ELAPSED => "referredDaysElapsed"
    }

    val groupFields: Seq[(String, String)] = report.group.map(groupField).toSeq
    val group: PipelineOperator            = GroupMulti(groupFields: _*)("field" -> PushField(reportField))

    val filters = Seq[JsObject]()
      .++(report.filter.decisionStartDate.map { range =>
        Json.obj(
          "decision.effectiveStartDate" -> Json.obj(
            "$lte" -> toJson(range.max),
            "$gte" -> toJson(range.min)
          )
        )
      })
      .++(report.filter.status.map { statuses =>
        Json.obj(
          "status" -> Json.obj(
            "$in" -> JsArray(statuses.map(_.toString).map(JsString).toSeq)
          )
        )
      })
      .++(report.filter.applicationType.map { types =>
        Json.obj(
          "application.type" -> Json.obj(
            "$in" -> JsArray(types.map(_.toString).map(JsString).toSeq)
          )
        )
      })
      .++(report.filter.assigneeId.map {
        case "none" => Json.obj("assignee.id" -> JsNull)
        case a      => Json.obj("assignee.id" -> a)
      })
      .++(report.filter.reference.map { references =>
        Json.obj("reference" -> Json.obj("$in" -> JsArray(references.map(JsString).toSeq)))
      })

    val aggregation = filters match {
      case s if s.isEmpty =>
        (group, List.empty)
      case s if s.size == 1 =>
        (Match(s.head), List(group))
      case s =>
        (Match(Json.obj("$and" -> JsArray(s))), List(group))
    }

    collection
      .aggregateWith[JsObject]()(_ => aggregation)
      .collect[List](Int.MaxValue, reactivemongo.api.Cursor.FailOnError())
      .map {
        _.map { obj: JsObject =>
          val id: JsObject = obj.value("_id").as[JsObject]
          val fields: Map[CaseReportGroup, Option[String]] = groupFields.map {
            case (field, _) if id.value.contains(field) =>
              CaseReportGroup.withName(field) -> Option(id.value(field))
                .filter(_.isInstanceOf[JsString])
                .map(_.as[JsString].value)
            case (field, _) =>
              CaseReportGroup.withName(field) -> None
          }.toMap
          ReportResult(
            fields,
            obj.value("field").as[JsArray].value.map(_.as[JsNumber].value.toInt).toList
          )
        }
      }
  }

  private def trunc(json: JsValueWrapper): JsValue =
    Json.obj("$trunc" -> json)

  private def divide(dividend: JsValueWrapper, divisor: JsValueWrapper): JsValue =
    Json.obj("$divide" -> Json.arr(dividend, divisor))

  private def subtract(minuend: JsValueWrapper, subtrahend: JsValueWrapper): JsValue =
    Json.obj("$subtract" -> Json.arr(minuend, subtrahend))

  private def substrBytes(operand: JsValueWrapper, offset: JsValueWrapper, length: JsValueWrapper): JsValue =
    Json.obj("$substrBytes" -> Json.arr(operand, offset, length))

  private def daysSince(operand: JsValueWrapper): JsValue =
    trunc(
      divide(
        subtract(Json.toJson(appConfig.clock.instant()), operand),
        86400000
      )
    )

  private def matchStage(framework: collection.AggregationFramework, report: Report) = {
    import framework._

    val caseTypeFilter =
      if (report.caseTypes.isEmpty)
        Json.obj()
      else
        Json.obj("application.type" -> Json.obj("$in" -> report.caseTypes.map(Json.toJson(_))))

    val teamFilter =
      if (report.teams.isEmpty)
        Json.obj()
      else
        Json.obj("queueId" -> Json.obj("$in" -> report.teams))

    val dateFilter =
      if (report.dateRange == InstantRange.allTime)
        Json.obj()
      else
        Json.obj(
          "createdDate" -> Json.obj(
            "$gte" -> Json.toJson(report.dateRange.min),
            "$lte" -> Json.toJson(report.dateRange.max)
          )
        )

    Match(caseTypeFilter ++ teamFilter ++ dateFilter)
  }

  private def sortStage(framework: collection.AggregationFramework, report: Report) = {
    import framework._

    if (report.sortOrder == SortDirection.ASCENDING)
      Sort(Ascending(report.sortBy.underlyingField))
    else
      Sort(Descending(report.sortBy.underlyingField))
  }

  private def groupStage(framework: collection.AggregationFramework, report: SummaryReport) = {
    import framework._

    val countField = Seq("count" -> SumAll)

    val casesField = if (report.includeCases) Seq("cases" -> PushField("$ROOT")) else Seq.empty

    val sumFields =
      report.sumFields.toList.map {
        case DaysSinceField(fieldName, underlyingField) =>
          s"sum_${fieldName}" -> Sum(daysSince(s"$$$underlyingField"))
        case field =>
          s"sum_${field.fieldName}" -> SumField(field.underlyingField)
      }

    val maxFields =
      report.maxFields.toList.map {
        case DaysSinceField(fieldName, underlyingField) =>
          s"max_${fieldName}" -> Max(daysSince(s"$$$underlyingField"))
        case field =>
          s"max_${field.fieldName}" -> MaxField(field.underlyingField)
      }

    val groupFields = countField ++ sumFields ++ maxFields ++ casesField

    report.groupBy match {
      case ChapterField(_, underlyingField) =>
        Group(substrBytes(s"$$${report.groupBy.underlyingField}", 0, 2))(groupFields: _*)
      case DaysSinceField(_, underlyingField) =>
        Group(daysSince(s"$$$underlyingField"))(groupFields: _*)
      case _ =>
        Group(JsString(s"$$${report.groupBy.underlyingField}"))(groupFields: _*)
    }
  }

  private def getFieldValue(field: ReportField[_], json: Option[JsValue]): ReportResultField[_] = field match {
    case field @ CaseTypeField(fieldName, _)  => field.withValue(json.flatMap(_.asOpt[ApplicationType.Value]))
    case field @ ChapterField(fieldName, _)   => field.withValue(json.flatMap(_.asOpt[String].filterNot(_.isEmpty)))
    case field @ DateField(fieldName, _)      => field.withValue(json.flatMap(_.asOpt[Instant]))
    case field @ DaysSinceField(fieldName, _) => field.withValue(json.flatMap(_.asOpt[Long]))
    case field @ NumberField(fieldName, _)    => field.withValue(json.flatMap(_.asOpt[Long]))
    case field @ StatusField(fieldName, _)    => field.withValue(json.flatMap(_.asOpt[CaseStatus.Value]))
    case field @ StringField(fieldName, _)    => field.withValue(json.flatMap(_.asOpt[String]))
    case field @ UserField(fieldName, _)      => field.withValue(json.flatMap(_.asOpt[Operator]))
  }

  override def summaryReport(report: SummaryReport, pagination: Pagination): Future[Paged[ResultGroup]] = {
    logger.info(s"Running report: $report with pagination $pagination")

    val countField = "resultCount"

    val runCount = collection
      .aggregateWith[JsObject]() { framework =>
        import framework._

        val first = matchStage(framework, report)

        val rest = List(
          groupStage(framework, report),
          Count(countField)
        )

        (first, rest)

      }
      .head

    val runAggregation = collection
      .aggregateWith[JsObject]() { framework =>
        import framework._

        val first = matchStage(framework, report)

        val sortFieldIsGroupField =
          report.sumFields.map(f => s"sum_${f.fieldName}").contains(report.sortBy.fieldName) ||
            report.maxFields.map(f => s"max_${f.fieldName}").contains(report.sortBy.fieldName) ||
            Seq("count", "cases", "groupKey").contains(report.sortBy.fieldName)

        val rest =
          // If the sort field is one of the columns added by the grouping stage, sort after grouping
          if (sortFieldIsGroupField)
            List(
              groupStage(framework, report),
              AddFields(Json.obj("groupKey" -> "$_id")),
              Project(Json.obj("_id"        -> 0)),
              sortStage(framework, report),
              Skip((pagination.page - 1) * pagination.pageSize),
              Limit(pagination.pageSize)
            )
          else
            // Otherwise sort first
            List(
              sortStage(framework, report),
              groupStage(framework, report),
              AddFields(Json.obj("groupKey" -> "$_id")),
              Project(Json.obj("_id"        -> 0)),
              Skip((pagination.page - 1) * pagination.pageSize),
              Limit(pagination.pageSize)
            )

        (first, rest)

      }
      .fold[Seq[ResultGroup]](Seq.empty, pagination.pageSize) {
        case (rows, json) =>
          rows ++ Seq(
            if (report.includeCases)
              CaseResultGroup(
                count    = json("count").as[Long],
                groupKey = getFieldValue(report.groupBy, json.value.get("groupKey")),
                sumFields = report.sumFields.map {
                  case field @ DaysSinceField(_, _) =>
                    field.withValue(json.value.get("sum_" + field.fieldName).flatMap(_.asOpt[Long]))
                  case field @ NumberField(_, _) =>
                    field.withValue(json.value.get("sum_" + field.fieldName).flatMap(_.asOpt[Long]))
                }.toList,
                maxFields = report.maxFields.map {
                  case field @ DaysSinceField(_, _) =>
                    field.withValue(json.value.get("max_" + field.fieldName).flatMap(_.asOpt[Long]))
                  case field @ NumberField(_, _) =>
                    field.withValue(json.value.get("max_" + field.fieldName).flatMap(_.asOpt[Long]))
                }.toList,
                cases = json("cases").as[List[Case]]
              )
            else
              SimpleResultGroup(
                count    = json("count").as[Long],
                groupKey = getFieldValue(report.groupBy, json.value.get("groupKey")),
                sumFields = report.sumFields.map {
                  case field @ DaysSinceField(_, _) =>
                    field.withValue(json.value.get("sum_" + field.fieldName).flatMap(_.asOpt[Long]))
                  case field @ NumberField(_, _) =>
                    field.withValue(json.value.get("sum_" + field.fieldName).flatMap(_.asOpt[Long]))
                }.toList,
                maxFields = report.maxFields.map {
                  case field @ DaysSinceField(_, _) =>
                    field.withValue(json.value.get("max_" + field.fieldName).flatMap(_.asOpt[Long]))
                  case field @ NumberField(_, _) =>
                    field.withValue(json.value.get("max_" + field.fieldName).flatMap(_.asOpt[Long]))
                }.toList
              )
          )
      }

    (runCount, runAggregation).mapN {
      case (count, results) =>
        Paged(results, pagination, count(countField).as[Int])
    }
  }

  override def caseReport(
    report: CaseReport,
    pagination: Pagination
  ): Future[Paged[Map[String, ReportResultField[_]]]] = {
    logger.info(s"Running report: $report with pagination $pagination")

    val countField = "resultCount"

    val runCount = collection
      .aggregateWith[JsObject]() { framework =>
        import framework._

        val first = matchStage(framework, report)

        val rest = List(Count(countField))

        (first, rest)

      }
      .head

    val runAggregation = collection
      .aggregateWith[JsObject]() { framework =>
        import framework._

        val fields = Json.obj(
          report.fields.toList.map {
            case ChapterField(fieldName, underlyingField) =>
              fieldName -> (substrBytes(s"$$$underlyingField", 0, 2): JsValueWrapper)
            case DaysSinceField(fieldName, underlyingField) =>
              fieldName -> (daysSince(s"$$$underlyingField"): JsValueWrapper)
            case field =>
              field.fieldName -> (s"$$${field.underlyingField}": JsValueWrapper)
          }: _*
        )

        val first = matchStage(framework, report)

        val rest = List(
          sortStage(framework, report),
          AddFields(fields),
          Project(Json.obj("_id" -> JsNumber(0))),
          Skip((pagination.page - 1) * pagination.pageSize),
          Limit(pagination.pageSize)
        )

        (first, rest)

      }
      .fold[Seq[Map[String, ReportResultField[_]]]](Seq.empty, pagination.pageSize) {
        case (rows, json) =>
          rows ++ Seq(
            report.fields
              .map(field => field.fieldName -> getFieldValue(field, json.value.get(field.fieldName)))
              .toMap[String, ReportResultField[_]]
          )
      }

    (runCount, runAggregation).mapN {
      case (count, results) =>
        Paged(results, pagination, count(countField).as[Int])
    }
  }
}
