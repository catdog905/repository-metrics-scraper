import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.implicits._
import com.github.mauricioaniche.ck.{CK, CKClassResult, CKMethodResult, CKNotifier}
import doobie.implicits._
import doobie.{ConnectionIO, Transactor, Update}

import scala.reflect.io.Path

object CKMetrics {
  def get(name: String, path: Path): ConnectionIO[Int] = {
    //"git clone git@github.com:mauricioaniche/ck.git".!
    var repoMetrics: List[CKClassResult] = List()
    new CK(false, 0, true).calculate(path.toString(), new CKNotifier() {
      override def notify(result: CKClassResult): Unit = {
        // Store the metrics values from each component of the project in a HashMap
        //results.put(result.getClassName, result)
        repoMetrics = result :: repoMetrics
      }

      override def notifyError(sourceFilePath: String, e: Exception): Unit = {
        System.err.println("Error in " + sourceFilePath)
        e.printStackTrace(System.err)
      }
    })
    saveClassesMetrics(for (value <- repoMetrics) yield PgClassMetrics(name, value))
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

  private def saveMethodMetrics(classMetrics: PgClassMetrics, pgMethodMetrics: List[PgMethodMetrics]): ConnectionIO[Int] =
}

case class PgClassMetrics(
                           repoName: String,
                           file: String,
                           className: String,
                           typeName: String,

                           cbo: Int,
                           cboModified: Int,
                           fanIn: Int,
                           fanOut: Int,
                           wmc: Int,
                           dit: Int,
                           noc: Int,
                           rfc: Int,
                           lcom: Int,
                           lcomStar: Double,
                           tcc: Double,
                           lcc: Double,

                           totalMethodsQty: Int,
                           staticMethodsQty: Int,
                           publicMethodsQty: Int,
                           privateMethodsQty: Int,
                           protectedMethodsQty: Int,
                           defaultMethodsQty: Int,
                           visibleMethodsQty: Int,
                           abstractMethodsQty: Int,
                           finalMethodsQty: Int,
                           synchronizedMethodsQty: Int,

                           totalFieldQty: Int,
                           staticFieldQty: Int,
                           publicFieldQty: Int,
                           privateFieldQty: Int,
                           protectedFieldQty: Int,
                           defaultFieldQty: Int,
                           finalFieldQty: Int,
                           synchronizedFieldQty: Int,

                           nosi: Int,
                           loc: Int,
                           returnQty: Int,
                           loopQty: Int,
                           comparisonQry: Int,
                           tryCatchQty: Int,
                           parenthesizedExpsQty: Int,
                           stringLiteralsQty: Int,
                           numbersQty: Int,
                           assignmentsQty: Int,
                           mathOperationsQty: Int,
                           variableQty: Int,
                           maxNestedBlocksQty: Int,
                           anonymousClassesQty: Int,
                           innerClassesQty: Int,
                           lambdasQty: Int,
                           uniqueWordsQty: Int,
                           modifiers: Int,
                           logStatementsQty: Int
                         )

object PgClassMetrics {
  def apply(repositoryName: String, metrics: CKClassResult) = new PgClassMetrics(
    repositoryName, metrics.getFile, metrics.getClassName, metrics.getType,
    metrics.getCbo, metrics.getCboModified,
    metrics.getFanin, metrics.getFanout,
    metrics.getWmc,
    metrics.getDit, metrics.getNoc, metrics.getRfc,
    metrics.getLcom, metrics.getLcomNormalized,
    metrics.getTightClassCohesion, metrics.getLooseClassCohesion,
    metrics.getNumberOfMethods, metrics.getNumberOfStaticMethods,
    metrics.getNumberOfPublicMethods, metrics.getNumberOfPrivateMethods,
    metrics.getNumberOfProtectedMethods, metrics.getNumberOfDefaultMethods,
    metrics.getVisibleMethods.size, metrics.getNumberOfAbstractMethods,
    metrics.getNumberOfFinalMethods, metrics.getNumberOfSynchronizedMethods,

    metrics.getNumberOfFields, metrics.getNumberOfStaticFields,
    metrics.getNumberOfPublicFields, metrics.getNumberOfPrivateFields,
    metrics.getNumberOfProtectedFields, metrics.getNumberOfDefaultFields,
    metrics.getNumberOfFinalFields, metrics.getNumberOfSynchronizedFields,

    metrics.getNosi,
    metrics.getLoc,
    metrics.getReturnQty,
    metrics.getLoopQty,
    metrics.getComparisonsQty,
    metrics.getTryCatchQty,
    metrics.getParenthesizedExpsQty,
    metrics.getStringLiteralsQty,
    metrics.getNumbersQty,
    metrics.getAssignmentsQty,
    metrics.getMathOperationsQty,
    metrics.getVariablesQty,
    metrics.getMaxNestedBlocks,
    metrics.getAnonymousClassesQty,
    metrics.getInnerClassesQty,
    metrics.getLambdasQty,
    metrics.getUniqueWordsQty,
    metrics.getModifiers,
    metrics.getNumberOfLogStatements
  )
}

case class PgMethodMetrics (
                           repoName: String,
                           file: String,
                           className: String,
                           methodName: String,
                           isConstructor: Boolean,
                           startLine: Int,
                           cbo: Int,
                           cboModified: Int,
                           fanIn: Int,
                           fanOut: Int,
                           wmc: Int,
                           rfc: Int,
                           loc: Int,
                           returnQty: Int,
                           variablesQty: Int,
                           parametersQty: Int,
                           methodInvocationsQty: Int,
                           methodInvocationsLocalQty: Int,
                           methodInvocationsIndirectLocalQty: Int,
                           loopQty: Int,
                           comparisonQty: Int,
                           tryCatchQty: Int,
                           parenthesizedExpsQty: Int,
                           stringLiteralsQty: Int,
                           numbersQty: Int,
                           assignmentsQty: Int,
                           mathOperationsQty: Int,
                           maxNestedBlocks: Int,
                           anonymousClassesQty: Int,
                           innerClassesQty: Int,
                           lambdasQty: Int,
                           UniqueWordsQty: Int,
                           modifiersQty: Int,
                           logStatementsQty: Int,
                           hasJavaDoc: Boolean
                           )

object PgMethodMetrics {
  def apply(repositoryName: String, classResult: CKClassResult, methodResult: CKMethodResult) = new PgMethodMetrics(
    repositoryName,
    classResult.getFile,
    classResult.getClassName,
    methodResult.getMethodName,
    methodResult.isConstructor,
    methodResult.getStartLine,
    methodResult.getCbo,
    methodResult.getCboModified,
    methodResult.getFanin,
    methodResult.getFanout,
    methodResult.getWmc,
    methodResult.getRfc,
    methodResult.getLoc,
    methodResult.getReturnQty,
    methodResult.getVariablesQty,
    methodResult.getParametersQty,
    methodResult.getMethodInvocations.size,
    methodResult.getMethodInvocationsLocal.size,
    methodResult.getMethodInvocationsIndirectLocal.size,
    methodResult.getLoopQty,
    methodResult.getComparisonsQty,
    methodResult.getTryCatchQty,
    methodResult.getParenthesizedExpsQty,
    methodResult.getStringLiteralsQty,
    methodResult.getNumbersQty,
    methodResult.getAssignmentsQty,
    methodResult.getMathOperationsQty,
    methodResult.getMaxNestedBlocks,
    methodResult.getAnonymousClassesQty,
    methodResult.getInnerClassesQty,
    methodResult.getLambdasQty,
    methodResult.getUniqueWordsQty,
    methodResult.getModifiers,
    methodResult.getLogStatementsQty,
    methodResult.getHasJavadoc
  )
}


object Main {
  def main(args: Array[String]): Unit = {
    val xa = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://master.f94318ce-b33a-4cdb-a291-6a10fd934f15.c.dbaas.selcloud.ru:5432/metrics",
      user = "yaroslav",
      password = "",
      logHandler = None
    )
    CKMetrics.get("ck", Path.apply("ck")).transact(xa).unsafeRunSync()
  }
}
