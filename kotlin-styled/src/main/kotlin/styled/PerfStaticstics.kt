package styled

fun MemoryInfo.getString(): String {
    val sb = StringBuilder("MemoryInfo {\n")
    sb.append("jsHeapSizeLimit: $jsHeapSizeLimit, ")
    sb.append("totalJSHeapSize: $totalJSHeapSize, ")
    sb.append("usedJSHeapSize: $usedJSHeapSize, ")
    sb.append("}\n")
    return sb.toString()
}

fun Performance.getString(): String {
    val sb = StringBuilder("Performance {\n")
    sb.append("eventsCount: $eventsCount, ")
    sb.append("memory: ${memory.getString()}, ")
    sb.append("timing: ${timing.getString()}, ")
    sb.append("timeOrigin: $timeOrigin, ")
    sb.append("}\n")
    return sb.toString()
}

fun PerformanceTiming.getString(): String {
    val sb = StringBuilder("PerformanceTiming {\n")
    sb.append("connectEnd: $connectEnd, ")
    sb.append("connectStart: $connectStart, ")
    sb.append("domComplete: $domComplete, ")
    sb.append("domContentLoadedEventEnd: $domContentLoadedEventEnd, ")
    sb.append("domContentLoadedEventStart: $domContentLoadedEventStart, ")
    sb.append("domInteractive: $domInteractive, ")
    sb.append("domLoading: $domLoading, ")
    sb.append("domainLookupEnd: $domainLookupEnd, ")
    sb.append("domainLookupStart: $domainLookupStart, ")
    sb.append("fetchStart: $fetchStart, ")
    sb.append("loadEventEnd: $loadEventEnd, ")
    sb.append("loadEventStart: $loadEventStart, ")
    sb.append("navigationStart: $navigationStart, ")
    sb.append("redirectEnd: $redirectEnd, ")
    sb.append("redirectStart: $redirectStart, ")
    sb.append("requestStart: $requestStart, ")
    sb.append("responseEnd: $responseEnd, ")
    sb.append("responseStart: $responseStart, ")
    sb.append("secureConnectionStart: $secureConnectionStart, ")
    sb.append("unloadEventEnd: $unloadEventEnd, ")
    sb.append("unloadEventStart: $unloadEventStart, ")
    sb.append("}\n")
    return sb.toString()
}

external interface Performance {
    val eventsCount: String
    val memory: MemoryInfo
    val timing: PerformanceTiming
    val timeOrigin: Double
}

external interface MemoryInfo {
    val jsHeapSizeLimit: Int
    val totalJSHeapSize: Int
    val usedJSHeapSize: Int
}

external interface PerformanceTiming {
    val connectEnd: Int
    val connectStart: Int
    val domComplete: Int
    val domContentLoadedEventEnd: Int
    val domContentLoadedEventStart: Int
    val domInteractive: Int
    val domLoading: Int
    val domainLookupEnd: Int
    val domainLookupStart: Int
    val fetchStart: Int
    val loadEventEnd: Int
    val loadEventStart: Int
    val navigationStart: Int
    val redirectEnd: Int
    val redirectStart: Int
    val requestStart: Int
    val responseEnd: Int
    val responseStart: Int
    val secureConnectionStart: Int
    val unloadEventEnd: Int
    val unloadEventStart: Int
}

class PerfStatistics {
    companion object {
        private val perfSB = StringBuilder()

        fun addMeasure(mark: String, perf: Performance) {
            perfSB.append(mark + ": { ${perf.getString()} }\n\n")
        }

        fun getPerformanceString(): String {
            return perfSB.toString()
        }
    }
}
