package com.fronzec.frbatchservice

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Shared
import spock.lang.Specification

import java.sql.ResultSet
import java.sql.Statement

// Note:: we need to import the test containers annotation coming from spock package
@Testcontainers
class MySQLContainerIntegrationTest extends Specification {

    //Note the image version used is compatible with ARM64, some images doesn't have an available compatible version, we can choose other
    // version or enable in docker for MAC the emulation for x86_64 with QEMU but sometimes doesn't work very well
    @Shared
    MySQLContainer mySQLContainer = new MySQLContainer(DockerImageName.parse("mysql:8.0.40"))
    .withDatabaseName("testdb")
    .withUsername("theuser")
    .withPassword("thepassword")

    def "waits until database accepts jdbc connections"() {

        given: "a jdbc connection"
        println "creating datasource..."
        HikariConfig hikariConfig = new HikariConfig()
        hikariConfig.setJdbcUrl(mySQLContainer.jdbcUrl)
        hikariConfig.setUsername("theuser")
        hikariConfig.setPassword("thepassword")
        HikariDataSource ds = new HikariDataSource(hikariConfig)

        when: "querying the database"
        println "querying the database"
        Statement statement = ds.getConnection().createStatement()
        statement.execute("SELECT 1")
        ResultSet resultSet = statement.getResultSet()
        resultSet.next()

        then: "result is returned"
        int resultSetInt = resultSet.getInt(1)
        resultSetInt == 1

        cleanup:
        ds.close()
    }
}
