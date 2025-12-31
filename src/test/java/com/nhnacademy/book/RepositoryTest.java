package com.nhnacademy.book;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.lang.annotation.*;

@Target(ElementType.TYPE) // 클래스 레벨에 붙일 수 있도록 설정
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 유지되어야 Spring이 읽을 수 있음
@Documented
@Inherited
@DataJpaTest
public @interface RepositoryTest {
}
