package com.austin.module.post.controller.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.austin.module.post.domain.PostComment;
import com.austin.module.post.domain.PostCommentStatus;
import org.junit.jupiter.api.Test;

class AdminPostCommentResponseTests {

    @Test
    void keepsOriginalContentForHiddenComment() {
        PostComment comment = PostComment.builder()
                .id(1L)
                .postId(2L)
                .authorAccountId(3L)
                .content("需要管理员复核的原文")
                .status(PostCommentStatus.HIDDEN_BY_ADMIN)
                .build();

        AdminPostCommentResponse response = AdminPostCommentResponse.from(comment, null);

        assertThat(response.content()).isEqualTo("需要管理员复核的原文");
        assertThat(response.authorAccountId()).isEqualTo(3L);
    }
}
