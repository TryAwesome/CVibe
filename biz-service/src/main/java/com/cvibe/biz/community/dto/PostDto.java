package com.cvibe.biz.community.dto;

import com.cvibe.biz.community.entity.Post;
import com.cvibe.biz.community.entity.Post.PostCategory;
import com.cvibe.biz.community.entity.Post.PostStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for Post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostDto {

    private UUID id;
    private String title;
    private String content;
    private PostCategory category;
    private List<String> tags;
    private String coverImageUrl;
    private PostStatus status;
    
    // Engagement stats
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Integer shareCount;
    
    // Flags
    private Boolean isPinned;
    private Boolean allowComments;
    
    // Author info
    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;
    
    // User interaction state (for current user)
    private Boolean isLiked;
    private Boolean isAuthor;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    
    // Nested (optional)
    private List<CommentDto> comments;

    public static PostDto from(Post post) {
        return from(post, null, false);
    }

    public static PostDto from(Post post, UUID currentUserId, boolean isLiked) {
        return PostDto.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .category(post.getCategory())
                .tags(post.getTagList())
                .coverImageUrl(post.getCoverImageUrl())
                .status(post.getStatus())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .shareCount(post.getShareCount())
                .isPinned(post.getIsPinned())
                .allowComments(post.getAllowComments())
                .authorId(post.getAuthor().getId())
                .authorName(post.getAuthor().getFullName())
                .authorAvatarUrl(post.getAuthor().getAvatarUrl())
                .isLiked(isLiked)
                .isAuthor(currentUserId != null && post.belongsToUser(currentUserId))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    /**
     * Create preview (without full content)
     */
    public static PostDto preview(Post post, UUID currentUserId, boolean isLiked) {
        PostDto dto = from(post, currentUserId, isLiked);
        // Truncate content for preview
        if (dto.getContent() != null && dto.getContent().length() > 300) {
            dto.setContent(dto.getContent().substring(0, 300) + "...");
        }
        return dto;
    }
}
