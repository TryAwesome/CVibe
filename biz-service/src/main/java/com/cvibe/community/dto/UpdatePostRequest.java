package com.cvibe.community.dto;

import com.cvibe.community.entity.PostCategory;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for updating a post.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;

    private List<String> images;

    private List<String> tags;

    private PostCategory category;
}
