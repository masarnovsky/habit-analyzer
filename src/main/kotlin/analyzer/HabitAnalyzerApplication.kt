package analyzer

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableAutoConfiguration
@EnableJpaRepositories
open class HabitAnalyzerApplication

fun main(args: Array<String>) {
    val context = runApplication<HabitAnalyzerApplication>(*args)

    context.getBean(Parser::class.java).parseCsvFileAndStoreData()
}