package com.austin.module.post.mapper.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostLikeCountRow {

    private Long postId;

    private Long likeCount;
}
