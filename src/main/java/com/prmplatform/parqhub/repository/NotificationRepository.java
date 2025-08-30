package com.prmplatform.parqhub.repository;

import com.prmplatform.parqhub.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdOrderByTimestampDesc(Long userId);
    List<Notification> findByAdminIdOrderByTimestampDesc(Long adminId);
}
