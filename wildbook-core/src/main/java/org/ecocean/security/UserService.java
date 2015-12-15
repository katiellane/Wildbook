package org.ecocean.security;

import java.util.List;
import java.util.Set;

import org.ecocean.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface UserService {
    public static Logger logger = LoggerFactory.getLogger("UserService");

    public User getUserById(final String id);
    public Set<String> getAllRolesForUserInContext(final String id, final String context);
    public List<Organization> getOrganizations();
    public User getUserByEmail(final String email);
    public User getUserByNameOrEmail(final String term);
    public String createPWResetToken(final String userid);
    public SecurityInfo getSecurityInfo(final String userid);
    public void saveUser(final User user);
    public void deleteRoles(final String userid);
    public void addRole(final String userid, final String context, final String role);
    public User verifyPRToken(final String token);
    public boolean doesUserHaveRole(final String userid, final String context, final String role);
    public Organization getOrganization(String name);
    public void addOrganization(Organization org);
    public User getUserByFullname(String fullname);
}
