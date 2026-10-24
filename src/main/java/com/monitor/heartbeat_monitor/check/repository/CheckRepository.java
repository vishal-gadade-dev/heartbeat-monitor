package com.monitor.heartbeat_monitor.check.repository;

import com.monitor.heartbeat_monitor.check.enums.CheckKind;
import com.monitor.heartbeat_monitor.check.enums.CheckStatus;
import com.monitor.heartbeat_monitor.check.model.Check;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface CheckRepository extends JpaRepository<Check, Long> {

    Optional<Check> findBySecretToken(String secretToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Check c WHERE c.secretToken = :secretToken")
    Optional<Check> findBySecretTokenWithLock(@Param("secretToken") String secretToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Check c WHERE c.id = :id")
    Optional<Check> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT c FROM Check c WHERE c.kind = :kind AND c.status = :status AND c.nextExpectedAt <= :now ORDER BY c.nextExpectedAt ASC")
    List<Check> findDueChecksByKindAndStatus(
            @Param("kind") CheckKind kind,
            @Param("status") CheckStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    default List<Check> findDueChecksForSweep(LocalDateTime now) {

        final int SWEEP_PAGE_SIZE = 2000;
        Pageable limit = PageRequest.of(0, SWEEP_PAGE_SIZE);
        List<Check> due = new ArrayList<>();
        due.addAll(findDueChecksByKindAndStatus(CheckKind.HEARTBEAT, CheckStatus.UP, now, limit));
        due.addAll(findDueChecksByKindAndStatus(CheckKind.HEARTBEAT, CheckStatus.LATE, now, limit));
        due.addAll(findDueChecksByKindAndStatus(CheckKind.WEBSITE, CheckStatus.UP, now, limit));
        due.addAll(findDueChecksByKindAndStatus(CheckKind.WEBSITE, CheckStatus.DOWN, now, limit));
        return due;
    }

    Page<Check> findByStatus(CheckStatus status, Pageable pageable);
}
