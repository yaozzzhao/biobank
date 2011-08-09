package edu.ualberta.med.biobank.common.wrappers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import edu.ualberta.med.biobank.common.exception.BiobankCheckException;
import edu.ualberta.med.biobank.common.exception.BiobankException;
import edu.ualberta.med.biobank.common.formatters.DateFormatter;
import edu.ualberta.med.biobank.common.peer.CollectionEventPeer;
import edu.ualberta.med.biobank.common.peer.OriginInfoPeer;
import edu.ualberta.med.biobank.common.peer.PatientPeer;
import edu.ualberta.med.biobank.common.peer.ShipmentInfoPeer;
import edu.ualberta.med.biobank.common.peer.SpecimenPeer;
import edu.ualberta.med.biobank.common.wrappers.base.CollectionEventBaseWrapper;
import edu.ualberta.med.biobank.common.wrappers.base.SpecimenBaseWrapper;
import edu.ualberta.med.biobank.common.wrappers.internal.EventAttrWrapper;
import edu.ualberta.med.biobank.common.wrappers.internal.StudyEventAttrWrapper;
import edu.ualberta.med.biobank.model.CollectionEvent;
import edu.ualberta.med.biobank.model.Log;
import edu.ualberta.med.biobank.model.Specimen;
import gov.nih.nci.system.applicationservice.ApplicationException;
import gov.nih.nci.system.applicationservice.WritableApplicationService;
import gov.nih.nci.system.query.hibernate.HQLCriteria;

@SuppressWarnings("unused")
public class CollectionEventWrapper extends CollectionEventBaseWrapper {
    private static final String HAS_SPECIMENS_MSG = "Specimens are still linked to this Collection Event. Delete them before attempting to remove this Collection Event";
    private static final Collection<Property<?, ? super CollectionEvent>> UNIQUE_VISIT_NUMBER_PROPS;
    static {
        Collection<Property<?, ? super CollectionEvent>> tmp = new ArrayList<Property<?, ? super CollectionEvent>>();
        tmp.add(CollectionEventPeer.PATIENT.to(PatientPeer.ID));
        tmp.add(CollectionEventPeer.VISIT_NUMBER);

        UNIQUE_VISIT_NUMBER_PROPS = Collections.unmodifiableCollection(tmp);
    };

    private Map<String, StudyEventAttrWrapper> studyEventAttrMap;
    private Map<String, EventAttrWrapper> eventAttrMap;

    public CollectionEventWrapper(WritableApplicationService appService) {
        super(appService);
    }

    public CollectionEventWrapper(WritableApplicationService appService,
        CollectionEvent wrappedObject) {
        super(appService, wrappedObject);
    }

    private void removeFromSpecimenCollections(
        List<? extends SpecimenBaseWrapper> specimenCollection) {
        super.removeFromAllSpecimenCollection(specimenCollection);
        super.removeFromOriginalSpecimenCollection(specimenCollection);
    }

    private void removeFromSpecimenCollectionsWithCheck(
        List<? extends SpecimenBaseWrapper> specimenCollection)
        throws BiobankCheckException {
        super.removeFromAllSpecimenCollectionWithCheck(specimenCollection);
        super.removeFromOriginalSpecimenCollectionWithCheck(specimenCollection);
    }

    @Override
    public void removeFromAllSpecimenCollection(
        List<? extends SpecimenBaseWrapper> specCollection) {
        removeFromSpecimenCollections(specCollection);
    }

    @Override
    public void removeFromOriginalSpecimenCollection(
        List<? extends SpecimenBaseWrapper> specCollection) {
        removeFromSpecimenCollections(specCollection);
    }

    @Override
    public void removeFromAllSpecimenCollectionWithCheck(
        List<? extends SpecimenBaseWrapper> specCollection)
        throws BiobankCheckException {
        removeFromSpecimenCollectionsWithCheck(specCollection);
    }

    @Override
    public void removeFromOriginalSpecimenCollectionWithCheck(
        List<? extends SpecimenBaseWrapper> specCollection)
        throws BiobankCheckException {
        removeFromSpecimenCollectionsWithCheck(specCollection);
    }

    @Override
    public void addToOriginalSpecimenCollection(
        List<? extends SpecimenBaseWrapper> specs) {
        super.addToOriginalSpecimenCollection(specs);
        super.addToAllSpecimenCollection(specs);
    }

    @Override
    protected Log getLogMessage(String action, String site, String details)
        throws Exception {
        Log log = new Log();
        log.setAction(action);
        if (site == null) {
            log.setCenter(null);
        } else {
            log.setCenter(site);
        }
        log.setPatientNumber(getPatient().getPnumber());
        List<String> detailsList = new ArrayList<String>();
        if (details.length() > 0) {
            detailsList.add(details);
        }

        detailsList.add(new StringBuilder("visit:").append(getVisitNumber())
            .toString());

        try {
            detailsList.add(new StringBuilder("specimens:").append(
                getSourceSpecimensCount(false)).toString());
        } catch (BiobankException e) {
            e.printStackTrace();
        } catch (ApplicationException e) {
            e.printStackTrace();
        }
        log.setDetails(StringUtils.join(detailsList, ", "));
        log.setType("CollectionEvent");
        return log;
    }

    private static final String COLLECTION_EVENTS_BY_WAYBILL_QRY = "from "
        + CollectionEvent.class.getName() + " ce join ce."
        + CollectionEventPeer.ORIGINAL_SPECIMEN_COLLECTION
        + " as spcs join spcs." + SpecimenPeer.ORIGIN_INFO.getName()
        + " as oi join oi." + OriginInfoPeer.SHIPMENT_INFO.getName()
        + " as shipinfo where shipinfo." + ShipmentInfoPeer.WAYBILL + "=?";

    // TODO: make sure that these count methods are actually correct if the
    // memory contents have been altered...

    public static List<CollectionEventWrapper> getCollectionEvents(
        WritableApplicationService appService, String waybill)
        throws ApplicationException {
        HQLCriteria c = new HQLCriteria(COLLECTION_EVENTS_BY_WAYBILL_QRY,
            Arrays.asList(new Object[] { waybill }));
        List<CollectionEvent> raw = appService.query(c);
        if (raw == null) {
            return new ArrayList<CollectionEventWrapper>();
        }
        return wrapModelCollection(appService, raw,
            CollectionEventWrapper.class);
    }

    private static final String COLLECTION_EVENTS_BY_DATE_RECEIVED_QRY = "from "
        + CollectionEvent.class.getName()
        + " ce join ce."
        + CollectionEventPeer.ORIGINAL_SPECIMEN_COLLECTION
        + " as spcs join spcs."
        + SpecimenPeer.ORIGIN_INFO.getName()
        + " as oi join oi."
        + OriginInfoPeer.SHIPMENT_INFO.getName()
        + " as shipinfo where shipinfo." + ShipmentInfoPeer.RECEIVED_AT + "=?";

    public static List<CollectionEventWrapper> getCollectionEvents(
        WritableApplicationService appService, Date dateReceived)
        throws ApplicationException {
        List<CollectionEvent> raw = appService.query(new HQLCriteria(
            COLLECTION_EVENTS_BY_DATE_RECEIVED_QRY, Arrays
                .asList(new Object[] { dateReceived })));
        if (raw == null) {
            return new ArrayList<CollectionEventWrapper>();
        }
        return wrapModelCollection(appService, raw,
            CollectionEventWrapper.class);
    }

    private static final String SOURCE_SPECIMEN_COUNT_QRY = "select count(specimens) from "
        + CollectionEvent.class.getName()
        + " as cEvent join cEvent."
        + CollectionEventPeer.ORIGINAL_SPECIMEN_COLLECTION.getName()
        + " as specimens where cEvent."
        + CollectionEventPeer.ID.getName()
        + "=?";

    public long getSourceSpecimensCount(boolean fast) throws BiobankException,
        ApplicationException {
        if (fast) {
            HQLCriteria criteria = new HQLCriteria(SOURCE_SPECIMEN_COUNT_QRY,
                Arrays.asList(new Object[] { getId() }));
            return getCountResult(appService, criteria);
        }
        List<SpecimenWrapper> list = getOriginalSpecimenCollection(false);
        if (list == null)
            return 0;
        return list.size();
    }

    private static final String ALL_SPECIMEN_COUNT_QRY = "select count(spc) from "
        + Specimen.class.getName()
        + " as spc where spc."
        + Property.concatNames(SpecimenPeer.COLLECTION_EVENT,
            CollectionEventPeer.ID) + "=?";

    public long getAllSpecimensCount(boolean fast) throws BiobankException,
        ApplicationException {
        if (fast) {
            HQLCriteria criteria = new HQLCriteria(ALL_SPECIMEN_COUNT_QRY,
                Arrays.asList(new Object[] { getId() }));
            return getCountResult(appService, criteria);
        }
        List<SpecimenWrapper> list = getOriginalSpecimenCollection(false);
        if (list == null)
            return 0;
        return list.size();
    }

    private static final String ALIQUOTED_SPECIMEN_COUNT_QRY = "select count(spc) from "
        + Specimen.class.getName()
        + " as spc where spc."
        + Property.concatNames(SpecimenPeer.COLLECTION_EVENT,
            CollectionEventPeer.ID)
        + "=? and spc."
        + SpecimenPeer.PARENT_SPECIMEN.getName() + " is not null";

    public long getAliquotedSpecimensCount(boolean fast)
        throws BiobankException, ApplicationException {
        if (fast) {
            HQLCriteria criteria = new HQLCriteria(
                ALIQUOTED_SPECIMEN_COUNT_QRY,
                Arrays.asList(new Object[] { getId() }));
            return getCountResult(appService, criteria);
        }
        List<SpecimenWrapper> aliquotedSpecimens = getAliquotedSpecimenCollection(false);
        if (aliquotedSpecimens == null)
            return 0;
        return aliquotedSpecimens.size();
    }

    public List<SpecimenWrapper> getAliquotedSpecimenCollection(boolean sort) {
        List<SpecimenWrapper> aliquotedSpecimens = new ArrayList<SpecimenWrapper>(
            getAllSpecimenCollection(true));
        aliquotedSpecimens.removeAll(getOriginalSpecimenCollection(false));
        return aliquotedSpecimens;
    }

    /**
     * source specimen that are in a process event
     */
    public List<SpecimenWrapper> getSourceSpecimenCollectionInProcess(
        ProcessingEventWrapper pEvent, boolean sort) {
        List<SpecimenWrapper> specimens = new ArrayList<SpecimenWrapper>();
        for (SpecimenWrapper specimen : getOriginalSpecimenCollection(sort)) {
            if (specimen.getProcessingEvent() != null
                && specimen.getProcessingEvent().equals(pEvent))
                specimens.add(specimen);
        }
        return specimens;
    }

    private Map<String, StudyEventAttrWrapper> getStudyEventAttrMap() {
        if (studyEventAttrMap != null)
            return studyEventAttrMap;

        PatientWrapper patient = getPatient();

        studyEventAttrMap = new HashMap<String, StudyEventAttrWrapper>();
        if (patient != null && patient.getStudy() != null) {
            Collection<StudyEventAttrWrapper> studyEventAttrCollection = patient
                .getStudy().getStudyEventAttrCollection();
            if (studyEventAttrCollection != null) {
                for (StudyEventAttrWrapper studyEventAttr : studyEventAttrCollection) {
                    studyEventAttrMap.put(studyEventAttr.getLabel(),
                        studyEventAttr);
                }
            }
        }
        return studyEventAttrMap;
    }

    private Map<String, EventAttrWrapper> getEventAttrMap() {
        getStudyEventAttrMap();
        if (eventAttrMap != null)
            return eventAttrMap;

        eventAttrMap = new HashMap<String, EventAttrWrapper>();
        List<EventAttrWrapper> pvAttrCollection = getEventAttrCollection(false);
        if (pvAttrCollection != null) {
            for (EventAttrWrapper pvAttr : pvAttrCollection) {
                eventAttrMap.put(pvAttr.getStudyEventAttr().getLabel(), pvAttr);
            }
        }
        return eventAttrMap;
    }

    public String[] getEventAttrLabels() {
        getEventAttrMap();
        return eventAttrMap.keySet().toArray(new String[] {});
    }

    public String getEventAttrValue(String label) throws Exception {
        getEventAttrMap();
        EventAttrWrapper pvAttr = eventAttrMap.get(label);
        if (pvAttr == null) {
            StudyEventAttrWrapper studyEventAttr = studyEventAttrMap.get(label);
            // make sure "label" is a valid study pv attr
            if (studyEventAttr == null) {
                throw new Exception("StudyEventAttr with label \"" + label
                    + "\" is invalid");
            }
            // not assigned yet so return null
            return null;
        }
        return pvAttr.getValue();
    }

    public String getEventAttrTypeName(String label) throws Exception {
        getEventAttrMap();
        EventAttrWrapper pvAttr = eventAttrMap.get(label);
        StudyEventAttrWrapper studyEventAttr = null;
        if (pvAttr != null) {
            studyEventAttr = pvAttr.getStudyEventAttr();
        } else {
            studyEventAttr = studyEventAttrMap.get(label);
            // make sure "label" is a valid study pv attr
            if (studyEventAttr == null) {
                throw new Exception("StudyEventAttr withr label \"" + label
                    + "\" does not exist");
            }
        }
        return studyEventAttr.getEventAttrType().getName();
    }

    public String[] getEventAttrPermissible(String label) throws Exception {
        getEventAttrMap();
        EventAttrWrapper pvAttr = eventAttrMap.get(label);
        StudyEventAttrWrapper studyEventAttr = null;
        if (pvAttr != null) {
            studyEventAttr = pvAttr.getStudyEventAttr();
        } else {
            studyEventAttr = studyEventAttrMap.get(label);
            // make sure "label" is a valid study pv attr
            if (studyEventAttr == null) {
                throw new Exception("EventAttr for label \"" + label
                    + "\" does not exist");
            }
        }
        String permissible = studyEventAttr.getPermissible();
        if (permissible == null) {
            return null;
        }
        return permissible.split(";");
    }

    /**
     * Assigns a value to a patient visit attribute. The value is parsed for
     * correctness.
     * 
     * @param label The attribute's label.
     * @param value The value to assign.
     * @throws Exception when assigning a label of type "select_single" or
     *             "select_multiple" and the value is not one of the permissible
     *             ones.
     * @throws NumberFormatException when assigning a label of type "number" and
     *             the value is not a valid double number.
     * @throws ParseException when assigning a label of type "date_time" and the
     *             value is not a valid date and time.
     * @see edu.ualberta.med.biobank
     *      .common.formatters.DateFormatter.DATE_TIME_FORMAT
     */
    public void setEventAttrValue(String label, String value) throws Exception {
        getEventAttrMap();
        EventAttrWrapper pvAttr = eventAttrMap.get(label);
        StudyEventAttrWrapper studyEventAttr = null;

        if (pvAttr != null) {
            studyEventAttr = pvAttr.getStudyEventAttr();
        } else {
            studyEventAttr = studyEventAttrMap.get(label);
            if (studyEventAttr == null) {
                throw new Exception("no StudyEventAttr found for label \""
                    + label + "\"");
            }
        }

        if (!studyEventAttr.getActivityStatus().isActive()) {
            throw new Exception("attribute for label \"" + label
                + "\" is locked, changes not premitted");
        }

        if (value != null) {
            // validate the value
            value = value.trim();
            if (value.length() > 0) {
                String type = studyEventAttr.getEventAttrType().getName();
                List<String> permissibleSplit = null;

                if (EventAttrTypeEnum.SELECT_SINGLE.isSameType(type)
                    || EventAttrTypeEnum.SELECT_MULTIPLE.isSameType(type)) {
                    String permissible = studyEventAttr.getPermissible();
                    if (permissible != null) {
                        permissibleSplit = Arrays
                            .asList(permissible.split(";"));
                    }
                }

                if (EventAttrTypeEnum.SELECT_SINGLE.isSameType(type)) {
                    if (!permissibleSplit.contains(value)) {
                        throw new Exception("value " + value
                            + "is invalid for label \"" + label + "\"");
                    }
                } else if (EventAttrTypeEnum.SELECT_MULTIPLE.isSameType(type)) {
                    for (String singleVal : value.split(";")) {
                        if (!permissibleSplit.contains(singleVal)) {
                            throw new Exception("value " + singleVal + " ("
                                + value + ") is invalid for label \"" + label
                                + "\"");
                        }
                    }
                } else if (EventAttrTypeEnum.NUMBER.isSameType(type)) {
                    Double.parseDouble(value);
                } else if (EventAttrTypeEnum.DATE_TIME.isSameType(type)) {
                    DateFormatter.dateFormatter.parse(value);
                } else if (EventAttrTypeEnum.TEXT.isSameType(type)) {
                    // do nothing
                } else {
                    throw new Exception("type \"" + type + "\" not tested");
                }
            }
        }

        if (pvAttr == null) {
            pvAttr = new EventAttrWrapper(appService);
            pvAttr.setCollectionEvent(this);
            pvAttr.setStudyEventAttr(studyEventAttr);

            EventAttrWrapper oldValue = eventAttrMap.put(label, pvAttr);

            if (oldValue != null) {
                removeFromEventAttrCollection(Arrays.asList(oldValue));
            }

            addToEventAttrCollection(Arrays.asList(pvAttr));
        }
        pvAttr.setValue(value);
    }

    @Override
    public void resetInternalFields() {
        eventAttrMap = null;
        studyEventAttrMap = null;
    }

    @Override
    public int compareTo(ModelWrapper<CollectionEvent> wrapper) {
        if (wrapper instanceof CollectionEventWrapper) {
            Integer nber1 = wrappedObject.getVisitNumber();
            Integer nber2 = wrapper.wrappedObject.getVisitNumber();
            if (nber1 != null && nber2 != null) {
                return nber1.compareTo(nber2);
            }
        }
        return 0;
    }

    public static Integer getNextVisitNumber(
        WritableApplicationService appService, CollectionEventWrapper cevent)
        throws Exception {
        HQLCriteria c = new HQLCriteria("select max(ce.visitNumber) from "
            + CollectionEvent.class.getName() + " ce where ce.patient.id=?",
            Arrays.asList(cevent.getPatient().getId()));
        List<Object> result = appService.query(c);
        if (result == null || result.size() == 0 || result.get(0) == null)
            return 1;
        else
            return (Integer) result.get(0) + 1;
    }

    @Override
    protected void addPersistTasks(TaskList tasks) {
        tasks.add(check().notNull(CollectionEventPeer.VISIT_NUMBER));

        tasks.add(check().unique(UNIQUE_VISIT_NUMBER_PROPS));

        tasks.persistRemoved(this, CollectionEventPeer.ALL_SPECIMEN_COLLECTION);
        tasks.persistRemoved(this,
            CollectionEventPeer.ORIGINAL_SPECIMEN_COLLECTION);

        super.addPersistTasks(tasks);
    }

    @Override
    protected void addDeleteTasks(TaskList tasks) {
        tasks.add(check().empty(CollectionEventPeer.ALL_SPECIMEN_COLLECTION,
            HAS_SPECIMENS_MSG));

        super.addDeleteTasks(tasks);
    }

    // TODO: remove this override when all persist()-s are like this!
    @Override
    public void persist() throws Exception {
        WrapperTransaction.persist(this, appService);
    }

    @Override
    public void delete() throws Exception {
        WrapperTransaction.delete(this, appService);
    }
}
