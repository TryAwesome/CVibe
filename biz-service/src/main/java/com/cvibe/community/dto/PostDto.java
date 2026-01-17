package com.cvibe.community.dto;

import com.cvibe.auth.entity.User;
import com.cvibe.community.entity.Post;
import com.cvibe.community.entity.PostCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * DTO for Post response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {

    private String id;
    private AuthorDto author;
    private String content;
    private List<String> images;
    private List<String> tags;
    private PostCategory category;
    private Integer likesCount;
    private Integer commentsCount;
    private Boolean liked;
    private String createdAt;
    private String updatedAt;

    /**
     * Inner class for author information.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthorDto {
        private String id;
        private String nickname;
        private String avatarUrl;

        public static AuthorDto fromUser(User user) {
            if (user == null) {
                return null;
            }
            return AuthorDto.builder()
                    .id(user.getId().toString())
                    .nickname(user.getNickname())
                    .avatarUrl(user.getAvatarUrl())
                    .build();
        }
    }

    /**
     * Create PostDto from Post entity.
     */
    public static PostDto fromEntity(Post post) {
        return fromEntity(post, false);
    }

    /**
     * Create PostDto from Post entity with liked status.
     */
    public static PostDto fromEntity(Post post, boolean liked) {
        if (post == null) {
            return null;
        }
        return PostDto.builder()
                .id(post.getId().toString())
                .author(AuthorDto.fromUser(post.getAuthor()))
                .content(post.getContent())
                .images(parseJsonArray(post.getImages()))
                .tags(parseJsonArray(post.getTags()))
                .category(post.getCategory())
                .likesCount(post.getLikesCount())
                .commentsCount(post.getCommentsCount())
                .liked(liked)
                .createdAt(post.getCreatedAt() != null ? post.getCreatedAt().toString() : null)
                .updatedAt(post.getUpdatedAt() != null ? post.getUpdatedAt().toString() : null)
                .build();
    }

    private static List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
