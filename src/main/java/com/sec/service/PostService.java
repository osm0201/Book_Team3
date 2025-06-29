package com.sec.service;

import com.sec.dto.PostCreateRequest;
import com.sec.dto.PostResponse;
import com.sec.dto.PostSearchCondition;
import com.sec.entity.Member;
import com.sec.entity.Post;
import com.sec.entity.Tag;
import com.sec.repository.MemberRepository;
import com.sec.repository.PostRepository;
import com.sec.repository.TagRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final TagRepository tagRepository;

    @Transactional
    public int createPost(PostCreateRequest request, int memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Post post = new Post();
        post.setTitle(request.getTitle());
        post.setContent(request.getContent());
        post.setMember(member);
        post.setCreatedAt(LocalDateTime.now());

        Set<Tag> tagSet = request.getTagIds().stream()
                .map(id -> tagRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그 ID입니다: " + id)))
                .collect(Collectors.toSet());
        post.setTags(tagSet);

        postRepository.save(post);
        return post.getPostId();
    }

    @Transactional
    public void updatePost(int postId, PostCreateRequest request, int memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        if (!Objects.equals(post.getMember().getMemberId(), memberId)) {
            throw new SecurityException("본인의 게시글만 수정할 수 있습니다.");
        }

        post.setTitle(request.getTitle());
        post.setContent(request.getContent());

        Set<Tag> updatedTags = request.getTagIds().stream()
                .map(id -> tagRepository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그 ID입니다: " + id)))
                .collect(Collectors.toSet());
        post.setTags(updatedTags);
    }

    @Transactional
    public void deletePost(int postId, int memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));
        if (!Objects.equals(post.getMember().getMemberId(), memberId)) {
            throw new SecurityException("본인의 게시글만 삭제할 수 있습니다.");
        }
        postRepository.delete(post);
    }

    @Transactional(readOnly = true)
    public PostResponse getPost(int postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        return PostResponse.from(post); // DTO 변환
    }
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        return postRepository.findAll().stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public List<PostResponse> searchPosts(String keyword) {
        List<Post> byContent = postRepository.findByContentContaining(keyword);
        List<Post> byTag = postRepository.findByTagsNameContaining(keyword);

        Set<Post> result = new HashSet<>();
        result.addAll(byContent);
        result.addAll(byTag);

        return result.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public Page<PostResponse> getPostsByCondition(PostSearchCondition condition, Pageable pageable) {
        Page<Post> result = postRepository.findAll((root, query, cb) -> {
            query.distinct(true);

            List<Predicate> predicates = new ArrayList<>();

            if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
                Predicate title = cb.like(cb.lower(root.get("title")), "%" + condition.getKeyword().toLowerCase() + "%");
                Predicate content = cb.like(root.get("content"), "%" + condition.getKeyword() + "%");
                predicates.add(cb.or(title, content));
            }

            if (condition.getTag() != null && !condition.getTag().isBlank()) {
                Join<Object, Object> tags = root.join("tags", JoinType.LEFT);
                predicates.add(cb.equal(tags.get("name"), condition.getTag()));
            }

            if (condition.getIsSolved() != null) {
                predicates.add(cb.equal(root.get("isSolved"), condition.getIsSolved()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        }, pageable);

        return result.map(PostResponse::from);
    }

}
