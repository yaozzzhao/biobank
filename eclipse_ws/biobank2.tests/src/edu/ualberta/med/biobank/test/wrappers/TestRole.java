package edu.ualberta.med.biobank.test.wrappers;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.ualberta.med.biobank.common.wrappers.BbRightWrapper;
import edu.ualberta.med.biobank.common.wrappers.PrivilegeWrapper;
import edu.ualberta.med.biobank.common.wrappers.RightPrivilegeWrapper;
import edu.ualberta.med.biobank.common.wrappers.RoleWrapper;
import edu.ualberta.med.biobank.model.RightPrivilege;
import edu.ualberta.med.biobank.model.Role;
import edu.ualberta.med.biobank.test.TestDatabase;
import edu.ualberta.med.biobank.test.internal.RoleHelper;

public class TestRole extends TestDatabase {

    @Test
    public void testGettersAndSetters() throws Exception {
        String name = "testGettersAndSetters" + r.nextInt();
        RoleWrapper role = RoleHelper.addRole(name, true);
        testGettersAndSetters(role);
    }

    @Test
    public void testCascadeWithRightPrivilege() throws Exception {
        String name = "testCascadeWithRightPrivilege" + r.nextInt();
        RoleWrapper role = new RoleWrapper(appService);
        role.setName(name);

        List<PrivilegeWrapper> privileges = PrivilegeWrapper
            .getAllPrivileges(appService);
        List<BbRightWrapper> rights = BbRightWrapper.getAllRights(appService);

        List<RightPrivilegeWrapper> rpList = new ArrayList<RightPrivilegeWrapper>();
        for (BbRightWrapper right : rights) {
            // for each right, give all privileges
            RightPrivilegeWrapper rp = new RightPrivilegeWrapper(appService);
            rp.setRight(right);
            rp.addToPrivilegeCollection(privileges);
            rp.setRole(role);
            rpList.add(rp);
        }
        role.addToRightPrivilegeCollection(rpList);
        role.persist();

        // checking cascade worked well:

        // supposed to have same number of rigthPrivilege than number of rights
        Assert.assertEquals(rights.size(),
            role.getRightPrivilegeCollection(false).size());
        List<Integer> rpIdList = new ArrayList<Integer>();
        int nberPriviliges = privileges.size();
        for (RightPrivilegeWrapper rp : role.getRightPrivilegeCollection(false)) {
            // each rightprivilege is supposed to have save number of privileges
            // than the total number of privileges
            Assert.assertEquals(nberPriviliges, rp
                .getPrivilegeCollection(false).size());
            rpIdList.add(rp.getId());
        }

        Integer idRole = role.getId();
        // delete Role
        role.delete();

        // check role deleted
        Role dbRole = ModelUtils
            .getObjectWithId(appService, Role.class, idRole);
        Assert.assertNull(dbRole);
        // check right privileges also deleted:
        for (Integer id : rpIdList) {
            RightPrivilege dbRp = ModelUtils.getObjectWithId(appService,
                RightPrivilege.class, id);
            Assert.assertNull(dbRp);
        }
    }
}
