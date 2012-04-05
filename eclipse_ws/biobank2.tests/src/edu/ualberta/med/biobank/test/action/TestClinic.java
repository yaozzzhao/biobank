package edu.ualberta.med.biobank.test.action;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.validation.ConstraintViolationException;

import org.hibernate.Query;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import edu.ualberta.med.biobank.common.action.clinic.ClinicDeleteAction;
import edu.ualberta.med.biobank.common.action.clinic.ClinicGetInfoAction;
import edu.ualberta.med.biobank.common.action.clinic.ClinicGetInfoAction.ClinicInfo;
import edu.ualberta.med.biobank.common.action.clinic.ClinicSaveAction;
import edu.ualberta.med.biobank.common.action.clinic.ClinicSaveAction.ContactSaveInfo;
import edu.ualberta.med.biobank.common.util.HibernateUtil;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.Address;
import edu.ualberta.med.biobank.model.Clinic;
import edu.ualberta.med.biobank.model.Contact;
import edu.ualberta.med.biobank.test.Utils;
import edu.ualberta.med.biobank.test.action.helper.ClinicHelper;
import edu.ualberta.med.biobank.test.action.helper.CollectionEventHelper;
import edu.ualberta.med.biobank.test.action.helper.DispatchHelper;
import edu.ualberta.med.biobank.test.action.helper.SiteHelper.Provisioning;

public class TestClinic extends TestAction {

    @Rule
    public TestName testname = new TestName();

    private String name;

    private ClinicSaveAction clinicSaveAction;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        name = getMethodNameR();

        clinicSaveAction = ClinicHelper.getSaveAction(name, name,
            ActivityStatus.ACTIVE, getR().nextBoolean());
    }

    @Test
    public void saveNew() throws Exception {
        clinicSaveAction.setName(null);
        try {
            exec(clinicSaveAction);
            Assert.fail(
                "should not be allowed to add site with no name");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

        // null short name
        clinicSaveAction.setName(name);
        clinicSaveAction.setNameShort(null);
        try {
            exec(clinicSaveAction);
            Assert.fail(
                "should not be allowed to add site with no short name");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

        clinicSaveAction.setNameShort(name);
        clinicSaveAction.setActivityStatus(null);
        try {
            exec(clinicSaveAction);
            Assert.fail(
                "should not be allowed to add Clinic with no activity status");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

        clinicSaveAction.setActivityStatus(ActivityStatus.ACTIVE);
        clinicSaveAction.setAddress(null);
        try {
            exec(clinicSaveAction);
            Assert.fail(
                "should not be allowed to add site with no address");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

        Address address = new Address();
        address.setCity(name);
        clinicSaveAction.setAddress(address);
        clinicSaveAction.setContactSaveInfos(null);
        try {
            exec(clinicSaveAction);
            Assert.fail(
                "should not be allowed to add site with null site ids");
        } catch (NullPointerException e) {
            Assert.assertTrue(true);
        }

        // success path
        clinicSaveAction
            .setContactSaveInfos(new HashSet<ContactSaveInfo>());
        exec(clinicSaveAction);
    }

    @Test
    public void checkGetAction() throws Exception {
        Provisioning provisioning = new Provisioning(getExecutor(), name);

        CollectionEventHelper.createCEventWithSourceSpecimens(getExecutor(),
            provisioning.patientIds.get(0), provisioning.clinicId);

        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(provisioning.clinicId));

        Assert.assertEquals(ActivityStatus.ACTIVE,
            clinicInfo.clinic.getActivityStatus());
        Assert.assertEquals(new Long(1), clinicInfo.patientCount);
        Assert.assertEquals(new Long(1), clinicInfo.collectionEventCount);
        Assert.assertEquals(1, clinicInfo.contacts.size());
        Assert.assertEquals(1, clinicInfo.studyInfos.size());
        Assert.assertEquals(name + "_clinic_city", clinicInfo.clinic
            .getAddress()
            .getCity());
    }

    @Test
    public void nameChecks() throws Exception {
        // ensure we can change name on existing clinic
        Integer clinicId = exec(clinicSaveAction).getId();
        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(clinicId));
        clinicInfo.clinic.setName(name + "_2");
        ClinicSaveAction clinicSave =
            ClinicHelper.getSaveAction(clinicInfo);
        exec(clinicSave);

        // ensure we can change short name on existing clinic
        clinicInfo = exec(new ClinicGetInfoAction(clinicId));
        clinicInfo.clinic.setNameShort(name + "_2");
        clinicSave = ClinicHelper.getSaveAction(clinicInfo);
        exec(clinicSave);

        // test for duplicate name
        ClinicSaveAction saveClinic2 =
            ClinicHelper.getSaveAction(name + "_2", name,
                ActivityStatus.ACTIVE, false);
        try {
            exec(saveClinic2);
            Assert.fail("should not be allowed to add clinic with same name");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

        // test for duplicate name short
        saveClinic2.setName(Utils.getRandomString(5, 10));
        saveClinic2.setNameShort(name + "_2");

        try {
            exec(saveClinic2);
            Assert
                .fail("should not be allowed to add clinic with same name short");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

    }

    @Test
    public void comments() {
        // save with no comments
        Integer clinicId = exec(clinicSaveAction).getId();
        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(clinicId));
        Assert.assertEquals(0, clinicInfo.clinic.getComments().size());

        clinicInfo = addComment(clinicId);
        Assert.assertEquals(1, clinicInfo.clinic.getComments().size());

        clinicInfo = addComment(clinicId);
        Assert.assertEquals(2, clinicInfo.clinic.getComments().size());

        // TODO: check full name on each comment's user
        // for (Comment comment : clinicInfo.clinic.getCommentCollection()) {
        //
        // }
    }

    private ClinicInfo addComment(Integer clinicId) {
        ClinicSaveAction clinicSaveAction = ClinicHelper.getSaveAction(
            exec(new ClinicGetInfoAction(clinicId)));
        clinicSaveAction.setCommentText(Utils.getRandomString(20, 30));
        exec(clinicSaveAction).getId();
        return exec(new ClinicGetInfoAction(clinicId));
    }

    @Test
    public void contacts() throws Exception {
        Set<ContactSaveInfo> contactsAll = new HashSet<ContactSaveInfo>();
        Set<ContactSaveInfo> set1 = new HashSet<ContactSaveInfo>();
        Set<ContactSaveInfo> set2 = new HashSet<ContactSaveInfo>();

        for (int i = 0; i < 10; ++i) {
            ContactSaveInfo contactSaveInfo = new ContactSaveInfo();
            contactSaveInfo.name = name + "_contact" + i;

            contactsAll.add(contactSaveInfo);
            if (i < 5) {
                set1.add(contactSaveInfo);
            } else {
                set2.add(contactSaveInfo);
            }
        }

        clinicSaveAction.setContactSaveInfos(contactsAll);
        Integer clinicId = exec(clinicSaveAction).getId();

        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(clinicId));
        Assert.assertEquals(getContactNamesFromSaveInfo(contactsAll),
            getContactNames(clinicInfo.contacts));

        // remove Set 2 from the clinic, Set 1 should be left
        clinicSaveAction =
            ClinicHelper.getSaveAction(clinicInfo);
        clinicSaveAction.setContactSaveInfos(set1);
        exec(clinicSaveAction);

        clinicInfo = exec(new ClinicGetInfoAction(clinicId));
        Assert.assertEquals(getContactNamesFromSaveInfo(set1),
            getContactNames(clinicInfo.contacts));

        // remove all
        clinicSaveAction =
            ClinicHelper.getSaveAction(clinicInfo);
        clinicSaveAction.setContactSaveInfos(new HashSet<ContactSaveInfo>());
        exec(clinicSaveAction);

        clinicInfo = exec(new ClinicGetInfoAction(clinicId));
        Assert.assertTrue(clinicInfo.contacts.isEmpty());

        // check that this clinic no longer has any contacts
        Query q = session.createQuery("SELECT COUNT(*) FROM "
            + Contact.class.getName()
            + " ct WHERE ct.clinic.id=?");
        q.setParameter(0, clinicId);
        Assert.assertTrue(HibernateUtil.getCountFromQuery(q).equals(0L));
    }

    private Set<String> getContactNamesFromSaveInfo(
        Collection<ContactSaveInfo> contactSaveInfos) {
        Set<String> result = new HashSet<String>();
        for (ContactSaveInfo contactSaveInfo : contactSaveInfos) {
            result.add(contactSaveInfo.name);
        }
        return result;
    }

    private Set<String> getContactNames(Collection<Contact> contacts) {
        Set<String> result = new HashSet<String>();
        for (Contact contact : contacts) {
            result.add(contact.getName());
        }
        return result;
    }

    @Test
    public void delete() {
        // delete a study with no patients and no other associations
        Integer clinicId = exec(clinicSaveAction).getId();
        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(clinicId));
        exec(new ClinicDeleteAction(clinicInfo.clinic));

        // hql query for clinic should return empty
        Query q =
            session.createQuery("SELECT COUNT(*) FROM "
                + Clinic.class.getName() + " WHERE id=?");
        q.setParameter(0, clinicId);
        Long result = HibernateUtil.getCountFromQuery(q);

        Assert.assertTrue(result.equals(0L));
    }

    @Test
    public void deleteWithStudies() {
        Provisioning provisioning = new Provisioning(getExecutor(), name);
        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(provisioning.clinicId));
        try {
            exec(new ClinicDeleteAction(clinicInfo.clinic));
            Assert
                .fail("should not be allowed to delete a clinic linked to a study");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void deleteWithSrcDispatch() throws Exception {
        Provisioning provisioning = new Provisioning(getExecutor(), name);
        ClinicInfo clinicInfo =
            exec(new ClinicGetInfoAction(provisioning.clinicId));

        DispatchHelper.createDispatch(getExecutor(), provisioning.clinicId,
            provisioning.siteId,
            provisioning.patientIds.get(0));

        try {
            exec(new ClinicDeleteAction(clinicInfo.clinic));
            Assert
                .fail(
                "should not be allowed to delete a clinic which is a source of dispatches");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void deleteWithDstDispatch() throws Exception {
        Provisioning provisioning = new Provisioning(getExecutor(), name);

        // add second clinic to be the destination of the dispatch

        ClinicSaveAction csa2 =
            ClinicHelper.getSaveAction(name + "_clinic2", name,
                ActivityStatus.ACTIVE, getR().nextBoolean());
        Integer clinicId2 = exec(csa2).getId();
        ClinicInfo clinic2Info =
            exec(new ClinicGetInfoAction(clinicId2));

        DispatchHelper.createDispatch(getExecutor(), provisioning.clinicId,
            clinicId2,
            provisioning.patientIds.get(0));

        try {
            exec(new ClinicDeleteAction(clinic2Info.clinic));
            Assert
                .fail(
                "should not be allowed to delete a clinic which is a destination for dispatches");
        } catch (ConstraintViolationException e) {
            Assert.assertTrue(true);
        }

    }

}