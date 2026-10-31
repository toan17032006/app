package backend.hoctap.repository;

import backend.hoctap.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    // ── Lọc theo môn học ──────────────────────────────────────────────────

    List<Exam> findByCategory(String category);

    List<Exam> findByCategoryOrderByCreatedAtDesc(String category);

    // ── Tìm kiếm theo tiêu đề (LIKE) ──────────────────────────────────────

    List<Exam> findByTitleContainingIgnoreCase(String keyword);

    // ── Tìm kèm câu hỏi (tránh N+1) ──────────────────────────────────────
    // Lưu ý: KHÔNG join fetch e.questions + q.options cùng lúc — Hibernate
    // ném MultipleBagFetchException khi fetch 2 collection (bag) trong 1 query.
    // Options được load riêng qua OptionRepository khi cần.
    @Query("""
            SELECT DISTINCT e FROM Exam e
            LEFT JOIN FETCH e.questions
            WHERE e.id = :id
            """)
    java.util.Optional<Exam> findByIdWithQuestionsAndOptions(@Param("id") Long id);

    // ── Danh sách môn học phân biệt ───────────────────────────────────────

    @Query("SELECT DISTINCT e.category FROM Exam e ORDER BY e.category")
    List<String> findAllCategories();
}
