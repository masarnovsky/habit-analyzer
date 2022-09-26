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
const val currentYear = 2022

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
            it.split(",")
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

        records.drop(1)
            .map { record -> parseRecord(range, fromDate, toDate, record) }
            .map { data -> habitsMap.put(data.first, data.second)}
        return habitsMap
    }

    private fun parseRecord(range: String, fromDate: LocalDate, toDate: LocalDate, record: List<String>): Pair<String, Pair<WeekRecord, MutableList<DayRecord>>> {
        val habit = record[0]
        var currentDate = fromDate
        val week = WeekRecord(range = range, from = fromDate, to = toDate)
        processDesiredWeekAmount(habit, week)
        val days = mutableListOf<DayRecord>()
        for (i in 1 until record.size) {
            val day = DayRecord()
            day.habit = week.habit
            if (record[i] == YES) {
                day.isDone = true
            }
            day.date = currentDate
            day.dayOfWeek = currentDate.dayOfWeek.name
            days.add(day)
            currentDate = currentDate.plusDays(1)
        }

        week.actual = days.count { it.isDone }

        if (toDate != currentDate.minusDays(1)) throw RuntimeException("toDate != currentDate for range $range")
        return Pair(week.habit!!, Pair(week, days))
    }

    private fun processDesiredWeekAmount(value: String, week: WeekRecord) {
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

            val fromRangeString = "$from $month $currentYear"
            val toRangeString = "$to $month $currentYear"
            val fromRange = LocalDate.parse(fromRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))
            val toRange = LocalDate.parse(toRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))

            return Pair(fromRange, toRange)
        } else {
            val secondTry =
                Regex(TWO_MONTH_PATTER).find(range) ?: throw RuntimeException("Can't parse date range $range")

            val (from, monthFrom, to, monthTo) = secondTry.destructured

            val fromRangeString = "$from $monthFrom $currentYear"
            val toRangeString = "$to $monthTo $currentYear"
            val fromRange = LocalDate.parse(fromRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))
            val toRange = LocalDate.parse(toRangeString, DateTimeFormatter.ofPattern("dd MMM yyyy"))

            return Pair(fromRange, toRange)
        }
    }
}