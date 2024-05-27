import com.github.mauricioaniche.ck.{CKClassResult, CKMethodResult}

case class PgMethodMetrics(
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