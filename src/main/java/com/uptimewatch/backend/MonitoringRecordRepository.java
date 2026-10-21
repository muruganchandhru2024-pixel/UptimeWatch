
package com.uptimewatch.backend;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitoringRecordRepository
        extends JpaRepository<MonitoringRecord, Long> {

    List<MonitoringRecord> findTop20ByOrderByCheckedAtDesc();
}
