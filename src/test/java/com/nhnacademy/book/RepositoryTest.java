package com.nhnacademy.book;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@Target(ElementType.TYPE) // 클래스 레벨에 붙일 수 있도록 설정
@Retention(RetentionPolicy.RUNTIME) // 런타임까지 유지되어야 Spring이 읽을 수 있음
@Documented
@Inherited
@DataJpaTest
public @interface RepositoryTest {
}
