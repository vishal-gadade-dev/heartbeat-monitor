package com.monitor.heartbeat_monitor.alert.repository;
import com.monitor.heartbeat_monitor.alert.enums.AlertStatus;
import com.monitor.heartbeat_monitor.alert.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByStatusAndNextRetryAtLessThanEqual(AlertStatus status, LocalDateTime now, Pageable pageable);
    Page<Alert> findByStatus(AlertStatus status, Pageable pageable);
}
