package com.audora.lotting_be.repository;

import com.audora.lotting_be.model.notice.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 제목이나 내용에 keyword가 포함된 공지사항 검색
    List<Notice> findByTitleContainingIgnoreCase(String keyword);
    List<Notice> findByContentContainingIgnoreCase(String keyword);
}
