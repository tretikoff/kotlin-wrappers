package kotlinx.css

object Statistics {
    private val st = HashMap<Pair<String, String>, Int>()
    private val rulesSB = StringBuilder()

    fun add(rules: LinkedHashMap<String, Any>) {
        rulesSB.append(rules.toString())
    }

    fun getRulesString(): String {
        return rulesSB.toString()
    }

    fun add(rule1: String, rule2: String) {
        if (rule1 == rule2) return
        val p: Pair<String, String> = Pair(rule1, rule2)
        val invp: Pair<String, String> = Pair(rule2, rule1)
        when {
            st.containsKey(p) -> {
                st[p] = st[p]!!.plus(1)
            }
            st.containsKey(invp) -> {
                st[invp] = st[invp]!!.plus(1)
            }
            else -> {
                st[p] = 1
            }
        }
    }
}