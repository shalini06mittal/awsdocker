package com.neueda.leap.sprint6;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ClientMapper {

    @Select("""
        SELECT
            c.client_id AS clientId,
            c.name AS name,
            c.date_of_birth AS dateOfBirth,
            c.risk_profile AS riskProfile,
            c.advisor_id AS advisorId,
            c.joined_date AS joinedDate,
            a.name AS advisorName,
            a.region AS advisorRegion
        FROM clients c
        LEFT JOIN advisors a
            ON c.advisor_id = a.advisor_id
        ORDER BY c.client_id
        """)
    List<Client> findAll();

    @Select("""
        SELECT
            c.client_id AS clientId,
            c.name AS name,
            c.date_of_birth AS dateOfBirth,
            c.risk_profile AS riskProfile,
            c.advisor_id AS advisorId,
            c.joined_date AS joinedDate,
            a.name AS advisorName,
            a.region AS advisorRegion
        FROM clients c
        LEFT JOIN advisors a
            ON c.advisor_id = a.advisor_id
        WHERE c.client_id = #{clientId}
        """)
    Optional<Client> findById(Integer clientId);
}