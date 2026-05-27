package com.homework.system.repository;

import com.homework.system.dto.SubmissionCheckSnapshot;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class SubmissionCheckRepository {

    private final JdbcTemplate jdbcTemplate;

    public SubmissionCheckRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void refreshAssignment(Long assignmentId, Long classId) {
        jdbcTemplate.update("""
                insert into submission_check_status(
                    assignment_id, class_id, student_id, has_submitted, final_submission_id, checked_at
                )
                select ?, ?, u.id,
                       case when s.id is null then 0 else 1 end,
                       s.id,
                       now()
                from class_member cm
                join sys_user u on u.id = cm.user_id
                left join submission s
                       on s.assignment_id = ?
                      and s.student_id = u.id
                      and s.is_final = 1
                where cm.class_id = ?
                  and u.role = 'STUDENT'
                on duplicate key update
                    class_id = values(class_id),
                    has_submitted = values(has_submitted),
                    final_submission_id = values(final_submission_id),
                    checked_at = values(checked_at)
                """, assignmentId, classId, assignmentId, classId);

        jdbcTemplate.update("""
                delete scs
                from submission_check_status scs
                left join class_member cm
                       on cm.class_id = ?
                      and cm.user_id = scs.student_id
                left join sys_user u
                       on u.id = scs.student_id
                      and u.role = 'STUDENT'
                where scs.assignment_id = ?
                  and (cm.id is null or u.id is null)
                """, classId, assignmentId);
    }

    public SubmissionCheckSnapshot findSnapshot(Long assignmentId) {
        return jdbcTemplate.queryForObject("""
                select count(*) as total_students,
                       coalesce(sum(case when has_submitted = 1 then 1 else 0 end), 0) as submitted_count,
                       max(checked_at) as checked_at
                from submission_check_status
                where assignment_id = ?
                """, (rs, rowNum) -> {
            int total = rs.getInt("total_students");
            int submitted = rs.getInt("submitted_count");
            int missing = Math.max(total - submitted, 0);
            int rate = total == 0 ? 0 : Math.round(submitted * 100f / total);
            Timestamp checkedAt = rs.getTimestamp("checked_at");
            return new SubmissionCheckSnapshot(
                    total,
                    submitted,
                    missing,
                    rate,
                    checkedAt == null ? null : checkedAt.toLocalDateTime()
            );
        }, assignmentId);
    }
}
