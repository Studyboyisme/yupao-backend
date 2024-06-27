package com.linyu.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.linyu.model.Team;
import com.linyu.model.User;
import com.linyu.model.dto.TeamQuery;
import com.linyu.model.request.TeamJoinRequest;
import com.linyu.model.request.TeamQuitRequest;
import com.linyu.model.request.TeamUpdateRequest;
import com.linyu.model.vo.TeamUserVO;
import java.util.List;

/**
* @author Linyu
* @description 针对表【team(队伍)】的数据库操作Service
* @createDate 2024-06-25 15:35:17
*/
public interface TeamService extends IService<Team> {
    long addTeam(Team team, User loginUser);

    List<TeamUserVO> listTeams(TeamQuery teamQuery, boolean isAdmin);

    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    boolean joinTeam(TeamJoinRequest teamJoinRequest, User user);

    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    boolean deleteTeam(long id, User loginUser);
}
