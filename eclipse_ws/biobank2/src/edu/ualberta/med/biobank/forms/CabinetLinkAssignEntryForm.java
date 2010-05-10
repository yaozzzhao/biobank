package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.springframework.remoting.RemoteConnectFailureException;

import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.BiobankCheckException;
import edu.ualberta.med.biobank.common.wrappers.ActivityStatusWrapper;
import edu.ualberta.med.biobank.common.wrappers.AliquotWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.PatientVisitWrapper;
import edu.ualberta.med.biobank.common.wrappers.PatientWrapper;
import edu.ualberta.med.biobank.common.wrappers.SampleStorageWrapper;
import edu.ualberta.med.biobank.common.wrappers.SampleTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.StudyWrapper;
import edu.ualberta.med.biobank.forms.LinkFormPatientManagement.PatientTextCallback;
import edu.ualberta.med.biobank.forms.listener.EnterKeyToNextFieldListener;
import edu.ualberta.med.biobank.logs.BiobankLogger;
import edu.ualberta.med.biobank.preferences.PreferenceConstants;
import edu.ualberta.med.biobank.validators.AbstractValidator;
import edu.ualberta.med.biobank.validators.CabinetInventoryIDValidator;
import edu.ualberta.med.biobank.validators.CabinetLabelValidator;
import edu.ualberta.med.biobank.widgets.CancelConfirmWidget;
import edu.ualberta.med.biobank.widgets.grids.AbstractContainerDisplayWidget;
import edu.ualberta.med.biobank.widgets.grids.ContainerDisplayFatory;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class CabinetLinkAssignEntryForm extends AbstractAliquotAdminForm {

    public static final String ID = "edu.ualberta.med.biobank.forms.CabinetLinkAssignEntryForm"; //$NON-NLS-1$

    private static BiobankLogger logger = BiobankLogger
        .getLogger(CabinetLinkAssignEntryForm.class.getName());

    private LinkFormPatientManagement linkFormPatientManagement;

    private Label cabinetLabel;
    private Label drawerLabel;
    private AbstractContainerDisplayWidget cabinetWidget;
    private AbstractContainerDisplayWidget drawerWidget;

    private Text inventoryIdText;
    private Label oldCabinetPositionLabel;
    private Text oldCabinetPosition;
    private Label oldCabinetPositionCheckLabel;
    private Text oldCabinetPositionCheck;
    private Label newCabinetPositionLabel;
    private Text newCabinetPosition;
    private Label sampleTypeComboLabel;
    private ComboViewer viewerSampleTypes;
    private Label sampleTypeTextLabel;
    private Text sampleTypeText;
    private Button checkButton;

    private CancelConfirmWidget cancelConfirmWidget;

    private IObservableValue resultShownValue = new WritableValue(
        Boolean.FALSE, Boolean.class);
    private static IObservableValue canLaunchCheck = new WritableValue(
        Boolean.TRUE, Boolean.class);

    private AliquotWrapper aliquot;
    private ContainerWrapper cabinet;
    private ContainerWrapper drawer;
    private ContainerWrapper bin;

    private String cabinetNameContains = ""; //$NON-NLS-1$

    private Button radioNew;

    private CabinetInventoryIDValidator inventoryIDValidator;

    private List<ContainerTypeWrapper> cabinetContainerTypes;

    protected boolean positionTextModified;

    private CabinetLabelValidator newCabinetPositionValidator;

    protected boolean inventoryIdModified;

    protected boolean oldPositionCheckModified;

    private AbstractValidator oldCabinetPositionCheckValidator;

    private List<SampleTypeWrapper> cabinetSampleTypes;

    private static final String SAMPLE_TYPE_LIST_BINDING = "sample-type-list-binding";

    @Override
    protected void init() {
        super.init();
        setPartName(Messages.getString("Cabinet.tabTitle")); //$NON-NLS-1$
        aliquot = new AliquotWrapper(appService);
        IPreferenceStore store = BioBankPlugin.getDefault()
            .getPreferenceStore();
        cabinetNameContains = store
            .getString(PreferenceConstants.CABINET_CONTAINER_NAME_CONTAINS);
        linkFormPatientManagement = new LinkFormPatientManagement(
            widgetCreator, this);
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText(Messages.getString("Cabinet.formTitle")); //$NON-NLS-1$
        GridLayout layout = new GridLayout(2, false);
        form.getBody().setLayout(layout);

        createFieldsSection();
        createLocationSection();

        cancelConfirmWidget = new CancelConfirmWidget(form.getBody(), this,
            true);

        addBooleanBinding(new WritableValue(Boolean.FALSE, Boolean.class),
            resultShownValue, Messages
                .getString("Cabinet.checkButton.validationMsg"));

        radioNew.setSelection(true);
        setMoveMode(false);
    }

    private void createLocationSection() throws ApplicationException {
        Composite client = toolkit.createComposite(form.getBody());
        GridLayout layout = new GridLayout(2, false);
        client.setLayout(layout);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.CENTER;
        gd.grabExcessHorizontalSpace = true;
        client.setLayoutData(gd);
        toolkit.paintBordersFor(client);

        cabinetLabel = toolkit.createLabel(client, "Cabinet"); //$NON-NLS-1$
        drawerLabel = toolkit.createLabel(client, "Drawer"); //$NON-NLS-1$

        List<ContainerTypeWrapper> types = ContainerTypeWrapper
            .getContainerTypesInSite(appService, SessionManager.getInstance()
                .getCurrentSite(), cabinetNameContains, false);
        ContainerTypeWrapper cabinetType = null;
        ContainerTypeWrapper drawerType = null;
        if (types.size() == 0) {
            BioBankPlugin.openAsyncError(Messages
                .getString("Cabinet.dialog.noType.error.title"), //$NON-NLS-1$
                Messages.getFormattedString("Cabinet.dialog.notType.error.msg", //$NON-NLS-1$
                    cabinetNameContains));
        } else {
            cabinetType = types.get(0);
            List<ContainerTypeWrapper> children = cabinetType
                .getChildContainerTypeCollection();
            if (children.size() > 0) {
                drawerType = children.get(0);
            }
        }
        cabinetWidget = ContainerDisplayFatory
            .createWidget(client, cabinetType);
        toolkit.adapt(cabinetWidget);
        GridData gdDrawer = new GridData();
        gdDrawer.verticalAlignment = SWT.TOP;
        cabinetWidget.setLayoutData(gdDrawer);

        drawerWidget = ContainerDisplayFatory.createWidget(client, drawerType);
        toolkit.adapt(drawerWidget);
        GridData gdBin = new GridData();
        gdBin.verticalSpan = 2;
        drawerWidget.setLayoutData(gdBin);
    }

    private void createFieldsSection() throws ApplicationException {
        Composite fieldsComposite = toolkit.createComposite(form.getBody());
        GridLayout layout = new GridLayout(3, false);
        layout.horizontalSpacing = 10;
        fieldsComposite.setLayout(layout);
        toolkit.paintBordersFor(fieldsComposite);
        GridData gd = new GridData();
        gd.widthHint = 500;
        gd.verticalAlignment = SWT.TOP;
        fieldsComposite.setLayoutData(gd);

        // radio button to choose new or move
        radioNew = toolkit.createButton(fieldsComposite, Messages
            .getString("Cabinet.button.new.text"), //$NON-NLS-1$
            SWT.RADIO);
        radioNew.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (radioNew.getSelection()) {
                    setMoveMode(false);
                }
            }
        });
        Button radioMove = toolkit.createButton(fieldsComposite, Messages
            .getString("Cabinet.button.move.text"), SWT.RADIO); //$NON-NLS-1$
        gd = new GridData();
        gd.horizontalSpan = 2;
        radioMove.setLayoutData(gd);
        radioMove.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                if (!radioNew.getSelection()) {
                    setMoveMode(true);
                }
            }
        });

        // Patient number + visits list
        linkFormPatientManagement.createPatientNumberText(fieldsComposite);
        linkFormPatientManagement
            .setPatientTextCallback(new PatientTextCallback() {
                @Override
                public void focusLost() {
                    setTypeCombosLists();
                }

                @Override
                public void textModified() {
                    viewerSampleTypes.setInput(null);
                }
            });

        linkFormPatientManagement.createVisitCombo(fieldsComposite);
        linkFormPatientManagement.createVisitText(fieldsComposite);

        // inventoryID
        inventoryIDValidator = new CabinetInventoryIDValidator();
        inventoryIdText = (Text) createBoundWidgetWithLabel(fieldsComposite,
            Text.class, SWT.NONE, Messages
                .getString("Cabinet.inventoryId.label"), new String[0], //$NON-NLS-1$
            BeansObservables.observeValue(aliquot, "inventoryId"), //$NON-NLS-1$
            inventoryIDValidator);
        gd = (GridData) inventoryIdText.getLayoutData();
        gd.horizontalSpan = 2;
        inventoryIdText.addKeyListener(EnterKeyToNextFieldListener.INSTANCE);
        inventoryIdText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (inventoryIdModified && !radioNew.getSelection()) {
                    // Move Mode only
                    try {
                        retrieveAliquotDataForMoving();
                    } catch (Exception ex) {
                        BioBankPlugin
                            .openAsyncError("Move - aliquot error", ex); //$NON-NLS-1$
                    }
                }
                inventoryIdModified = false;
            }
        });
        inventoryIdText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                inventoryIdModified = true;
            }
        });

        createPositionFields(fieldsComposite);

        createTypeCombo(fieldsComposite);

        checkButton = toolkit.createButton(fieldsComposite, Messages
            .getString("Cabinet.checkButton.text"), //$NON-NLS-1$
            SWT.PUSH);
        gd = new GridData();
        gd.horizontalSpan = 3;
        checkButton.setLayoutData(gd);
        checkButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                checkPositionAndAliquot();
            }
        });
        addBooleanBinding(new WritableValue(Boolean.TRUE, Boolean.class),
            canLaunchCheck, Messages.getString("Cabinet.canLaunchCheck.msg"));
    }

    /**
     * Only one in creation mode. 3 fields in move mode
     */
    private void createPositionFields(Composite fieldsComposite) {
        GridData gd;
        // for move mode: display old position retrieved from database
        oldCabinetPositionLabel = widgetCreator.createLabel(fieldsComposite,
            Messages.getString("Cabinet.old.position.label"));
        oldCabinetPositionLabel.setLayoutData(new GridData(
            GridData.VERTICAL_ALIGN_BEGINNING));
        oldCabinetPosition = (Text) widgetCreator.createBoundWidget(
            fieldsComposite, Text.class, SWT.NONE, oldCabinetPositionLabel,
            new String[0], null, null);
        gd = (GridData) oldCabinetPosition.getLayoutData();
        gd.horizontalSpan = 2;
        oldCabinetPosition.setEnabled(false);

        // for move mode: field to enter old position. Check needed to be sure
        // nothing is wrong with the aliquot
        oldCabinetPositionCheckLabel = widgetCreator.createLabel(
            fieldsComposite, Messages
                .getString("Cabinet.old.position.check.label"));
        oldCabinetPositionCheckLabel.setLayoutData(new GridData(
            GridData.VERTICAL_ALIGN_BEGINNING));
        oldCabinetPositionCheckValidator = new AbstractValidator(
            "Enter correct old position") {
            @Override
            public IStatus validate(Object value) {
                if (value != null && !(value instanceof String)) {
                    throw new RuntimeException(
                        "Not supposed to be called for non-strings.");
                }

                if (value != null) {
                    String s = (String) value;
                    if (s.equals(oldCabinetPosition.getText())) {
                        hideDecoration();
                        return Status.OK_STATUS;
                    }
                }
                showDecoration();
                return ValidationStatus.error(errorMessage);
            }
        };
        oldCabinetPositionCheck = (Text) widgetCreator.createBoundWidget(
            fieldsComposite, Text.class, SWT.NONE,
            oldCabinetPositionCheckLabel, new String[0], new WritableValue("",
                String.class), oldCabinetPositionCheckValidator);
        gd = (GridData) oldCabinetPositionCheck.getLayoutData();
        gd.horizontalSpan = 2;

        // for all modes: position to be assigned to the aliquot
        newCabinetPositionLabel = widgetCreator.createLabel(fieldsComposite,
            Messages.getString("Cabinet.position.label"));
        newCabinetPositionLabel.setLayoutData(new GridData(
            GridData.VERTICAL_ALIGN_BEGINNING));
        newCabinetPositionValidator = new CabinetLabelValidator(Messages
            .getString("Cabinet.position.validationMsg"));
        displayOldCabinetFields(false);
        newCabinetPosition = (Text) widgetCreator.createBoundWidget(
            fieldsComposite, Text.class, SWT.NONE, newCabinetPositionLabel,
            new String[0],
            new WritableValue("", String.class), newCabinetPositionValidator); //$NON-NLS-1$
        gd = (GridData) newCabinetPosition.getLayoutData();
        gd.horizontalSpan = 2;
        newCabinetPosition.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (positionTextModified
                    && newCabinetPositionValidator.validate(newCabinetPosition
                        .getText()) == Status.OK_STATUS) {
                    BusyIndicator.showWhile(PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell().getDisplay(),
                        new Runnable() {
                            @Override
                            public void run() {
                                initContainersFromPosition();
                                int typeListSize = setTypeCombosLists();
                                if (typeListSize == 0) {
                                    newCabinetPosition.setFocus();
                                }
                            }
                        });
                }
                positionTextModified = false;
            }
        });
        newCabinetPosition.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                positionTextModified = true;
                if (radioNew.getSelection()) {
                    viewerSampleTypes.setInput(null);
                }
            }
        });
        displayOldCabinetFields(false);
    }

    private void displayOldCabinetFields(boolean displayOld) {
        oldCabinetPositionLabel.setVisible(displayOld);
        ((GridData) oldCabinetPositionLabel.getLayoutData()).exclude = !displayOld;
        oldCabinetPosition.setVisible(displayOld);
        ((GridData) oldCabinetPosition.getLayoutData()).exclude = !displayOld;
        oldCabinetPositionCheckLabel.setVisible(displayOld);
        ((GridData) oldCabinetPositionCheckLabel.getLayoutData()).exclude = !displayOld;
        oldCabinetPositionCheck.setVisible(displayOld);
        ((GridData) oldCabinetPositionCheck.getLayoutData()).exclude = !displayOld;
        if (displayOld) {
            newCabinetPositionLabel.setText(Messages
                .getString("Cabinet.new.position.label"));
            oldCabinetPositionCheck.setText("");
        } else {
            newCabinetPositionLabel.setText(Messages
                .getString("Cabinet.position.label"));
            oldCabinetPositionCheck.setText(oldCabinetPosition.getText());
        }
        form.layout(true, true);
    }

    protected void initContainersFromPosition() {
        try {
            String binLabel = newCabinetPosition.getText().substring(0, 6);
            List<ContainerWrapper> foundContainers = ContainerWrapper
                .getContainersInSite(appService, SessionManager.getInstance()
                    .getCurrentSite(), binLabel);
            List<ContainerWrapper> cabinetContainers = new ArrayList<ContainerWrapper>();
            for (ContainerWrapper container : foundContainers) {
                ContainerWrapper cont = container;
                while (cont.getParent() != null) {
                    cont = cont.getParent();
                }
                if (cabinetContainerTypes.contains(cont.getContainerType())) {
                    cabinetContainers.add(container);
                }
            }
            if (cabinetContainers.size() == 1) {
                bin = cabinetContainers.get(0);
                drawer = bin.getParent();
                cabinet = drawer.getParent();
            } else if (cabinetContainers.size() == 0) {
                String errorMsg = Messages.getFormattedString(
                    "Cabinet.activitylog.checkParent.error.found", binLabel); //$NON-NLS-1$
                BioBankPlugin.openAsyncError(
                    "Check position and aliquot", errorMsg); //$NON-NLS-1$
                appendLogNLS("Cabinet.activitylog.checkParent.error", errorMsg); //$NON-NLS-1$
                viewerSampleTypes.getCombo().setEnabled(false);
                return;
            } else {
                BioBankPlugin.openAsyncError("Container problem",
                    "More than one container found for " + binLabel //$NON-NLS-1$
                        + " --- should do something"); //$NON-NLS-1$
                viewerSampleTypes.getCombo().setEnabled(false);
                return;
            }
        } catch (Exception ex) {
            BioBankPlugin.openAsyncError("Init container from position", ex);
        }
    }

    protected void setMoveMode(boolean moveMode) {
        try {
            reset();

            linkFormPatientManagement.enabledPatientText(!moveMode);
            linkFormPatientManagement.enabledVisitsList(!moveMode);
            linkFormPatientManagement.enableValidators(!moveMode);
            inventoryIDValidator.setManageOldInventoryIDs(moveMode);
            // Validator has change: we need to re-validate
            inventoryIDValidator.validate("");
            displayOldCabinetFields(moveMode);
            enableAndShowSampleTypeCombo(!moveMode);
            canLaunchCheck.setValue(true);

            form.layout(true, true);
        } catch (Exception ex) {
            BioBankPlugin.openAsyncError("Error setting move mode " + moveMode, //$NON-NLS-1$
                ex);
        }
    }

    private void enableAndShowSampleTypeCombo(boolean enable) {
        if (enable) {
            widgetCreator.addBinding(SAMPLE_TYPE_LIST_BINDING);
        } else {
            widgetCreator.removeBinding(SAMPLE_TYPE_LIST_BINDING);
        }
        viewerSampleTypes.getCombo().setEnabled(enable);
        widgetCreator.showWidget(sampleTypeComboLabel, enable);
        widgetCreator.showWidget(viewerSampleTypes.getCombo(), enable);
        widgetCreator.showWidget(sampleTypeTextLabel, !enable);
        widgetCreator.showWidget(sampleTypeText, !enable);
    }

    private void createTypeCombo(Composite fieldsComposite)
        throws ApplicationException {
        initCabinetContainerTypesList();
        sampleTypeComboLabel = widgetCreator.createLabel(fieldsComposite,
            Messages.getString("Cabinet.sampleType.label"));
        viewerSampleTypes = widgetCreator
            .createComboViewerWithNoSelectionValidator(
                fieldsComposite,
                sampleTypeComboLabel,
                null,
                null,
                Messages.getString("Cabinet.sampleType.validationMsg"), true, SAMPLE_TYPE_LIST_BINDING); //$NON-NLS-1$
        GridData gd = (GridData) viewerSampleTypes.getCombo().getLayoutData();
        gd.horizontalSpan = 2;
        viewerSampleTypes
            .addSelectionChangedListener(new ISelectionChangedListener() {
                @Override
                public void selectionChanged(SelectionChangedEvent event) {
                    IStructuredSelection stSelection = (IStructuredSelection) viewerSampleTypes
                        .getSelection();
                    aliquot.setSampleType((SampleTypeWrapper) stSelection
                        .getFirstElement());
                }
            });

        // for move mode
        sampleTypeTextLabel = widgetCreator.createLabel(fieldsComposite,
            Messages.getString("Cabinet.sampleType.label"));
        sampleTypeTextLabel.setLayoutData(new GridData(
            GridData.VERTICAL_ALIGN_BEGINNING));
        sampleTypeText = (Text) widgetCreator.createBoundWidget(
            fieldsComposite, Text.class, SWT.NONE, sampleTypeTextLabel,
            new String[0], null, null);
        ((GridData) sampleTypeText.getLayoutData()).horizontalSpan = 2;
        sampleTypeText.setEnabled(false);
    }

    private void initCabinetContainerTypesList() throws ApplicationException {
        cabinetContainerTypes = ContainerTypeWrapper.getContainerTypesInSite(
            appService, SessionManager.getInstance().getCurrentSite(),
            cabinetNameContains, false);
    }

    private List<SampleTypeWrapper> getCabinetSampleTypes()
        throws ApplicationException {
        if (cabinetSampleTypes == null) {
            cabinetSampleTypes = SampleTypeWrapper
                .getSampleTypeForContainerTypes(appService, SessionManager
                    .getInstance().getCurrentSite(), cabinetNameContains);
        }
        return cabinetSampleTypes;
    }

    protected void checkPositionAndAliquot() {
        BusyIndicator.showWhile(Display.getDefault(), new Runnable() {
            public void run() {
                try {
                    appendLog("----"); //$NON-NLS-1$
                    PatientVisitWrapper pv = linkFormPatientManagement
                        .getSelectedPatientVisit();
                    aliquot.setPatientVisit(pv);
                    if (radioNew.getSelection()) {
                        appendLogNLS("Cabinet.activitylog.checkingId", //$NON-NLS-1$
                            aliquot.getInventoryId());
                        aliquot.checkInventoryIdUnique();
                    }
                    String positionString = newCabinetPosition.getText();
                    if (bin == null) {
                        resultShownValue.setValue(Boolean.FALSE);
                        hidePositions();
                        return;
                    }
                    appendLogNLS(
                        "Cabinet.activitylog.checkingPosition", positionString); //$NON-NLS-1$
                    aliquot.setAliquotPositionFromString(positionString, bin);
                    if (aliquot.isPositionFree(bin)) {
                        aliquot.setParent(bin);
                        showPositions();
                        resultShownValue.setValue(Boolean.TRUE);
                        cancelConfirmWidget.setFocus();
                    } else {
                        BioBankPlugin.openAsyncError("Position not free",
                            Messages.getFormattedString(
                                "Cabinet.checkStatus.error", positionString, //$NON-NLS-1$
                                bin.getLabel()));
                        appendLogNLS(
                            "Cabinet.activitylog.checkPosition.error", positionString, bin.getLabel()); //$NON-NLS-1$
                        return;
                    }
                    setDirty(true);
                } catch (RemoteConnectFailureException exp) {
                    BioBankPlugin.openRemoteConnectErrorMessage();
                } catch (BiobankCheckException bce) {
                    BioBankPlugin.openAsyncError(
                        "Error while checking position", bce); //$NON-NLS-1$
                    appendLog("ERROR: " + bce.getMessage()); //$NON-NLS-1$
                    resultShownValue.setValue(Boolean.FALSE);
                } catch (Exception e) {
                    BioBankPlugin.openAsyncError(
                        "Error while checking position", e); //$NON-NLS-1$
                }
            }

        });
    }

    /**
     * Get sample types only defined in the patient's study and available in
     * current selected bin. Then set these types to the types combo
     * 
     * @return the size of type combo list
     */
    private int setTypeCombosLists() {
        viewerSampleTypes.getCombo().setEnabled(true);
        List<SampleTypeWrapper> studiesSampleTypes = new ArrayList<SampleTypeWrapper>();
        if (linkFormPatientManagement.getCurrentPatient() != null
            && bin != null) {
            List<SampleTypeWrapper> binTypes = bin.getContainerType()
                .getSampleTypeCollection();
            StudyWrapper study = linkFormPatientManagement.getCurrentPatient()
                .getStudy();
            try {
                // need to reload study to avoid performance problem when using
                // the same lots of time (like is try differents positions for
                // same patient)
                study.reload();
            } catch (Exception e) {
                BioBankPlugin.openAsyncError("Problem reloading study", e);
            }
            for (SampleStorageWrapper ss : study.getSampleStorageCollection()) {
                if (ss.getActivityStatus().isActive()) {
                    SampleTypeWrapper type = ss.getSampleType();
                    if (binTypes.contains(type)) {
                        studiesSampleTypes.add(type);
                    }
                }
            }
            if (studiesSampleTypes.size() == 0) {
                String studyText = "unknown";
                if (linkFormPatientManagement.getCurrentPatient() != null) {
                    studyText = linkFormPatientManagement.getCurrentPatient()
                        .getStudy().getNameShort();
                }
                BioBankPlugin.openError("No Sample Types",
                    "There are no sample types that "
                        + "are defined for current patient study (" + studyText
                        + ") and that are defined as possible for bin "
                        + bin.getLabel());
            }
            if (!radioNew.getSelection()) {
                // Move
                SampleTypeWrapper type = aliquot.getSampleType();
                if (!studiesSampleTypes.contains(type)
                    && binTypes.contains(type)) {
                    // in move mode, the sample source could be deactivate
                    studiesSampleTypes.add(type);
                }
            }
        }
        viewerSampleTypes.setInput(studiesSampleTypes);
        if (radioNew.getSelection()) {
            viewerSampleTypes.getCombo().setEnabled(true);
            if (studiesSampleTypes.size() == 1) {
                viewerSampleTypes.getCombo().select(0);
                aliquot.setSampleType(studiesSampleTypes.get(0));
            } else {
                viewerSampleTypes.getCombo().deselectAll();
                aliquot.setSampleType(null);
            }
        } else {
            viewerSampleTypes.setSelection(new StructuredSelection(aliquot
                .getSampleType()));
            viewerSampleTypes.getCombo().setEnabled(false);
        }
        return studiesSampleTypes.size();
    }

    /**
     * In move mode, get informations from the existing aliquot
     * 
     * @throws Exception
     */
    protected void retrieveAliquotDataForMoving() throws Exception {
        String inventoryId = inventoryIdText.getText();
        if (inventoryId.isEmpty()) {
            return;
        }
        if (inventoryId.length() == 4) {
            // compatibility with old aliquots imported
            // 4 letters aliquots are now C+4letters
            inventoryId = "C" + inventoryId; //$NON-NLS-1$
        }
        resultShownValue.setValue(false);
        reset();
        aliquot.setInventoryId(inventoryId);
        inventoryIdText.setText(inventoryId);
        oldCabinetPositionCheck.setText("?");

        appendLogNLS("Cabinet.activitylog.gettingInfoId", //$NON-NLS-1$
            aliquot.getInventoryId());
        List<AliquotWrapper> aliquots = AliquotWrapper.getAliquotsInSite(
            appService, aliquot.getInventoryId(), SessionManager.getInstance()
                .getCurrentSite());
        if (aliquots.size() > 1) {
            canLaunchCheck.setValue(false);
            throw new Exception(
                "Error while retrieving aliquot with inventoryId " //$NON-NLS-1$
                    + aliquot.getInventoryId()
                    + ": more than one aliquot found."); //$NON-NLS-1$
        }
        if (aliquots.size() == 0) {
            canLaunchCheck.setValue(false);
            throw new Exception("No aliquot found with inventoryId " //$NON-NLS-1$
                + aliquot.getInventoryId());
        }
        aliquot.initObjectWith(aliquots.get(0));
        List<SampleTypeWrapper> possibleTypes = getCabinetSampleTypes();
        if (!possibleTypes.contains(aliquot.getSampleType())) {
            canLaunchCheck.setValue(false);
            throw new Exception(
                "This aliquot is of type " + aliquot.getSampleType().getNameShort() //$NON-NLS-1$
                    + ": this is not a cabinet type");
        }
        canLaunchCheck.setValue(true);
        PatientWrapper patient = aliquot.getPatientVisit().getPatient();
        linkFormPatientManagement.setCurrentPatientAndVisit(patient, aliquot
            .getPatientVisit());
        String positionString = aliquot.getPositionString(true, false);
        if (positionString == null) {
            widgetCreator.hideWidget(oldCabinetPositionCheckLabel);
            widgetCreator.hideWidget(oldCabinetPositionCheck);
            oldCabinetPositionCheck.setText("");
            positionString = "none"; //$NON-NLS-1$
        } else {
            widgetCreator.showWidget(oldCabinetPositionCheckLabel);
            widgetCreator.showWidget(oldCabinetPositionCheck);
            oldCabinetPositionCheck.setText(oldCabinetPositionCheck.getText());
        }
        oldCabinetPosition.setText(positionString);
        sampleTypeText.setText(aliquot.getSampleType().getNameShort());
        form.layout(true, true);
        appendLogNLS(
            "Cabinet.activitylog.aliquotInfo", aliquot.getInventoryId(), //$NON-NLS-1$
            positionString);
    }

    private void showPositions() {
        if (drawer == null || bin == null || cabinet == null) {
            cabinetWidget.setSelection(null);
            cabinetLabel.setText("Cabinet"); //$NON-NLS-1$
            drawerWidget.setSelection(null);
            drawerLabel.setText("Drawer"); //$NON-NLS-1$
        } else {
            cabinetWidget.setContainerType(cabinet.getContainerType());
            cabinetWidget.setSelection(drawer.getPosition());
            cabinetLabel.setText("Cabinet " + cabinet.getLabel()); //$NON-NLS-1$
            drawerWidget.setSelection(bin.getPosition());
            drawerLabel.setText("Drawer " + drawer.getLabel()); //$NON-NLS-1$
        }
        form.layout(true, true);
    }

    private void hidePositions() {
        cabinet = null;
        bin = null;
        drawer = null;
        showPositions();
    }

    protected void initParentContainersFromPosition(String positionString)
        throws Exception {
        bin = null;
        String binLabel = positionString.substring(0, 6);
        appendLogNLS("Cabinet.activitylog.checkingParent", binLabel, //$NON-NLS-1$ 
            aliquot.getSampleType().getName());
        List<ContainerWrapper> containers = ContainerWrapper
            .getContainersHoldingSampleType(appService, SessionManager
                .getInstance().getCurrentSite(), binLabel, aliquot
                .getSampleType());
        if (containers.size() == 1) {
            bin = containers.get(0);
            drawer = bin.getParent();
            cabinet = drawer.getParent();
        } else if (containers.size() == 0) {
            containers = ContainerWrapper.getContainersInSite(appService,
                SessionManager.getInstance().getCurrentSite(), binLabel);
            String errorMsg = null;
            if (containers.size() > 0) {
                errorMsg = Messages.getFormattedString(
                    "Cabinet.activitylog.checkParent.error.type", binLabel, //$NON-NLS-1$
                    aliquot.getSampleType().getName());
            } else {
                errorMsg = Messages.getFormattedString(
                    "Cabinet.activitylog.checkParent.error.found", binLabel); //$NON-NLS-1$
            }
            if (errorMsg != null) {
                BioBankPlugin.openError("Check position and aliquot", errorMsg); //$NON-NLS-1$
                appendLogNLS("Cabinet.activitylog.checkParent.error", errorMsg); //$NON-NLS-1$
            }
            return;
        } else {
            throw new Exception("More than one container found for " + binLabel //$NON-NLS-1$
                + " --- should do something"); //$NON-NLS-1$
        }
    }

    @Override
    public void reset() throws Exception {
        aliquot.resetToNewObject();
        aliquot.reset(); // reset internal values
        cabinet = null;
        drawer = null;
        bin = null;
        cabinetWidget.setSelection(null);
        drawerWidget.setSelection(null);
        resultShownValue.setValue(Boolean.FALSE);
        linkFormPatientManagement.reset(true);
        // the 2 following lines are needed. The validator won't update if don't
        // do that (why ?)
        inventoryIdText.setText("**"); //$NON-NLS-1$ 
        inventoryIdText.setText(""); //$NON-NLS-1$
        oldCabinetPosition.setText("");
        oldCabinetPositionCheck.setText(""); //$NON-NLS-1$
        newCabinetPosition.setText(""); //$NON-NLS-1$
        sampleTypeText.setText("");
        if (viewerSampleTypes.getCombo().getItemCount() > 1) {
            viewerSampleTypes.getCombo().deselectAll();
        }
        setDirty(false);
    }

    @Override
    protected void saveForm() throws Exception {
        if (radioNew.getSelection()) {
            aliquot.setLinkDate(new Date());
            aliquot.setQuantityFromType();
            aliquot.setActivityStatus(ActivityStatusWrapper
                .getActiveActivityStatus(appService));
        }
        aliquot.persist();
        String posStr = aliquot.getPositionString(true, false);
        if (posStr == null) {
            posStr = "none"; //$NON-NLS-1$
        }
        String msgString = "";
        if (radioNew.getSelection()) {
            msgString = "Cabinet.activitylog.aliquot.saveNew"; //$NON-NLS-1$
        } else {
            msgString = "Cabinet.activitylog.aliquot.saveMove"; //$NON-NLS-1$
        }
        appendLogNLS(msgString, posStr, aliquot.getInventoryId(), aliquot
            .getSampleType().getName(), linkFormPatientManagement
            .getCurrentPatient().getPnumber(), aliquot.getPatientVisit()
            .getFormattedDateProcessed(), aliquot.getPatientVisit()
            .getShipment().getClinic().getName());
        setFinished(false);
    }

    @Override
    protected String getOkMessage() {
        return "Add cabinet aliquots."; //$NON-NLS-1$
    }

    @Override
    protected void handleStatusChanged(IStatus status) {
        if (status.getSeverity() == IStatus.OK) {
            form.setMessage(getOkMessage(), IMessageProvider.NONE);
            cancelConfirmWidget.setConfirmEnabled(true);
            setConfirmEnabled(true);
            checkButton.setEnabled(true);
        } else {
            form.setMessage(status.getMessage(), IMessageProvider.ERROR);
            cancelConfirmWidget.setConfirmEnabled(false);
            setConfirmEnabled(false);
            checkButton.setEnabled(canLaunchCheck.getValue().equals(true));
            if (status.getMessage() != null
                && status.getMessage().contentEquals(
                    Messages.getString("Cabinet.checkButton.validationMsg"))) {
                checkButton.setEnabled(true);
            } else {
                checkButton.setEnabled(false);
            }
        }
    }

    @Override
    public String getNextOpenedFormID() {
        return ID;
    }

    @Override
    protected String getActivityTitle() {
        return "Cabinet link/assign activity"; //$NON-NLS-1$
    }

    @Override
    public BiobankLogger getErrorLogger() {
        return logger;
    }

    @Override
    public boolean onClose() {
        linkFormPatientManagement.onClose();
        return super.onClose();
    }
}
