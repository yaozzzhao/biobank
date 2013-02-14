package edu.ualberta.med.biobank.forms.linkassign;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.scanprocess.CellInfo;
import edu.ualberta.med.biobank.common.action.scanprocess.SpecimenHierarchyInfo;
import edu.ualberta.med.biobank.common.action.scanprocess.SpecimenLinkProcessAction;
import edu.ualberta.med.biobank.common.action.scanprocess.result.ProcessResult;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction.AliquotedSpecimenInfo;
import edu.ualberta.med.biobank.common.action.specimen.SpecimenLinkSaveAction.AliquotedSpecimenResInfo;
import edu.ualberta.med.biobank.common.action.specimenType.SpecimenTypesGetForContainerTypesAction;
import edu.ualberta.med.biobank.common.util.StringUtil;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.forms.linkassign.LinkFormPatientManagement.CEventComboCallback;
import edu.ualberta.med.biobank.forms.linkassign.LinkFormPatientManagement.PatientTextCallback;
import edu.ualberta.med.biobank.forms.listener.EnterKeyToNextFieldListener;
import edu.ualberta.med.biobank.gui.common.BgcLogger;
import edu.ualberta.med.biobank.gui.common.BgcPlugin;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;
import edu.ualberta.med.biobank.model.AbstractPosition;
import edu.ualberta.med.biobank.model.ActivityStatus;
import edu.ualberta.med.biobank.model.AliquotedSpecimen;
import edu.ualberta.med.biobank.model.Capacity;
import edu.ualberta.med.biobank.model.Specimen;
import edu.ualberta.med.biobank.model.SpecimenType;
import edu.ualberta.med.biobank.model.util.RowColPos;
import edu.ualberta.med.biobank.validators.StringLengthValidator;
import edu.ualberta.med.biobank.widgets.grids.well.PalletWell;
import edu.ualberta.med.biobank.widgets.grids.well.UICellStatus;
import edu.ualberta.med.biobank.widgets.specimentypeselection.AliquotedSpecimenSelectionWidget;
import edu.ualberta.med.biobank.widgets.specimentypeselection.ISpecimenTypeSelectionChangedListener;
import edu.ualberta.med.biobank.widgets.specimentypeselection.SpecimenTypeSelectionEvent;
import edu.ualberta.med.biobank.widgets.specimentypeselection.SpecimenTypeSelectionWidget;
import edu.ualberta.med.scannerconfig.ScannerConfigPlugin;
import edu.ualberta.med.scannerconfig.preferences.PreferenceConstants;
import gov.nih.nci.system.applicationservice.ApplicationException;

// FIXME the custom selection is not done in this version.
public class SpecimenLinkEntryForm extends AbstractLinkAssignEntryForm {
    private static final I18n i18n = I18nFactory.getI18n(SpecimenLinkEntryForm.class);

    protected static BgcLogger log = BgcLogger.getLogger(SpecimenLinkEntryForm.class.getName());

    @SuppressWarnings("nls")
    public static final String ID = "edu.ualberta.med.biobank.forms.SpecimenLinkEntryForm";

    @SuppressWarnings("nls")
    private static final String INVENTORY_ID_BINDING = "inventoryId-binding";

    @SuppressWarnings("nls")
    private static final String NEW_SINGLE_POSITION_BINDING = "newSinglePosition-binding";

    private static BgcLogger logger = BgcLogger.getLogger(SpecimenLinkEntryForm.class.getName());

    private static Mode mode = Mode.MULTIPLE;

    // set to true when in scan link multiple and user enters barcodes using the
    // handheld scanner
    private boolean scanMultipleWithHandheldInput = false;

    // TODO do not need a composite class anymore if only one link form is left
    private final LinkFormPatientManagement linkFormPatientManagement;

    // single linking
    // source specimen / type relation when only one specimen
    private AliquotedSpecimenSelectionWidget singleTypesWidget;
    protected boolean inventoryIdModified;
    private Label newSinglePositionLabel;
    private BgcBaseText newSinglePositionText;
    private StringLengthValidator newSinglePositionValidator;
    private boolean positionTextModified;

    private BgcBaseText inventoryIdText;

    // Multiple linking
    // list of source specimen / type widget for multiple linking
    private SpecimenTypeSelectionWidget specimenTypesWidget;
    private Composite multipleOptionsFields;

    // row:total (for one row number, associate the number of specimen found on
    // this row)
    private final Map<Integer, Integer> typesRows = new HashMap<Integer, Integer>();

    // source/type hierarchy selected (use rows order)
    private List<SpecimenHierarchyInfo> preSelections;

    public SpecimenLinkEntryForm() {
        linkFormPatientManagement = new LinkFormPatientManagement(widgetCreator, this);
    }

    @SuppressWarnings("nls")
    @Override
    protected void init() throws Exception {
        log.debug("init");
        super.init();
        // TR: title
        setPartName(i18n.tr("Linking specimens"));
        setCanLaunchScan(true);
    }

    @SuppressWarnings("nls")
    @Override
    protected String getFormTitle() {
        // TR: form title
        return i18n.tr("Linking specimens");
    }

    @SuppressWarnings("nls")
    @Override
    protected boolean isSingleMode() {
        log.debug("isSingleMode:" + mode.isSingleMode());
        return mode.isSingleMode();
    }

    @SuppressWarnings("nls")
    @Override
    protected void setMode(Mode m) {
        log.debug("setMode: " + mode);
        mode = m;
        if (mode == Mode.MULTIPLE) {
            scanMultipleWithHandheldInput = false;
        }
    }

    @SuppressWarnings("nls")
    @Override
    protected String getActivityTitle() {
        return "Specimen Link";
    }

    @Override
    public BgcLogger getErrorLogger() {
        return logger;
    }

    @SuppressWarnings("nls")
    @Override
    protected String getOkMessage() {
        // TR: title area message
        return i18n.tr("Link specimens to their source specimens");
    }

    @Override
    public String getNextOpenedFormId() {
        return ID;
    }

    @Override
    protected boolean showSinglePosition() {
        return SessionManager.getUser().getCurrentWorkingSite() != null;
    }

    @Override
    protected void createCommonFields(Composite commonFieldsComposite) {
        // Patient number
        linkFormPatientManagement.createPatientNumberText(commonFieldsComposite);
        linkFormPatientManagement.setPatientTextCallback(new PatientTextCallback() {
            @Override
            public void focusLost() {
                setTypeCombos();
            }

            @Override
            public void textModified() {
            }
        });
        // Processing event and Collection events lists
        linkFormPatientManagement.createEventsWidgets(commonFieldsComposite);
        linkFormPatientManagement.setCEventComboCallback(new CEventComboCallback() {
            @Override
            public void selectionChanged() {
                setTypeCombos();
            }
        });
    }

    @Override
    protected void createMultipleFields(final Composite parent) {
        multipleOptionsFields = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(3, false);
        layout.horizontalSpacing = 10;
        layout.marginWidth = 0;
        multipleOptionsFields.setLayout(layout);
        toolkit.paintBordersFor(multipleOptionsFields);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        multipleOptionsFields.setLayoutData(gd);
        // plate selection
        createPlateToScanField(multipleOptionsFields);
        plateToScanText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (checkGridDimensionsChanged()) {
                    recreateScanPalletWidget(currentGridDimensions.getRow(),
                        currentGridDimensions.getCol());
                    // typesSelectionPerRowComposite.dispose();
                    updateHierarchyWidgets(currentGridDimensions.getRow());
                    page.layout(true, true);
                    book.reflow(true);
                }
            }
        });
        createScanButton(parent);
        createHierarchyWidgets(parent);
    }

    @SuppressWarnings("nls")
    @Override
    protected void defaultInitialisation() {
        log.debug("defaultInitialisation");
        super.defaultInitialisation();
        setNeedSinglePosition(mode == Mode.SINGLE_POSITION);
        scanMultipleWithHandheldInput = false;
    }

    @SuppressWarnings("nls")
    @Override
    protected void setNeedSinglePosition(boolean position) {
        log.debug("setNeedSinglePosition: " + position);
        widgetCreator.setBinding(NEW_SINGLE_POSITION_BINDING, position);
        widgetCreator.showWidget(newSinglePositionLabel, position);
        widgetCreator.showWidget(newSinglePositionText, position);
    }

    @SuppressWarnings("nls")
    @Override
    protected void createSingleFields(Composite parent) {
        Composite fieldsComposite = toolkit.createComposite(parent);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        fieldsComposite.setLayout(layout);
        toolkit.paintBordersFor(fieldsComposite);
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        fieldsComposite.setLayoutData(gd);

        final NonEmptyStringValidator idValidator = new NonEmptyStringValidator(
            // validation error message
            i18n.tr("Inventory ID cannot be empty"));

        // position field
        Label inventoryIdLabel = widgetCreator.createLabel(fieldsComposite,
            Specimen.PropertyName.INVENTORY_ID.toString());

        // inventoryID
        inventoryIdText = (BgcBaseText) widgetCreator.createBoundWidget(
            fieldsComposite, BgcBaseText.class, SWT.NONE,
            inventoryIdLabel, new String[0],
            new WritableValue(StringUtil.EMPTY_STRING, String.class),
            idValidator, INVENTORY_ID_BINDING);
        inventoryIdText.addKeyListener(textFieldKeyListener);
        inventoryIdText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (inventoryIdModified
                    && idValidator.validate(inventoryIdText.getText()) == Status.OK_STATUS) {
                    BusyIndicator.showWhile(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getShell().getDisplay(), new Runnable() {
                        @Override
                        public void run() {
                            checkInventoryId(inventoryIdText);
                        }
                    });
                }
                inventoryIdModified = false;
            }
        });
        inventoryIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inventoryIdModified = true;
                newSinglePositionText.setText(StringUtil.EMPTY_STRING);
                canSaveSingleSpecimen.setValue(false);
            }
        });

        // position field
        newSinglePositionLabel =
            widgetCreator.createLabel(fieldsComposite, AbstractPosition.NAME.singular().toString());
        newSinglePositionValidator = new StringLengthValidator(4,
            // validation error message
            i18n.tr("Enter a position"));
        newSinglePositionText = (BgcBaseText) widgetCreator.createBoundWidget(
            fieldsComposite, BgcBaseText.class, SWT.NONE, newSinglePositionLabel, new String[0],
            new WritableValue(StringUtil.EMPTY_STRING, String.class),
            newSinglePositionValidator, NEW_SINGLE_POSITION_BINDING);
        newSinglePositionText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (positionTextModified
                    && newSinglePositionValidator.validate(newSinglePositionText.getText()) == Status.OK_STATUS) {
                    BusyIndicator.showWhile(PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                        .getShell().getDisplay(), new Runnable() {
                        @Override
                        public void run() {
                            initContainersFromPosition(newSinglePositionText, null);
                            checkPositionAndSpecimen(inventoryIdText, newSinglePositionText);
                        }
                    });
                }
                positionTextModified = false;
            }
        });
        newSinglePositionText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                positionTextModified = true;
                displaySinglePositions(false);
                canSaveSingleSpecimen.setValue(false);
            }
        });
        newSinglePositionText.addKeyListener(EnterKeyToNextFieldListener.INSTANCE);

        // widget to select the source and the type
        singleTypesWidget =
            new AliquotedSpecimenSelectionWidget(fieldsComposite, null, null, widgetCreator, false);
        singleTypesWidget.addBindings();
    }

    @SuppressWarnings("nls")
    private void checkInventoryId(BgcBaseText inventoryIdText) {
        log.debug("checkInventoryId: " + inventoryIdText.getText());
        boolean ok = true;
        try {
            SpecimenWrapper specimen =
                SpecimenWrapper.getSpecimen(SessionManager.getAppService(),
                    inventoryIdText.getText());
            if (specimen != null) {
                BgcPlugin.openAsyncError(
                    // dialog title
                    i18n.tr("InventoryId error"),
                    // dialog message
                    i18n.tr("InventoryId {0} already exists.", inventoryIdText.getText()));
                ok = false;
            }
        } catch (Exception e) {
            BgcPlugin.openAsyncError(
                // dialog title
                i18n.tr("Error checking inventoryId"), e);
            ok = false;
        }
        singleTypesWidget.setEnabled(ok);
        newSinglePositionText.setEnabled(ok);
        canSaveSingleSpecimen.setValue(ok);
        if (!ok) focusControl(inventoryIdText);

    }

    @Override
    protected void setBindings(boolean isSingleMode) {
        super.setBindings(isSingleMode);
        widgetCreator.setBinding(INVENTORY_ID_BINDING, isSingleMode);
        singleTypesWidget.removeBindings();
        if (isSingleMode) {
            singleTypesWidget.addBindings();
            singleTypesWidget.deselectAll();
        }
    }

    @SuppressWarnings("nls")
    @Override
    protected void updateAvailableSpecimenTypes() {
        log.debug("updateAvailableSpecimenTypes");
        setTypeCombos();
    }

    /**
     * Get types only defined in the patient's study. Then set these types to the types combos
     * 
     * @param typesRowss used only in multiple to indicate the count of each row after scan has been
     *            done.
     */
    @SuppressWarnings("nls")
    private void setTypeCombos() {
        log.debug("setTypeCombos");

        List<AliquotedSpecimen> studiesAliquotedTypes = null;
        List<SpecimenType> authorizedTypesInContainers = new ArrayList<SpecimenType>();
        if (isSingleMode()) {
            log.debug("setTypeCombos: single mode");

            if ((parentContainers != null) && (parentContainers.size() >= 1)) {
                for (SpecimenTypeWrapper wrapper : parentContainers.get(0).getContainerType().getSpecimenTypeCollection()) {
                    authorizedTypesInContainers.add(wrapper.getWrappedObject());
                }
            }
        } else {
            log.debug("setTypeCombos: multiple mode");

            /*
             * If the current center is a site, and if this site defines containers of 8*12 or 10*10
             * size, then get the specimen types these containers can contain
             */
            if (SessionManager.getUser().getCurrentWorkingSite() != null) {
                List<SpecimenType> res = null;
                try {
                    res = getSpecimenTypeForPalletScannable();
                } catch (ApplicationException e) {
                    BgcPlugin.openAsyncError("Error", "Failed to retrieve specimen types.");
                }
                if (res.size() != 0) authorizedTypesInContainers = res;
            }
        }

        studiesAliquotedTypes =
            linkFormPatientManagement.getStudyAliquotedTypes(authorizedTypesInContainers);
        List<Specimen> availableSourceSpecimens =
            linkFormPatientManagement.getParentSpecimenForPEventAndCEvent();

        if (!authorizedTypesInContainers.isEmpty()) {
            // availableSourceSpecimen should be parents of the authorized Types
            // !
            List<Specimen> filteredSpecs = new ArrayList<Specimen>();
            for (Specimen spec : availableSourceSpecimens)
                if (!Collections.disjoint(authorizedTypesInContainers,
                    spec.getSpecimenType().getChildSpecimenTypes())) {
                    filteredSpecs.add(spec);
                }
            availableSourceSpecimens = filteredSpecs;
        }

        // for single
        singleTypesWidget.resetValues(true, false);
        singleTypesWidget.setSourceSpecimens(availableSourceSpecimens);
        singleTypesWidget.setResultTypes(studiesAliquotedTypes);
        if (!isSingleMode()) {
            specimenTypesWidget.setSelections(availableSourceSpecimens, studiesAliquotedTypes);
        }
    }

    private List<SpecimenType> getSpecimenTypeForPalletScannable() throws ApplicationException {
        List<SpecimenType> result = new ArrayList<SpecimenType>();

        SiteWrapper site = SessionManager.getUser().getCurrentWorkingSite();
        if (site == null) {
            // scan link being used when working center is a clinic, clinics do not have
            // container types
            return result;
        }

        Set<Capacity> capacities = new HashSet<Capacity>();
        for (String gridDimensions : PreferenceConstants.SCANNER_PALLET_GRID_DIMENSIONS_ROWSCOLS) {
            Capacity capacity = new Capacity();
            capacity.setRowCapacity(PreferenceConstants.gridRows(gridDimensions));
            capacity.setColCapacity(PreferenceConstants.gridCols(gridDimensions));
            capacities.add(capacity);
        }
        result = SessionManager.getAppService().doAction(
            new SpecimenTypesGetForContainerTypesAction(site.getWrappedObject(), capacities
            )).getList();
        return result;
    }

    @Override
    protected boolean fieldsValid() {
        if (mode.isSingleMode()) return true;
        if ((mode == Mode.MULTIPLE) && scanMultipleWithHandheldInput) return true;
        return isPlateValid() && linkFormPatientManagement.fieldsValid();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("nls")
    @Override
    protected void doBeforeSave() throws Exception {
        log.debug("doBeforeSave");
        // can't access the combos in another thread, so do it now
        if (mode.isSingleMode()) {
            SpecimenHierarchyInfo selection = singleTypesWidget.getSelection();
            singleSpecimen.setInventoryId(inventoryIdText.getText());
            singleSpecimen.setParentSpecimen(
                new SpecimenWrapper(SessionManager.getAppService(), selection.getParentSpecimen()));
            singleSpecimen.setSpecimenType(
                new SpecimenTypeWrapper(SessionManager.getAppService(), selection.getAliquotedSpecimenType().getSpecimenType()));
            singleSpecimen.setCollectionEvent(linkFormPatientManagement.getSelectedCollectionEvent());
        }
    }

    @SuppressWarnings("nls")
    @Override
    protected void saveForm() throws Exception {
        log.debug("saveForm");
        if (mode.isSingleMode()) {
            saveSingleSpecimen();
        } else {
            saveMultipleSpecimens();
        }
        setFinished(false);
        SessionManager.log("save", null, "SpecimenLink");
    }

    @SuppressWarnings("nls")
    private void saveMultipleSpecimens() throws Exception {
        log.debug("saveMultipleSpecimens");

        @SuppressWarnings("unchecked")
        Map<RowColPos, PalletWell> cells = (Map<RowColPos, PalletWell>) palletWidget.getCells();
        List<AliquotedSpecimenInfo> asiList = new ArrayList<AliquotedSpecimenInfo>();
        for (PalletWell cell : cells.values()) {
            if (PalletWell.hasValue(cell) && cell.getStatus() == UICellStatus.TYPE) {
                SpecimenWrapper sourceSpecimen = cell.getSourceSpecimen();
                SpecimenWrapper aliquotedSpecimen = cell.getSpecimen();
                AliquotedSpecimenInfo asi = new AliquotedSpecimenInfo();
                asi.activityStatus = ActivityStatus.ACTIVE;
                asi.typeId = aliquotedSpecimen.getSpecimenType().getId();
                asi.inventoryId = cell.getValue();
                asi.parentSpecimenId = sourceSpecimen.getId();
                asiList.add(asi);
            }
        }
        List<AliquotedSpecimenResInfo> resList =
            SessionManager
                .getAppService()
                .doAction(
                    new SpecimenLinkSaveAction(SessionManager.getUser().getCurrentWorkingCenter()
                        .getId(), linkFormPatientManagement.getCurrentPatient().getStudy().getId(),
                        asiList)).getList();
        printSaveMultipleLogMessage(resList);
    }

    @SuppressWarnings("nls")
    protected void printSaveMultipleLogMessage(List<AliquotedSpecimenResInfo> resList) {
        log.debug("printSaveMultipleLogMessage");

        StringBuffer sb = new StringBuffer("ALIQUOTED SPECIMENS:\n");
        for (AliquotedSpecimenResInfo resInfo : resList) {
            sb.append(MessageFormat
                .format(
                    "LINKED: ''{0}'' with type ''{1}'' to source: {2} ({3}) - Patient: {4} - Visit: {5} - Center: {6}\n",
                    resInfo.inventoryId, resInfo.typeName, resInfo.parentTypeName,
                    resInfo.parentInventoryId, resInfo.patientPNumber, resInfo.visitNumber,
                    resInfo.currentCenterName));
        }
        // Want only one common 'log entry' so use a stringbuffer to print
        // everything together
        appendLog(sb.toString());

        // LINKING\: {0} specimens linked to patient {1} on center {2}
        appendLog(MessageFormat.format(
            "LINKING: {0} specimens linked to patient {1} on center {2}", resList.size(),
            linkFormPatientManagement.getCurrentPatient().getPnumber(), SessionManager.getUser()
                .getCurrentWorkingCenter().getNameShort()));
    }

    @SuppressWarnings("nls")
    private void saveSingleSpecimen() throws Exception {
        log.debug("saveSingleSpecimen");

        AliquotedSpecimenInfo asi = new AliquotedSpecimenInfo();
        asi.activityStatus = ActivityStatus.ACTIVE;
        asi.typeId = singleSpecimen.getSpecimenType().getId();
        if (singleSpecimen.getParentContainer() != null) {
            asi.containerId = singleSpecimen.getParentContainer().getId();
            asi.position = singleSpecimen.getPosition();
        }
        asi.inventoryId = singleSpecimen.getInventoryId();
        asi.parentSpecimenId = singleSpecimen.getParentSpecimen().getId();

        List<AliquotedSpecimenResInfo> resList = SessionManager.getAppService().doAction(
            new SpecimenLinkSaveAction(SessionManager.getUser().getCurrentWorkingCenter()
                .getId(), linkFormPatientManagement.getCurrentPatient().getStudy().getId(),
                Arrays.asList(asi))).getList();
        printSaveSingleLogMessage(resList);
    }

    @SuppressWarnings("nls")
    protected void printSaveSingleLogMessage(List<AliquotedSpecimenResInfo> resList) {
        log.debug("printSaveSingleLogMessage");

        if (resList.size() == 1) {
            AliquotedSpecimenResInfo resInfo = resList.get(0);
            String posStr = resInfo.position;
            if (posStr == null) {
                posStr = "none";
            }
            appendLog(MessageFormat
                .format(
                    "LINKED: ''{0}'' with type ''{1}'' to source: {2} ({3}) - Patient: {4} - Visit: {5} - Center: {6} - Position: {7}\n",
                    resInfo.inventoryId, resInfo.typeName, resInfo.parentInventoryId,
                    resInfo.parentTypeName, resInfo.patientPNumber, resInfo.visitNumber,
                    resInfo.currentCenterName, posStr));
        } else {
            throw new RuntimeException("Result size incorrect");
        }
    }

    @SuppressWarnings("nls")
    @Override
    public void setValues() throws Exception {
        log.debug("setValues");

        super.setValues();
        if (isSingleMode()) singleTypesWidget.deselectAll();
        showOnlyPallet(true);
        form.layout(true, true);
    }

    @SuppressWarnings("nls")
    @Override
    public void reset(boolean resetAll) {
        log.debug("reset: " + resetAll);

        linkFormPatientManagement.reset(resetAll);
        if (resetAll) {
            palletWidget.setCells(null);
            specimenTypesWidget.resetValues(true, true);
        }
        inventoryIdText.setText(StringUtil.EMPTY_STRING);
        super.reset(resetAll);
    }

    @SuppressWarnings("nls")
    @Override
    public boolean onClose() {
        log.debug("onClose");
        linkFormPatientManagement.onClose();
        return super.onClose();
    }

    @SuppressWarnings("nls")
    @Override
    /**
     * Multiple linking: do this before multiple scan is made
     */
    protected void beforeScanThreadStart() {
        log.debug("beforeScanThreadStart");
        super.beforeScanThreadStart();
        setTypeCombos();
        beforeScans(true);
    }

    @SuppressWarnings("nls")
    @Override
    /**
     * Multiple linking: do this before scan of one tube is really made
     */
    protected void beforeScanTubeAlone() {
        log.debug("beforeScanTubeAlone");
        super.beforeScanTubeAlone();
        beforeScans(false);
    }

    /**
     * Multiple linking: do this before any scan is really launched
     */
    @SuppressWarnings("nls")
    private void beforeScans(boolean resetTypeRows) {
        log.debug("beforeScans: " + resetTypeRows);
        preSelections = specimenTypesWidget.getSelections();
        if (resetTypeRows) typesRows.clear();
    }

    @SuppressWarnings("nls")
    @Override
    /**
     * Multiple linking: return fake cells for testing
     */
    protected Map<RowColPos, PalletWell> getFakeDecodedWells(String plateToScan) throws Exception {
        if (isFakeScanRandom) {
            return PalletWell.getRandomScanLink(plateToScan);
        }
        try {
            return PalletWell.getRandomScanLinkWithSpecimensAlreadyLinked(
                SessionManager.getAppService(), SessionManager.getUser().getCurrentWorkingCenter()
                    .getId(), plateToScan);
        } catch (Exception ex) {
            BgcPlugin.openAsyncError("Fake Scan problem", ex);
        }
        return null;
    }

    @SuppressWarnings("nls")
    @Override
    protected Action<ProcessResult> getCellProcessAction(Integer centerId, CellInfo cell,
        Locale locale) {
        log.debug("getCellProcessAction");
        return new SpecimenLinkProcessAction(centerId, linkFormPatientManagement
            .getCurrentPatient().getStudy().getId(), cell, locale);
    }

    @SuppressWarnings("nls")
    @Override
    protected Action<ProcessResult> getPalletProcessAction(Integer centerId,
        Map<RowColPos, CellInfo> cells, boolean isRescanMode, Locale locale) {
        log.debug("getPalletProcessAction");
        return new SpecimenLinkProcessAction(centerId, linkFormPatientManagement
            .getCurrentPatient().getStudy().getId(), cells, isRescanMode, locale);
    }

    @Override
    /**
     *  Multiple linking: do this after multiple scan has been launched
     *  @param rowToProcess is null if scanning everything, is the current row if is scanning a single cell
     */
    protected void afterScanAndProcess(final Integer rowToProcess) {
        Display.getDefault().asyncExec(new Runnable() {
            @SuppressWarnings("nls")
            @Override
            public void run() {
                log.debug("afterScanAndProcess: asyncExec");

                // enabled the hierarchy combos
                specimenTypesWidget.setEnabled(currentScanState != UICellStatus.ERROR);
                // set the combos lists
                if (typesRows.size() > 0) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (typesRows == null) return;

                            if (rowToProcess == null) {
                                specimenTypesWidget.setCounts(typesRows);
                            } else {
                                specimenTypesWidget.setCount(rowToProcess, typesRows.get(rowToProcess));
                            }
                        }
                    });
                }
                specimenTypesWidget.setFocus();
                // Show result in grid
                palletWidget.setCells(getCells());
                setRescanMode();
            }
        });
    }

    /**
     * Multiple linking: Post process of only one cell after server call has been made
     */
    @SuppressWarnings("nls")
    @Override
    protected void processCellResult(final RowColPos rcp, PalletWell cell) {
        log.debug("processCellResult");
        Integer typesRowsCount = typesRows.get(rcp.getRow());
        if (typesRowsCount == null) {
            typesRowsCount = 0;
            specimenTypesWidget.resetValues(rcp.getRow(), !isRescanMode(), true, true);
        }
        SpecimenHierarchyInfo selection = preSelections.get(cell.getRow());
        if (selection != null) setHierarchyToCell(cell, selection);
        if (PalletWell.hasValue(cell)) {
            typesRowsCount++;
            typesRows.put(rcp.getRow(), typesRowsCount);
        }
    }

    /**
     * Multiple linking: apply a source and type to a specific cell.
     */
    @SuppressWarnings("nls")
    private void setHierarchyToCell(PalletWell cell, SpecimenHierarchyInfo selection) {
        log.debug("setHierarchyToCell");
        cell.setSourceSpecimen(new SpecimenWrapper(SessionManager.getAppService(),
            selection.getParentSpecimen()));
        cell.setSpecimenType(new SpecimenTypeWrapper(SessionManager.getAppService(),
            selection.getAliquotedSpecimenType().getSpecimenType()));
        if (cell.getStatus() != UICellStatus.ERROR) cell.setStatus(UICellStatus.TYPE);
    }

    @SuppressWarnings("nls")
    @Override
    protected Mode initialisationMode() {
        log.debug("initialisationMode: " + mode);
        return mode;
    }

    @SuppressWarnings("nls")
    @Override
    protected void enableFields(boolean enable) {
        log.debug("enableFields: " + enable);
        super.enableFields(enable);
        multipleOptionsFields.setEnabled(enable);
    }

    @Override
    protected boolean canScanTubesManually(PalletWell cell) {
        if (linkFormPatientManagement.getSelectedCollectionEvent() == null) {
            return false;
        }
        return super.canScanTubesManually(cell);
    }

    @Override
    protected void postprocessScanTubeAlone(Set<PalletWell> palletCells) throws Exception {
        super.postprocessScanTubeAlone(palletCells);
        widgetCreator.setBinding(PLATE_VALIDATOR, false);
        scanMultipleWithHandheldInput = true;
        hideScannerBarcodeDecoration();
    }

    private void createHierarchyWidgets(Composite parent) {
        specimenTypesWidget = new SpecimenTypeSelectionWidget(parent, widgetCreator,
            ScannerConfigPlugin.getDefault().getPlateMaxRows(), currentGridDimensions.getRow());
        specimenTypesWidget.addSelectionChangedListener(new ISpecimenTypeSelectionChangedListener() {

            @Override
            public void selectionChanged(SpecimenTypeSelectionEvent event) {
                // if (selection != null) {
                @SuppressWarnings("unchecked")
                Map<RowColPos, PalletWell> cells =
                    (Map<RowColPos, PalletWell>) palletWidget.getCells();
                if (cells != null) {
                    for (RowColPos rcp : cells.keySet()) {
                        if (rcp.getRow() == event.getRowNumber()) {
                            PalletWell cell = cells.get(rcp);
                            if (PalletWell.hasValue(cell)) {
                                setHierarchyToCell(cell, event);
                            }
                        }
                    }
                    palletWidget.redraw();
                }
                // }

                if (palletWidget.isEverythingTyped()) {
                    setDirty(true);
                }

            }

        });
        toolkit.adapt(specimenTypesWidget);
    }

    private void updateHierarchyWidgets(int rows) {
        specimenTypesWidget.updateHierarchyWidgets(rows);
    }

}
