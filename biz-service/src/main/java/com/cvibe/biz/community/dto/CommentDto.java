package com.cvibe.biz.community.dto;

import com.cvibe.biz.community.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * DTO for Comment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private UUID id;
    private UUID postId;
    private UUID parentId;
    private String content;
    private Integer likeCount;
    private Integer replyCount;
    private Boolean isEdited;
    private Boolean isDeleted;
    
    // Author info
    private UUID authorId;
    private String authorName;
    private String authorAvatarUrl;
    
    // User interaction state
    private Boolean isLiked;
    private Boolean isAuthor;
    
    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    
    // Nested replies (optional)
    private List<CommentDto> replies;

    public static CommentDto from(Comment comment) {
        return from(comment, null, false);
    }

    public static CommentDto from(Comment comment, UUID currentUserId, boolean isLiked) {
        return CommentDto.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .parentId(comment.getParent() != null ? comment.getParent().getId() : null)
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .isEdited(comment.getIsEdited())
                .isDeleted(comment.getIsDeleted())
                .authorId(comment.getAuthor().getId())
                .authorName(comment.getAuthor().getFullName())
                .authorAvatarUrl(comment.getAuthor().getAvatarUrl())
                .isLiked(isLiked)
                .isAuthor(currentUserId != null && comment.belongsToUser(currentUserId))
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    /**
     * Create with nested replies
     */
    public static CommentDto withReplies(Comment comment, UUID currentUserId, boolean isLiked) {
        CommentDto dto = from(comment, currentUserId, isLiked);
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            dto.setReplies(comment.getReplies().stream()
                    .filter(r -> !r.getIsDeleted())
                    .map(r -> CommentDto.from(r, currentUserId, false))
                    .collect(Collectors.toList()));
        }
        return dto;
    }
}
