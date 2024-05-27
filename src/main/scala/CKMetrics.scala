import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.github.mauricioaniche.ck.{CK, CKClassResult, CKMethodResult, CKNotifier}
import doobie.implicits._
import doobie.{ConnectionIO, Transactor, Update}

import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.reflect.io.Path
import scala.sys.process._

object CKMetrics {
  def save(name: String, path: Path): ConnectionIO[Int] = {
    //"git clone git@github.com:mauricioaniche/ck.git".!
    var repoMetrics: List[CKClassResult] = List()
    new CK(false, 0, true).calculate(path.toString(), new CKNotifier() {
      override def notify(result: CKClassResult): Unit = {
        repoMetrics = result :: repoMetrics
      }

      override def notifyError(sourceFilePath: String, e: Exception): Unit = {
        System.err.println("Error in " + sourceFilePath)
        e.printStackTrace(System.err)
      }
    })
    val repoMethodMetrics = repoMetrics
      .flatMap(ckClassResult =>
        ckClassResult.getMethods.asScala
          .map(methodResult => PgMethodMetrics(
            name,
            ckClassResult,
            methodResult)))

    val variablesMetrics = repoMetrics
      .flatMap(ckClassResult =>
        ckClassResult.getMethods.asScala
          .flatMap(methodResult => methodResult.getVariablesUsage.entrySet().asScala
            .map(entry => PgVariableMetrics(name, ckClassResult, methodResult, entry.getKey, entry.getValue))))

    val fieldsMetrics = repoMetrics
      .flatMap(ckClassResult =>
        ckClassResult.getMethods.asScala
          .flatMap(methodResult => methodResult.getFieldUsage.entrySet().asScala
            .map(entry => PgVariableMetrics(name, ckClassResult, methodResult, entry.getKey, entry.getValue))))

    saveClassesMetrics(for (value <- repoMetrics) yield PgClassMetrics(name, value))
      .flatMap(_ =>
        saveMethodMetrics(repoMethodMetrics)
          .flatMap(_ =>
            saveVariableMetrics("variable_metrics", variablesMetrics)
              .flatMap(_ =>
                saveVariableMetrics("field_metrics", fieldsMetrics)
                  .map(con => con)
              )
          )
      )
  }

  private def saveClassesMetrics(pgClassesMetrics: List[PgClassMetrics]): ConnectionIO[Int] =
    Update[PgClassMetrics](
      """
        |insert into class_metrics
        | (repo_name, file, class, type,
        |  cbo, cbomodified,
        |  fanin, fanout,
        |  wmc,
        |  dit, noc, rfc,
        |  lcom, lcom_star,
        |  tcc, lcc,
        |  totalmethodsqty, staticmethodsqty,
        |  publicmethodsqty, privatemethodsqty,
        |  protectedmethodsqty, defaultmethodsqty,
        |  visiblemethodsqty, abstractmethodsqty,
        |  finalmethodsqty, synchronizedmethodsqty,
        |
        |  totalfieldsqty, staticfieldsqty,
        |  publicfieldsqty, privatefieldsqty,
        |  protectedfieldsqty, defaultfieldsqty,
        |  finalfieldsqty, synchronizedfieldsqty,
        |  nosi,
        |  loc,
        |  returnqty,
        |  loopqty,
        |  comparisonqty,
        |  trycatchqty,
        |  parenthesizedexpsqty,
        |  stringliteralsqty,
        |  numbersqty,
        |  assignmentsqty,
        |  mathoperationsqty,
        |  variablesqty,
        |  maxnestedblocksqty,
        |  anonymousclassesqty,
        |  innerclassesqty,
        |  lambdasqty,
        |  uniquewordsqty,
        |  modifiers,
        |  logstatementsqty)
        |values (
        |  ?, ?, ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?,
        |  ?, ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |  ?, ?,
        |
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?
        |)
        |""".stripMargin).updateMany(pgClassesMetrics)
  private def saveMethodMetrics(pgMethodMetrics: List[PgMethodMetrics]): ConnectionIO[Int] =
    Update[PgMethodMetrics](
      """
        |insert into method_metrics
        | (repo_name, file, class, method,
        |  constructor,
        |  line,
        |  cbo, cboModified,
        |  fanin, fanout,
        |  wmc,
        |  rfc,
        |  loc,
        |  returnsQty,
        |  variablesQty,
        |  parametersQty,
        |  methodsInvokedQty, methodsInvokedLocalQty, methodsInvokedIndirectlyQty,
        |  loopQty,
        |  comparisonQty,
        |  tryCatchQty,
        |  parenthesizedExpsQty,
        |  stringLiteralsQty,
        |  numbersQty,
        |  assignmentsQty,
        |  mathOperationsQty,
        |  maxNestedBlocksQty,
        |  anonymousClassesQty,
        |  innerClassesQty,
        |  lambdasQty,
        |  uniqueWordsQty,
        |  modifiers,
        |  logStatementsQty,
        |  hasJavaDoc
        | )
        |values (
        |  ?, ?, ?, ?,
        |  ?,
        |  ?,
        |  ?, ?,
        |  ?, ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?, ?, ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?,
        |  ?
        |)
        |""".stripMargin).updateMany(pgMethodMetrics)

  private def saveVariableMetrics(
                                   tableName: String,
                                   variableMetrics: List[PgVariableMetrics]
                                 ): ConnectionIO[Int] =
    Update[PgVariableMetrics](
      s"""
        |insert into $tableName
        | (repo_name, file, class, method, variable, usage)
        |values
        | (?, ?, ?, ?, ?, ?)
        |""".stripMargin
    ).updateMany(variableMetrics)
}