import com.github.mauricioaniche.ck.{CKClassResult, CKMethodResult}

case class PgVariableMetrics(
                         repoName: String,
                         file: String,
                         className: String,
                         methodName: String,
                         variableName: String,
                         usage: Int
                       )

object PgVariableMetrics {
  def apply(
             repositoryName: String,
             classResult: CKClassResult,
             methodResult: CKMethodResult,
             variableName: String,
             usage: Int) =
    new PgVariableMetrics(
      repositoryName,
      classResult.getFile,
      classResult.getClassName,
      methodResult.getMethodName,
      variableName,
      usage)
}