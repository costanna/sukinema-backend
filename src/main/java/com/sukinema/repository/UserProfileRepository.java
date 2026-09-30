package com.sukinema.repository;

import com.sukinema.model.Account;
import com.sukinema.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    List<UserProfile> findByAccountIdOrderByCreatedAtAscIdAsc(Long accountId);

    Optional<UserProfile> findByIdAndAccountId(Long id, Long accountId);

    long countByAccountId(Long accountId);

    boolean existsByAccountIdAndNameIgnoreCase(Long accountId, String name);

    boolean existsByAccountIdAndNameIgnoreCaseAndIdNot(Long accountId, String name, Long id);

    // Perfiles creados antes de que existieran las cuentas: pasan a la cuenta indicada
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE UserProfile p SET p.account = :account WHERE p.account IS NULL")
    int claimOrphans(@Param("account") Account account);

    // Al eliminar un tráiler, desaparece de las listas y de los likes de todos los perfiles
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM profile_my_list WHERE movie_id = :movieId", nativeQuery = true)
    void removeMovieFromAllLists(@Param("movieId") Long movieId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM profile_likes WHERE movie_id = :movieId", nativeQuery = true)
    void removeMovieFromAllLikes(@Param("movieId") Long movieId);
}
