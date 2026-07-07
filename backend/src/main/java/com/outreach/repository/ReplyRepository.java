package com.outreach.repository;

import com.outreach.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Long> {

    boolean existsByMessageId(String messageId);

    long countByUserIdAndClassification(Long userId, Reply.ReplyClassification classification);
}
