package styled

import react.*

data class MemoizedResult(val args: dynamic) {
}

// like react styled.useEffect, but with Kotlin equality checking
fun useStructEffect(args: RDependenciesList, callback: () -> Unit): Unit =
    useEffect(
        effect = getMemoizedCallback(false, args, callback),
        dependencies = args
    )

// Memoize in Kotlin way to compare equality
internal fun getMemoizedCallback(
    checkByRef: Boolean,
    args: RDependenciesList,
    callback: () -> Unit
): () -> Unit {
    val prevResultRef = useRef<MemoizedResult?>(null)
    return {
        val prevResult = prevResultRef.current
        var index = -1
        console.log(prevResult?.args)
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
        if (!equal) {
            callback().also {
                prevResultRef.current = MemoizedResult(args)
            }
        }
    }
}
