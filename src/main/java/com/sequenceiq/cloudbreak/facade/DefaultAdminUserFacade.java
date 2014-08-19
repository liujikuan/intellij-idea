package com.sequenceiq.cloudbreak.facade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.json.RoleUpdateRequest;
import com.sequenceiq.cloudbreak.controller.json.StatusUpdateRequest;
import com.sequenceiq.cloudbreak.controller.json.UserJson;
import com.sequenceiq.cloudbreak.controller.json.UserUpdateRequest;
import com.sequenceiq.cloudbreak.converter.UserConverter;
import com.sequenceiq.cloudbreak.domain.User;
import com.sequenceiq.cloudbreak.domain.UserRole;
import com.sequenceiq.cloudbreak.domain.UserStatus;
import com.sequenceiq.cloudbreak.service.account.AccountService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.UserRolesUtil;

@Service
public class DefaultAdminUserFacade implements AdminUserFacade {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAdminUserFacade.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserConverter userConverter;

    @Autowired
    private AccountService accountService;

    @Override
    public String inviteUser(User admin, String email) {
        LOGGER.debug("Inviting {} ...", email);
        String hash = userService.inviteUser(admin, email, UserRole.ACCOUNT_USER);
        LOGGER.debug("Invitation hash {}.", hash);
        return hash;
    }

    @Override
    public String inviteAdmin(User admin, String email) {
        LOGGER.debug("Inviting {} ...", email);
        String hash = userService.inviteUser(admin, email, UserRole.ACCOUNT_ADMIN);
        LOGGER.debug("Invitation hash {}.", hash);
        return hash;
    }

    @Override
    public UserJson activateUser(Long userId) {
        LOGGER.debug("Activating user with id [{}] ...");
        User user = userService.setUserStatus(userId, UserStatus.ACTIVE);
        LOGGER.debug("User with id [{}] activated");
        return userConverter.convert(user);
    }

    @Override
    public UserJson deactivateUser(Long userId) {
        LOGGER.debug("Dectivating user with id [{}] ...");
        User user = userService.setUserStatus(userId, UserStatus.DISABLED);
        LOGGER.debug("User with id [{}] deactivated");
        return userConverter.convert(user);
    }

    @Override
    public List<UserJson> accountUsers(User admin) {
        Set<User> accountUserSet = accountService.accountUsers(admin.getAccount().getId());
        return new ArrayList<UserJson>(userConverter.convertAllEntityToJson(accountUserSet));
    }

    @Override
    public UserJson putUserInRoles(Long userId, Set<UserRole> roles) {
        LOGGER.debug("Dectivating user with id [{}] ...");
        User user = userService.setUserRoles(userId, roles);
        LOGGER.debug("User with id [{}] deactivated");
        return userConverter.convert(user);

    }

    @Override
    public UserJson updateUser(User admin, Long userId, UserUpdateRequest updateRequest) {
        UserJson modifiedUser = null;
        if (updateRequest instanceof StatusUpdateRequest) {
            StatusUpdateRequest statusUpdateRequest = (StatusUpdateRequest) updateRequest;
            LOGGER.debug("Status update request received for user id: {}, status: {}", userId, statusUpdateRequest.getUserStatus());
            switch (statusUpdateRequest.getUserStatus()) {
                case ACTIVE:
                    modifiedUser = activateUser(userId);
                    break;
                case DISABLED:
                    modifiedUser = deactivateUser(userId);
                    break;
                default:
                    throw new IllegalStateException(String.format("Unsupported status change to %s", statusUpdateRequest.getUserStatus().name()));
            }
        } else if (updateRequest instanceof RoleUpdateRequest) {
            RoleUpdateRequest roleUpdateRequest = (RoleUpdateRequest) updateRequest;
            LOGGER.debug("Role update request received for user id: {}, roles: {}", userId, roleUpdateRequest.getUserRole());
            if (UserRolesUtil.isUserInRole(admin, roleUpdateRequest.getUserRole())) {
                modifiedUser = putUserInRoles(userId, UserRolesUtil.getGroupForRole(roleUpdateRequest.getUserRole()));
            } else {
                throw new UnsupportedOperationException(String.format("Can't set the role to %s", roleUpdateRequest.getUserRole()));
            }
        } else {
            throw new IllegalStateException("Invalid UserUpdate request!");
        }
        return modifiedUser;
    }
}
