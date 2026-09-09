package com.austin.module.profile.mapper;

import com.austin.module.profile.domain.UserProfile;
import com.austin.module.profile.mapper.model.ProfileSummaryRow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {

    List<ProfileSummaryRow> selectSummaries(@Param("accountIds") Collection<Long> accountIds);
}
