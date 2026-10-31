package backend.hoctap.repository;

import backend.hoctap.entity.Option;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OptionRepository extends JpaRepository<Option, Long> {

    // ── Lấy đáp án theo câu hỏi ───────────────────────────────────────────

    List<Option> findByQuestionIdOrderByOptionIndexAsc(Long questionId);

    int countByQuestionId(Long questionId);

    // ── Xóa toàn bộ đáp án của một câu hỏi ───────────────────────────────

    void deleteByQuestionId(Long questionId);
}
