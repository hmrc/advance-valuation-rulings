package uk.gov.hmrc.advancevaluationrulings.models

import generators.ModelGenerators
import org.scalacheck.Arbitrary.arbitrary
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.libs.json.{JsString, JsSuccess, Json}
import play.api.mvc.PathBindable

class DraftIdSpec extends AnyFreeSpec with Matchers with ScalaCheckPropertyChecks with ModelGenerators with EitherValues {

  "an draft Id" - {

    val pathBindable = implicitly[PathBindable[DraftId]]

    "must bind from a url" in {

      forAll(arbitrary[String], draftIdGen) {
        (key, value) =>
          pathBindable.bind(key, value.toString).value mustEqual value
      }
    }

    "must unbind to a url" in {

      forAll(arbitrary[String], draftIdGen) {
        (key, value) =>
          pathBindable.unbind(key, value) mustEqual value.toString
      }
    }

    "must serialise and deserialise to / from JSON" in {

      forAll(draftIdGen) {
        draftId =>

          val json = Json.toJson(draftId)
          json mustEqual JsString(draftId.toString)
          json.validate[DraftId] mustEqual JsSuccess(draftId)
      }
    }
  }
}
