// Enum utility from
// http://stackoverflow.com/questions/1898932/case-classes-vs-enumerations-in-scala/4958905#4958905

package com.ometer

/* 
 * Use like this:
 * sealed trait Currency extends Currency.Value
 * object Currency extends CaseEnum[Currency] {
 *    case object EUR extends Currency
 *    case object GBP extends Currency
 *    val values = List(EUR, GBP)
 * }
 *  
 * As discussed on stack overflow, the manual list of values is needed
 * because objects initialize lazily. It could be fixed using reflection
 * to force-initialize but let's not go there for now.
 */

trait CaseEnum[A] {
    trait Value { self : A => }
    val values : List[A]
}
