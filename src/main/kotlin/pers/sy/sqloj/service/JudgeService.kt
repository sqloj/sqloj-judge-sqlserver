package pers.sy.sqloj.service

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.sql.ResultSetMetaData
import java.sql.Statement

val prepared = """
IF NOT EXISTS
    (SELECT * FROM sys.databases WHERE [name] = 'temp')
    CREATE DATABASE temp;
"""

@Service
class JudgeService
@Autowired constructor(
    val jdbcTemplate: JdbcTemplate
) {
    fun exec(statement: String): List<Any>? {
        System.err.println("[LOG] exec: statement = $statement")
        return jdbcTemplate.execute { stmt: Statement ->
            val ret = stmt.execute(statement)
            if (!ret) {
                null
            } else {
                val rs = stmt.resultSet
                val rsmd: ResultSetMetaData = rs.metaData
                val count: Int = rsmd.columnCount
                val list: MutableList<Map<*, *>> =
                    ArrayList()
                while (rs.next()) {
                    val row: MutableMap<String, Any> = HashMap()
                    for (i in 1..count) {
                        row[rsmd.getColumnName(i)] = rs.getObject(i)
                    }
                    list.add(row)
                }
                list
            }
        }
    }

    fun execWithoutRet(statement: String) {
        System.err.println("[LOG] execWithoutRet: statement = $statement")
        jdbcTemplate.execute(statement)
    }

    fun judge(sql: String, tmpDB: String): Any? {
        execWithoutRet(prepared)
        execWithoutRet("create database ${tmpDB} COLLATE Chinese_PRC_CI_AS;")
        execWithoutRet("use ${tmpDB};")
        val regex = Regex("""(--.*\n|#.*\n|\/\*(.|\r\n|\n)*\*\/)""")
        val list = regex.replace(sql, "").split(";")
        val ret: MutableList<Any?> = ArrayList()
        try {
            for (l in list) {
                if (l.isBlank()) {
                    continue
                }
                val tmp = exec(l)
                if (tmp != null && tmp.isNotEmpty()) {
                    ret.add(tmp)
                }
            }
        } finally {
            execWithoutRet("use temp;")
            execWithoutRet("drop database ${tmpDB};")
        }
        return ret
    }
}