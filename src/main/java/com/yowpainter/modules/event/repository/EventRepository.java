package com.yowpainter.modules.event.repository;

import com.yowpainter.modules.event.entity.Event;
import com.yowpainter.modules.event.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findByArtistId(UUID artistId);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.startDateTime > :now ORDER BY e.startDateTime ASC")
    List<Event> findUpcomingEvents(@Param("now") LocalDateTime now);

    @Query("SELECT e FROM Event e WHERE (LOWER(e.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(e.location) LIKE LOWER(CONCAT('%', :q, '%'))) AND e.status = 'PUBLISHED'")
    List<Event> searchPublicEvents(@Param("q") String q);

    @Query("SELECT DISTINCT e.location FROM Event e WHERE e.location IS NOT NULL")
    List<String> findDistinctLocations();

    long countByStatusAndStartDateTimeAfter(EventStatus status, LocalDateTime now);
}
