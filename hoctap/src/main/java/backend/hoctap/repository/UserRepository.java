package backend.hoctap.repository;

import backend.hoctap.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ── Tìm kiếm ──────────────────────────────────────────────────────────

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmailAndAuthProvider(String email, User.AuthProvider authProvider);

    // ── Bảng xếp hạng (Leaderboard) ───────────────────────────────────────

    /** Top N học sinh theo xu tích lũy và GPA */
    @Query("SELECT u FROM User u ORDER BY u.coins DESC, u.gpa DESC")
    List<User> findLeaderboard();

    @Query("SELECT u FROM User u ORDER BY u.coins DESC, u.gpa DESC LIMIT :limit")
    List<User> findTopLeaderboard(@Param("limit") int limit);

    // ── Cập nhật xu ───────────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE User u SET u.coins = u.coins + :amount WHERE u.id = :userId")
    int addCoins(@Param("userId") Long userId, @Param("amount") int amount);

    // ── Tìm theo lớp ──────────────────────────────────────────────────────

    List<User> findByGrade(String grade);
}
