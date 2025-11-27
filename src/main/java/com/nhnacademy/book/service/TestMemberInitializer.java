package com.nhnacademy.book.service; // 또는 parser

import com.nhnacademy.book.entity.Member;
import com.nhnacademy.book.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestMemberInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 테스트용 ID (1번)이 존재하는지 먼저 확인
        if (memberRepository.count() == 0) {
            // 테스트용 회원 생성 (ID는 1번이 자동 생성됨)
            Member testUser = new Member();
            testUser.setName("테스트사용자1");
            testUser.setTotalPoints(1000);

            memberRepository.save(testUser);
            // H2의 @GeneratedValue(IDENTITY) 특성상
            // 첫 번째 저장 객체의 ID는 1번이 자동 생성
        }
    }
}