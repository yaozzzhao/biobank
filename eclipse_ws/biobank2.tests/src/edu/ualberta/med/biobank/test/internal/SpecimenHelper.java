package edu.ualberta.med.biobank.test.internal;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

import edu.ualberta.med.biobank.common.util.RowColPos;
import edu.ualberta.med.biobank.common.wrappers.ActivityStatusWrapper;
import edu.ualberta.med.biobank.common.wrappers.CenterWrapper;
import edu.ualberta.med.biobank.common.wrappers.ClinicWrapper;
import edu.ualberta.med.biobank.common.wrappers.CollectionEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.OriginInfoWrapper;
import edu.ualberta.med.biobank.common.wrappers.PatientWrapper;
import edu.ualberta.med.biobank.common.wrappers.ProcessingEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.test.Utils;
import edu.ualberta.med.biobank.test.wrappers.TestCommon;

public class SpecimenHelper extends DbHelper {

    public static SpecimenWrapper newSpecimen(SpecimenTypeWrapper specimenType,
        String activityStatus, Date createdAt) throws Exception {
        SpecimenWrapper specimen = new SpecimenWrapper(appService);
        specimen.setSpecimenType(specimenType);
        specimen.setInventoryId(TestCommon.getNewInventoryId(new Random()));
        if (activityStatus != null) {
            specimen.setActivityStatus(ActivityStatusWrapper.getActivityStatus(
                appService, activityStatus));
        }
        specimen.setCreatedAt(createdAt);
        return specimen;
    }

    public static SpecimenWrapper newSpecimen(SpecimenTypeWrapper specimenType)
        throws Exception {
        return newSpecimen(specimenType,
            ActivityStatusWrapper.ACTIVE_STATUS_STRING, Utils.getRandomDate());
    }

    public static SpecimenWrapper newSpecimen(String specimenTypeName)
        throws Exception {
        return newSpecimen(SpecimenTypeHelper.addSpecimenType(specimenTypeName));
    }

    public static SpecimenWrapper newSpecimen(SpecimenWrapper parentSpc,
        SpecimenTypeWrapper specimenType, String activityStatus,
        CollectionEventWrapper cevent, ProcessingEventWrapper pevent,
        ContainerWrapper container, Integer row, Integer col,
        CenterWrapper<?> center) throws Exception {
        SpecimenWrapper specimen = newSpecimen(specimenType, activityStatus,
            Utils.getRandomDate());
        if (container != null) {
            specimen.setParent(container);
        }
        specimen.setProcessingEvent(pevent);
        specimen.setCollectionEvent(parentSpc.getCollectionEvent());
        if ((row != null) && (col != null)) {
            specimen.setPosition(new RowColPos(row, col));
        }
        OriginInfoWrapper oi = new OriginInfoWrapper(appService);
        oi.setCenter(center);
        oi.persist();
        specimen.setOriginInfo(oi);
        specimen.setCurrentCenter(center);
        specimen.setCollectionEvent(cevent);
        return specimen;
    }

    public static SpecimenWrapper addSpecimen(SpecimenTypeWrapper specimenType,
        String activityStatus, Date createdAt) throws Exception {
        SpecimenWrapper specimen = newSpecimen(specimenType, activityStatus,
            createdAt);
        specimen.persist();
        return specimen;
    }

    public static SpecimenWrapper addSpecimen(SpecimenTypeWrapper specimenType,
        String activityStatus, Date createdAt, CollectionEventWrapper cevent,
        CenterWrapper<?> center) throws Exception {
        SpecimenWrapper specimen = newSpecimen(specimenType, activityStatus,
            createdAt);
        specimen.setCollectionEvent(cevent);

        OriginInfoWrapper originInfo = new OriginInfoWrapper(appService);
        originInfo.setCenter(center);
        originInfo.persist();
        specimen.setOriginInfo(originInfo);
        specimen.setCurrentCenter(center);

        specimen.persist();
        return specimen;
    }

    /**
     * Adds an aliquoted specimen to the processing event.
     * 
     * @param parentSpc The source specimen
     * @param specimenType The specimen type for the aliquoted specimen
     * @param pevent The processing event.
     * @param container The container where the aliquoted specimen will be
     *            stored.
     * @param row The row in the container where the aliquoted specimen will be
     *            stored.
     * @param col The column in the container where the aliquoted specimen will
     *            be stored.
     * @return
     * @throws Exception
     */
    public static SpecimenWrapper addSpecimen(SpecimenWrapper parentSpc,
        SpecimenTypeWrapper specimenType, CollectionEventWrapper cevent,
        ProcessingEventWrapper pevent, ContainerWrapper container, Integer row,
        Integer col) throws Exception {
        SpecimenWrapper spc = newSpecimen(parentSpc, specimenType,
            ActivityStatusWrapper.ACTIVE_STATUS_STRING, cevent, pevent,
            container, row, col, container.getSite());
        spc.persist();
        return spc;
    }

    /**
     * Creates aliquoted specimens and adds them to a newly created processing
     * event. A source specimen is also created along with a collection event.
     * If specified, the aliquoted specimens are liked to the container starting
     * at the position specified.
     * 
     * @param patient The patient the samples are from.
     * @param clinic The clinic the collection event is from.
     * @param container The container where to link the specimens to.
     * @rowStart the starting row where to place the specimens at.
     * @colStart the starting column where to place the specimens at.
     * @count the number of specimens to create.
     * @return A list of the specimens that were created.
     * @throws Exception
     */
    public static List<SpecimenWrapper> addSpecimens(PatientWrapper patient,
        ClinicWrapper clinic, ContainerWrapper container, int rowStart,
        int colStart, int spcCount) throws Exception {
        int rowCap = container.getRowCapacity();
        int colCap = container.getColCapacity();

        if ((rowStart < 0) || (rowStart >= rowCap)) {
            throw new Exception("invalid rowStart: " + rowStart);
        }

        if ((colStart < 0) || (colStart >= colCap)) {
            throw new Exception("invalid colStart: " + colStart);
        }

        int posOffset = (rowStart * colCap) + colCap;

        if (posOffset + spcCount >= rowCap * colCap) {
            throw new Exception("cannot fit number of specimens: " + spcCount);
        }

        List<SpecimenTypeWrapper> allSampleTypes = SpecimenTypeWrapper
            .getAllSpecimenTypes(appService, true);

        SpecimenWrapper spc = SpecimenHelper.newSpecimen(allSampleTypes.get(0),
            ActivityStatusWrapper.ACTIVE_STATUS_STRING, Utils.getRandomDate());

        OriginInfoWrapper oi = new OriginInfoWrapper(appService);
        oi.setCenter(clinic);
        oi.persist();

        CollectionEventWrapper ce = CollectionEventHelper.addCollectionEvent(
            clinic, patient, 1, oi, spc);

        ProcessingEventWrapper pe = ProcessingEventHelper.addProcessingEvent(
            container.getSite(), patient, Utils.getRandomDate());

        List<SpecimenWrapper> spcs = new ArrayList<SpecimenWrapper>();

        for (int i = 0; i < spcCount; ++i, posOffset++) {
            SpecimenWrapper childSpc = SpecimenHelper.addSpecimen(spc,
                allSampleTypes.get(0), ce, pe, container, posOffset / colCap,
                posOffset % colCap);
            spcs.add(childSpc);
        }

        return spcs;
    }

    public static SpecimenWrapper addParentSpecimen() throws Exception {
        SpecimenTypeWrapper st = SpecimenTypeHelper.addSpecimenType("testst"
            + r.nextInt());
        st.persist();
        SpecimenWrapper newSpec = newSpecimen(st);
        newSpec.setCreatedAt(Utils.getRandomDate());
        newSpec.setActivityStatus(ActivityStatusWrapper
            .getActiveActivityStatus(appService));

        StudyWrapper study = StudyHelper.addStudy("Study-" + r.nextInt());
        SiteWrapper site = SiteHelper.addSite("testsite" + r.nextInt());
        PatientWrapper patient = PatientHelper.addPatient(
            "testp" + r.nextInt(), study);
        OriginInfoWrapper oi = OriginInfoHelper.addOriginInfo(site);

        newSpec.setSpecimenType(st);
        newSpec.setOriginInfo(oi);
        CollectionEventWrapper ce = CollectionEventHelper.addCollectionEvent(
            site, patient, 2, oi, newSpec);
        return ce.getOriginalSpecimenCollection(false).get(0);
    }
}
