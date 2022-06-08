package pers.sy.sqloj.common

object Configs {
    val password: String = System.getenv("SQLOJ_PASSWORD") ?: "123456"
}
