package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.notice.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByTitleContainingIgnoreCase(String keyword);
    List<Notice> findByContentContainingIgnoreCase(String keyword);
}
