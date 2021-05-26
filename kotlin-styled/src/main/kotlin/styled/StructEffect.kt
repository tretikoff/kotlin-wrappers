package styled

import react.*

data class MemoizedResult<T>(val args: dynamic, val value: T)

// like react styled.useMemo, but with Kotlin equality checking
fun <T> useStructMemo(vararg args: dynamic, callback: () -> T): T = useMemo(
    callback = getMemoizedCallback(false, args.toList(), callback),
    dependencies = args
)

// like react styled.useEffect, but with Kotlin equality checking
fun useStructEffect(args: RDependenciesList, callback: () -> Unit): Unit = useEffect(
    effect = getMemoizedCallback<Unit>(false, args, callback),
    dependencies = args
)

// Memoize in Kotlin way to compare equality
internal fun <T> getMemoizedCallback(
    checkByRef: Boolean,
    args: RDependenciesList,
    callback: () -> T
): () -> T {
    val prevResultRef = useRef<MemoizedResult<T>?>(null)
    return {
        val prevResult = prevResultRef.current
        var index = -1
        var equal = false
        if (prevResult != null) {
            equal = true
            for (prevArg: Any in prevResult.args) {
                index++
                val newArg: Any = args[index]
                equal = if (checkByRef) {
                    equal && prevArg === newArg
                } else equal && prevArg == newArg
            }
        }
        if (prevResult != null && equal) {
            prevResult.value
        } else {
            callback().also { result ->
                prevResultRef.current = MemoizedResult(args, result)
            }
        }
    }
}
