package com.cvibe.biz.community.dto;

import com.cvibe.biz.community.entity.Post.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for creating a post
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePostRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must be less than 200 characters")
    private String title;

    @NotBlank(message = "Content is required")
    @Size(max = 50000, message = "Content must be less than 50000 characters")
    private String content;

    @NotNull(message = "Category is required")
    private PostCategory category;

    @Size(max = 10, message = "Maximum 10 tags allowed")
    private List<String> tags;

    private String coverImageUrl;

    /**
     * Save as draft instead of publishing
     */
    @Builder.Default
    private Boolean isDraft = false;

    /**
     * Allow comments on this post
     */
    @Builder.Default
    private Boolean allowComments = true;
}
