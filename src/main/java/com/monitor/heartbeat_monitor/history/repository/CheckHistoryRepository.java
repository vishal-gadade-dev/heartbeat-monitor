package com.monitor.heartbeat_monitor.history.repository;

import com.monitor.heartbeat_monitor.history.model.CheckHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CheckHistoryRepository extends JpaRepository<CheckHistory, Long> {
    List<CheckHistory> findByCheckIdOrderByCreatedAtDesc(Long checkId);
    Page<CheckHistory> findByCheckIdOrderByCreatedAtDesc(Long checkId, Pageable pageable);
}
