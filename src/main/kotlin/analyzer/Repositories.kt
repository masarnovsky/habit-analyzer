package analyzer

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DayRecordRepository : JpaRepository<DayRecord, Long>

@Repository
interface WeekRecordRepository : JpaRepository<WeekRecord, Long>