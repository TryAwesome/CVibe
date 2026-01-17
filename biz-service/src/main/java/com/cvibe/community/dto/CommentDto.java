package com.cvibe.community.dto;

import com.cvibe.community.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for Comment response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {

    private String id;
    private String postId;
    private PostDto.AuthorDto author;
    private String content;
    private String parentId;
    private Integer repliesCount;
    private List<CommentDto> replies;
    private String createdAt;

    /**
     * Create CommentDto from Comment entity.
     */
    public static CommentDto fromEntity(Comment comment) {
        return fromEntity(comment, null);
    }

    /**
     * Create CommentDto from Comment entity with replies.
     */
    public static CommentDto fromEntity(Comment comment, List<CommentDto> replies) {
        if (comment == null) {
            return null;
        }
        return CommentDto.builder()
                .id(comment.getId().toString())
                .postId(comment.getPost().getId().toString())
                .author(PostDto.AuthorDto.fromUser(comment.getAuthor()))
                .content(comment.getContent())
                .parentId(comment.getParent() != null ? comment.getParent().getId().toString() : null)
                .repliesCount(comment.getRepliesCount())
                .replies(replies)
                .createdAt(comment.getCreatedAt() != null ? comment.getCreatedAt().toString() : null)
                .build();
    }
}
