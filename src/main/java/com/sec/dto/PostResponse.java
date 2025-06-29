package com.sec.dto;

import com.sec.entity.Post;
import com.sec.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Builder
@NoArgsConstructor
@AllArgsConstructor
public @Data class PostResponse {
    private int postId;
    private String content;
    private String title;
    private boolean isSolved;
    private String nickname;
    private Set<String> tags;
    private LocalDateTime createdAt;
    private int viewCnt;
    private int memberId;

    public static PostResponse from(Post post) {
        Set<String> tagNames = post.getTags() == null ? Set.of() :
                post.getTags().stream()
                        .map(Tag::getName)
                        .collect(Collectors.toSet());

        return PostResponse.builder()
                .postId(post.getPostId())
                .title(post.getTitle())
                .content(post.getContent())
                .nickname(post.getMember().getNickname())
                .isSolved(Boolean.TRUE.equals(post.getIsSolved()))
                .tags(tagNames)
                .createdAt(post.getCreatedAt())
                .viewCnt(post.getView_cnt())
                .memberId(post.getMember().getMemberId())
                .build();
    }
}
