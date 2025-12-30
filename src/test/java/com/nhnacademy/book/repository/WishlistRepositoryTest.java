package com.nhnacademy.book.repository;

import com.nhnacademy.book.entity.Book;
import com.nhnacademy.book.entity.Wishlist;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class WishlistRepositoryTest {

    @Autowired
    private WishlistRepository wishlistRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("위시리스트 저장 및 조회 성공")
    void saveAndFindWishlist() {
        // given
        Book book = createAndPersistBook();
        Long memberId = 123L;

        Wishlist wishlist = Wishlist.create(memberId, book);

        // when
        Wishlist savedWishlist = wishlistRepository.save(wishlist);

        // then
        assertThat(savedWishlist.getId()).isNotNull();
        assertThat(savedWishlist.getMemberId()).isEqualTo(memberId);
        assertThat(savedWishlist.getBook()).isEqualTo(book);
    }

    @Test
    @DisplayName("중복 위시리스트 저장 시 예외 발생 (Unique Constraint)")
    void saveDuplicateWishlist_ShouldThrowException() {
        // given
        Book book = createAndPersistBook();
        Long memberId = 1L;

        Wishlist wishlist1 = Wishlist.create(memberId, book);
        wishlistRepository.save(wishlist1);

        // when & then
        // 같은 멤버가 같은 책을 또 찜하려고 함
        Wishlist wishlist2 = Wishlist.create(memberId, book);

        assertThatThrownBy(() -> wishlistRepository.saveAndFlush(wishlist2))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("회원 ID와 책으로 찜 여부 확인 (existsByMemberIdAndBook)")
    void existsByMemberIdAndBook() {
        // given
        Book book = createAndPersistBook();
        Long memberId = 50L;

        wishlistRepository.save(Wishlist.create(memberId, book));

        // when
        boolean exists = wishlistRepository.existsByMemberIdAndBook(memberId, book);
        boolean notExists = wishlistRepository.existsByMemberIdAndBook(999L, book); // 없는 회원

        // then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    @DisplayName("회원 ID와 책으로 위시리스트 엔티티 조회 (findByMemberIdAndBook)")
    void findByMemberIdAndBook() {
        // given
        Book book = createAndPersistBook();
        Long memberId = 77L;
        wishlistRepository.save(Wishlist.create(memberId, book));

        // when
        Optional<Wishlist> found = wishlistRepository.findByMemberIdAndBook(memberId, book);
        Optional<Wishlist> notFound = wishlistRepository.findByMemberIdAndBook(memberId, createAndPersistBook()); // 다른 책

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getMemberId()).isEqualTo(memberId);
        assertThat(notFound).isEmpty();
    }

    @Test
    @DisplayName("특정 회원의 모든 위시리스트 조회 (findByMemberId)")
    void findByMemberId() {
        // given
        Long targetMemberId = 100L;
        Book book1 = createAndPersistBook();
        Book book2 = createAndPersistBook();

        wishlistRepository.save(Wishlist.create(targetMemberId, book1));
        wishlistRepository.save(Wishlist.create(targetMemberId, book2));
        wishlistRepository.save(Wishlist.create(200L, book1)); // 다른 회원의 찜

        // when
        List<Wishlist> result = wishlistRepository.findByMemberId(targetMemberId);

        // then
        assertThat(result).hasSize(2); // 내 것만 2개 나와야 함
    }

    // --- Helper Method ---
    private Book createAndPersistBook() {
        Book book = Book.builder()
                .isbn(UUID.randomUUID().toString())
                .bookName("Test Book")
                .bookDescription("Desc")
                .bookStock(10)
                .bookRegularPrice(10000)
                .bookSalePrice(9000)
                .build();
        return entityManager.persist(book);
    }
}