package mobi.lab.labencryptedstorage.internal

/**
 * when statement only creates a compiler error for sealed classes when used as an expression.
 * Use this .exhaustive extensions in other cases to get the compiler error when not all available options are defined.
 *
 * sealed class A
 *
 * class B : A()
 * class C : A()
 *
 * // Compiler error
 * val y = when(x) {
 *   is B ->
 * }
 *
 * // No compiler error
 * when(x) {
 *   is B ->
 * }
 *
 * // Compiler error
 * when(x) {
 *   is B ->
 * }.exhaustive
 */
internal val <T> T.exhaustive: T
    get() = this
