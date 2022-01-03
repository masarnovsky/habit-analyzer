package analyzer

import java.time.LocalDate
import javax.persistence.*

@Entity
data class DayRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var habit: String? = null,
    var date: LocalDate? = null,

    @Column(name = "dayOfWeek")
    var dayOfWeek: String? = null,

    @Column(name = "isDone")
    var isDone: Boolean = false,
)

@Entity
data class WeekRecord(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "week_range")
    val range: String,

    @Column(name = "from_date")
    val from: LocalDate,

    @Column(name = "to_date")
    val to: LocalDate,
    var habit: String? = null,
    var desired: Int? = null,
    var actual: Int = 0,
)