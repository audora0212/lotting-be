package com.audora.lotting_be.service;

import com.audora.lotting_be.model.notice.Notice;
import com.audora.lotting_be.repository.NoticeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class NoticeService {

    @Autowired
    private NoticeRepository noticeRepository;

    public Notice createNotice(Notice notice) {
        return noticeRepository.save(notice);
    }

    public Notice updateNotice(Long id, Notice updatedNotice) {
        Optional<Notice> optionalNotice = noticeRepository.findById(id);
        if (optionalNotice.isPresent()) {
            Notice notice = optionalNotice.get();
            notice.setTitle(updatedNotice.getTitle());
            notice.setContent(updatedNotice.getContent());
            return noticeRepository.save(notice);
        }
        throw new RuntimeException("Notice not found with id: " + id);
    }

    public void deleteNotice(Long id) {
        noticeRepository.deleteById(id);
    }

    public Optional<Notice> getNoticeById(Long id) {
        return noticeRepository.findById(id);
    }

    public List<Notice> searchNotices(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return noticeRepository.findAll();
        }
        List<Notice> byTitle = noticeRepository.findByTitleContainingIgnoreCase(keyword);
        List<Notice> byContent = noticeRepository.findByContentContainingIgnoreCase(keyword);
        byTitle.addAll(byContent);
        return byTitle.stream().distinct().toList();
    }

    public List<Notice> getAllNotices(){
        return noticeRepository.findAll();
    }
}
