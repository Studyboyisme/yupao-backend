package com.linyu.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linyu.model.UserTeam;
import com.linyu.mapper.UserTeamMapper;
import com.linyu.service.UserTeamService;
import org.springframework.stereotype.Service;

/**
* @author Linyu
* @description 针对表【user_team(用户队伍关系)】的数据库操作Service实现
* @createDate 2024-06-25 15:37:00
*/
@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService{

}




