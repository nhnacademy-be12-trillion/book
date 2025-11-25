package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}