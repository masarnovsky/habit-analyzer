package analyzer

import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.streams.toList

const val ONE_MONTH_PATTER = "(?<from>[0-9]+)-(?<to>[0-9]+) (?<month>[A-Za-z]*)"
const val TWO_MONTH_PATTER = "(?<from>[0-9]+) (?<monthFrom>[A-Za-z]*) - (?<to>[0-9]+) (?<monthTo>[A-Za-z]*)"
const val HABIT_TITLE = "(?<habit>[A-Za-z0-9 ]*)\\((?<desired>[0-9]+)\\)"
const val YES = "Yes"

@Service
class Parser(
    private val dayRecordRepository: DayRecordRepository,
    private val weekRepository: WeekRecordRepository
) {

    fun parseCsvFileAndStoreData() {
        val files = getAllFilesInResources()

        files.map { parseFile(it) }.map { habitRecord ->
            saveWeekRecords(habitRecord.values.map { it.first }.toList())
            saveDailyRecords(habitRecord.values.map { it.second }.toList())
        }
    }

    fun saveDailyRecords(records: List<List<DayRecord>>) {
        records.forEach { dayRecordRepository.saveAll(it) }
    }

    fun saveWeekRecords(records: List<WeekRecord>) {
        weekRepository.saveAll(records)
    }

    fun getAllFilesInResources(): List<Path> {
        val projectDirAbsolutePath = Paths.get("").toAbsolutePath().toString()
        val resourcesPath = Paths.get(projectDirAbsolutePath, "/src/main/resources/files")
        return Files.walk(resourcesPath)
            .filter { item -> Files.isRegularFile(item) }
            .filter { item -> item.toString().endsWith(".csv") }
            .toList()
    }

    fun parseFile(path: Path): MutableMap<String, Pair<WeekRecord, List<DayRecord>>> {
        val fileName = path.fileName
        println("parse $fileName")

        val range = fileName.name.dropLast(4)

        val (fromDate, toDate) = parseDateRange(range)
        val records = File(path.pathString).useLines { it.toList() }.map {
            it.split(",").filter { value -> value.isNotBlank() && value != "status" }
        }.toList()

        return processRecords(range, fromDate, toDate, records)
    }

    fun processRecords(
        range: String,
        fromDate: LocalDate,
        toDate: LocalDate,
        records: List<List<String>>
    ): MutableMap<String, Pair<WeekRecord, List<DayRecord>>> {
        val habitsMap = mutableMapOf<String, Pair<WeekRecord, List<DayRecord>>>()
        val columns = records[0].size
        for (i in 1 until columns) {
            var currentDate = fromDate
            val week = WeekRecord(range = range, from = fromDate, to = toDate)
            val days = mutableListOf<DayRecord>()
            for (j in records.indices) {
                val value = records[j][i]
                if (j == 0) {
                    parseHeaderColumn(value, week)
                } else {
                    val day = parseHabitColumn(week.habit, value, currentDate)
                    days.add(day)

                    currentDate = currentDate.plusDays(1)
                }
            }
            week.actual = days.count { it.isDone }

            if (toDate != currentDate.minusDays(1)) throw RuntimeException("$toDate != $currentDate for range $range")
            habitsMap[week.habit!!] = Pair(week, days)
        }

        return habitsMap
    }

    private fun parseHabitColumn(habit: String?, value: String, currentDate: LocalDate): DayRecord {
        val day = DayRecord()
        day.habit = habit
        if (value == YES) {
            day.isDone = true
        }
        day.date = currentDate
        day.dayOfWeek = currentDate.dayOfWeek.name

        return day
    }

    private fun parseHeaderColumn(value: String, week: WeekRecord) {
        println(value)
        val tryHabit = Regex(HABIT_TITLE).find(value)!!
        val (habit, desired) = tryHabit.destructured
        week.habit = habit
        week.desired = desired.toInt()
    }

    fun parseDateRange(range: String): Pair<LocalDate, LocalDate> {
        val firstTry = Regex(ONE_MONTH_PATTER).find(range)

        if (firstTry != null) {
            val (from, to, month) = firstTry.destructured

            val fromRangeString = "$from $month 2021"
            val toRangeString = "$to $month 2021"
            val fromRange = LocalDate.parse(fromRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))
            val toRange = LocalDate.parse(toRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))

            return Pair(fromRange, toRange)
        } else {
            val secondTry =
                Regex(TWO_MONTH_PATTER).find(range) ?: throw RuntimeException("Can't parse date range $range")

            val (from, monthFrom, to, monthTo) = secondTry.destructured

            val fromRangeString = "$from $monthFrom 2021"
            val toRangeString = "$to $monthTo 2021"
            val fromRange = LocalDate.parse(fromRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))
            val toRange = LocalDate.parse(toRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))

            return Pair(fromRange, toRange)
        }
    }
}