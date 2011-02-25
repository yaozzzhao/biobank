package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;

import edu.ualberta.med.biobank.BioBankPlugin;
import edu.ualberta.med.biobank.Messages;
import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.common.util.RowColPos;
import edu.ualberta.med.biobank.common.wrappers.ActivityStatusWrapper;
import edu.ualberta.med.biobank.common.wrappers.SpecimenWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerLabelingSchemeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerTypeWrapper;
import edu.ualberta.med.biobank.common.wrappers.ContainerWrapper;
import edu.ualberta.med.biobank.common.wrappers.ProcessingEventWrapper;
import edu.ualberta.med.biobank.common.wrappers.SiteWrapper;
import edu.ualberta.med.biobank.forms.listener.EnterKeyToNextFieldListener;
import edu.ualberta.med.biobank.logs.BiobankLogger;
import edu.ualberta.med.biobank.model.CellStatus;
import edu.ualberta.med.biobank.model.PalletCell;
import edu.ualberta.med.biobank.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.widgets.BiobankText;
import edu.ualberta.med.biobank.widgets.grids.ContainerDisplayWidget;
import edu.ualberta.med.biobank.widgets.grids.ScanPalletDisplay;
import edu.ualberta.med.biobank.widgets.grids.ScanPalletWidget;
import edu.ualberta.med.biobank.widgets.utils.ComboSelectionUpdate;
import edu.ualberta.med.scannerconfig.dmscanlib.ScanCell;
import gov.nih.nci.system.applicationservice.ApplicationException;

public class ScanAssignEntryForm extends AbstractPalletSpecimenAdminForm {

    public static final String ID = "edu.ualberta.med.biobank.forms.ScanAssignEntryForm"; //$NON-NLS-1$

    private static BiobankLogger logger = BiobankLogger
        .getLogger(ScanAssignEntryForm.class.getName());

    private BiobankText palletproductBarcodeText;
    private BiobankText palletPositionText;
    private ComboViewer palletTypesViewer;

    private Label freezerLabel;
    private ContainerDisplayWidget freezerWidget;
    private Label palletLabel;
    private ScanPalletWidget palletWidget;
    private Label hotelLabel;
    private ContainerDisplayWidget hotelWidget;

    protected ContainerWrapper currentPalletWrapper;

    // for debugging only (fake scan) :
    private Button fakeScanLinkedOnlyButton;

    private ScrolledComposite containersScroll;
    private Composite containersComposite;

    // global state of the pallet process
    private CellStatus currentScanState;

    // contains moved and missing aliquots. a missing one is set to into the
    // missing rowColPos. A moved one is set into its old RowColPos
    private Map<RowColPos, PalletCell> movedAndMissingAliquotsFromPallet = new HashMap<RowColPos, PalletCell>();

    private Composite fieldsComposite;

    protected boolean palletproductBarcodeTextModified;

    protected boolean palletPositionTextModified;

    private List<ContainerTypeWrapper> palletContainerTypes;

    private NonEmptyStringValidator productBarcodeValidator;

    private NonEmptyStringValidator palletLabelValidator;

    // Label of the pallet found with given product barcode
    private String palletFoundWithProductBarcodeLabel;

    private ContainerWrapper containerToRemove;

    private boolean modificationMode;

    private IObservableValue validationMade = new WritableValue(Boolean.TRUE,
        Boolean.class);

    protected boolean useNewProductBarcode;

    private boolean isFakeScanLinkedOnly;

    private Control nextFocusWidget;

    private SiteWrapper currentSiteSelected;

    private boolean saveEvenIfMissing;

    @Override
    protected void init() throws Exception {
        super.init();
        setPartName(Messages.getString("ScanAssign.tabTitle")); //$NON-NLS-1$
        currentPalletWrapper = new ContainerWrapper(appService);
        initPalletValues();
        addBooleanBinding(new WritableValue(Boolean.TRUE, Boolean.class),
            validationMade, "Validation needed: hit enter"); //$NON-NLS-1$
    }

    @Override
    protected void createFormContent() throws Exception {
        form.setText(Messages.getString("ScanAssign.formTitle")); //$NON-NLS-1$
        GridLayout layout = new GridLayout(2, false);
        GridData gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        page.setLayout(layout);
        page.setLayoutData(gd);

        createFieldsSection();

        createContainersVisualisationSection();

        createCancelConfirmWidget();
    }

    private void createFieldsSection() throws Exception {
        Composite leftSideComposite = toolkit.createComposite(page);
        GridLayout layout = new GridLayout(2, false);
        layout.horizontalSpacing = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        leftSideComposite.setLayout(layout);
        toolkit.paintBordersFor(leftSideComposite);
        GridData gd = new GridData();
        gd.widthHint = 400;
        gd.verticalAlignment = SWT.TOP;
        leftSideComposite.setLayoutData(gd);

        fieldsComposite = toolkit.createComposite(leftSideComposite);
        layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        fieldsComposite.setLayout(layout);
        toolkit.paintBordersFor(fieldsComposite);
        gd = new GridData();
        gd.widthHint = 400;
        gd.verticalAlignment = SWT.TOP;
        gd.horizontalSpan = 2;
        fieldsComposite.setLayoutData(gd);

        createSiteCombo(fieldsComposite, true);

        productBarcodeValidator = new NonEmptyStringValidator( //$NON-NLS-1$
            Messages.getString("ScanAssign.productBarcode.validationMsg"));
        palletLabelValidator = new NonEmptyStringValidator(
            Messages.getString("ScanAssign.palletLabel.validationMsg"));

        palletproductBarcodeText = (BiobankText) createBoundWidgetWithLabel(
            fieldsComposite,
            BiobankText.class,
            SWT.NONE,
            Messages.getString("ScanAssign.productBarcode.label"), //$NON-NLS-1$
            null, currentPalletWrapper,
            "productBarcode", productBarcodeValidator); //$NON-NLS-1$
        palletproductBarcodeText.addKeyListener(textFieldKeyListener);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        palletproductBarcodeText.setLayoutData(gd);

        palletproductBarcodeText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (palletproductBarcodeTextModified
                    && productBarcodeValidator.validate(
                        currentPalletWrapper.getProductBarcode()).equals(
                        Status.OK_STATUS)) {
                    validateValues();
                }
                palletproductBarcodeTextModified = false;
            }
        });
        palletproductBarcodeText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!modificationMode) {
                    palletproductBarcodeTextModified = true;
                    validationMade.setValue(false);
                }
            }
        });

        palletPositionText = (BiobankText) createBoundWidgetWithLabel(
            fieldsComposite, BiobankText.class, SWT.NONE,
            Messages.getString("ScanAssign.palletLabel.label"), null, //$NON-NLS-1$
            BeansObservables.observeValue(currentPalletWrapper, "label"), //$NON-NLS-1$
            palletLabelValidator); //$NON-NLS-1$
        palletPositionText.addKeyListener(EnterKeyToNextFieldListener.INSTANCE);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        palletPositionText.setLayoutData(gd);
        palletPositionText.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (palletPositionTextModified) {
                    validateValues();
                }
                palletPositionTextModified = false;
            }
        });
        palletPositionText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (!modificationMode) {
                    palletPositionTextModified = true;
                    validationMade.setValue(false);
                }
            }
        });

        createPalletTypesViewer(fieldsComposite);

        createPlateToScanField(fieldsComposite);

        createScanButton(leftSideComposite);
    }

    protected void validateValues() {
        // if null, initialization of all fields is not finished
        if (productBarcodeValidator != null) {
            nextFocusWidget = null;
            modificationMode = true;
            try {
                if (productBarcodeValidator.validate(
                    currentPalletWrapper.getProductBarcode()).equals(
                    Status.OK_STATUS)) {
                    reset(true);
                    boolean canLaunch = true;
                    boolean exists = getExistingPalletFromProductBarcode();
                    if ((!exists || !palletFoundWithProductBarcodeLabel
                        .equals(currentPalletWrapper.getLabel()))
                        && palletLabelValidator.validate(
                            currentPalletWrapper.getLabel()).equals(
                            Status.OK_STATUS)) {
                        canLaunch = checkPallet();
                    }
                    setCanLaunchScan(canLaunch);
                }
            } catch (Exception ex) {
                BioBankPlugin.openError("Values validation", ex); //$NON-NLS-1$
                appendLogNLS("ScanAssign.activitylog.error", //$NON-NLS-1$
                    ex.getMessage());
                if (ex.getMessage() != null
                    && ex.getMessage().startsWith(
                        "Can't find container with label")) {
                    // FIXME: find a better way than that ? Add info inside the
                    // BiobankCheckException ?
                    nextFocusWidget = palletPositionText;
                }

                setCanLaunchScan(false);
            }
            if (nextFocusWidget != null) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run() {
                        nextFocusWidget.setFocus();
                    }
                });
            }
            modificationMode = false;
            validationMade.setValue(true);
        }
    }

    @Override
    protected boolean fieldsValid() {
        IStructuredSelection selection = (IStructuredSelection) palletTypesViewer
            .getSelection();
        return isPlateValid()
            && productBarcodeValidator.validate(
                palletproductBarcodeText.getText()).equals(Status.OK_STATUS)
            && palletLabelValidator.validate(palletPositionText.getText())
                .equals(Status.OK_STATUS) && selection.size() > 0;
    }

    private void createPalletTypesViewer(Composite parent) throws Exception {
        palletContainerTypes = getPalletContainerTypes();
        palletTypesViewer = createComboViewer(
            parent,
            Messages.getString("ScanAssign.palletType.label"), //$NON-NLS-1$
            palletContainerTypes, null,
            Messages.getString("ScanAssign.palletType.validationMsg"),
            new ComboSelectionUpdate() {
                @Override
                public void doSelection(Object selectedObject) {
                    if (!modificationMode) {
                        ContainerTypeWrapper oldContainerType = currentPalletWrapper
                            .getContainerType();
                        currentPalletWrapper
                            .setContainerType((ContainerTypeWrapper) selectedObject);
                        if (oldContainerType != null) {
                            validateValues();
                        }
                        palletTypesViewer.getCombo().setFocus();
                    }
                }
            }); //$NON-NLS-1$
        if (palletContainerTypes.size() == 1) {
            currentPalletWrapper.setContainerType(palletContainerTypes.get(0));
            palletTypesViewer.setSelection(new StructuredSelection(
                palletContainerTypes.get(0)));
        }
    }

    /**
     * get container with type name that contains 'palletNameContains'
     */
    private List<ContainerTypeWrapper> getPalletContainerTypes()
        throws ApplicationException {
        SiteWrapper site = siteCombo.getSelectedSite();
        if (site != null) {
            List<ContainerTypeWrapper> palletContainerTypes = ContainerTypeWrapper
                .getContainerTypesPallet96(appService, site);
            if (palletContainerTypes.size() == 0) {
                BioBankPlugin
                    .openAsyncError(
                        Messages
                            .getString("ScanAssign.dialog.noPalletFoundError.title"), //$NON-NLS-1$
                        Messages
                            .getFormattedString("ScanAssign.dialog.noPalletFoundError.msg" //$NON-NLS-1$
                            ));
            }
            return palletContainerTypes;
        }
        return new ArrayList<ContainerTypeWrapper>();
    }

    @Override
    protected void createFakeOptions(Composite fieldsComposite) {
        Composite comp = toolkit.createComposite(fieldsComposite);
        comp.setLayout(new GridLayout());
        GridData gd = new GridData();
        gd.horizontalSpan = 2;
        comp.setLayoutData(gd);
        fakeScanLinkedOnlyButton = toolkit.createButton(comp,
            "Select linked only aliquots", SWT.RADIO); //$NON-NLS-1$
        fakeScanLinkedOnlyButton.setSelection(true);
        toolkit.createButton(comp,
            "Select linked and assigned aliquots", SWT.RADIO); //$NON-NLS-1$
    }

    private void createContainersVisualisationSection() {
        containersScroll = new ScrolledComposite(page, SWT.H_SCROLL);
        containersScroll.setExpandHorizontal(true);
        containersScroll.setExpandVertical(true);
        containersScroll.setLayout(new FillLayout());
        GridData scrollData = new GridData();
        scrollData.horizontalAlignment = SWT.FILL;
        scrollData.grabExcessHorizontalSpace = true;
        containersScroll.setLayoutData(scrollData);
        containersComposite = toolkit.createComposite(containersScroll);
        GridLayout layout = getNeutralGridLayout();
        layout.numColumns = 3;
        containersComposite.setLayout(layout);
        GridData gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = true;
        containersComposite.setLayoutData(gd);
        toolkit.paintBordersFor(containersComposite);

        containersScroll.setContent(containersComposite);

        Composite freezerComposite = toolkit
            .createComposite(containersComposite);
        freezerComposite.setLayout(getNeutralGridLayout());
        GridData gdFreezer = new GridData();
        gdFreezer.horizontalSpan = 3;
        gdFreezer.horizontalAlignment = SWT.RIGHT;
        freezerComposite.setLayoutData(gdFreezer);
        freezerLabel = toolkit.createLabel(freezerComposite, "Freezer"); //$NON-NLS-1$
        freezerLabel.setLayoutData(new GridData());
        freezerWidget = new ContainerDisplayWidget(freezerComposite);
        freezerWidget.initDisplayFromType(true);
        toolkit.adapt(freezerWidget);
        freezerWidget.setDisplaySize(ScanPalletDisplay.PALLET_WIDTH, 100);

        Composite hotelComposite = toolkit.createComposite(containersComposite);
        hotelComposite.setLayout(getNeutralGridLayout());
        hotelComposite.setLayoutData(new GridData());
        hotelLabel = toolkit.createLabel(hotelComposite, "Hotel"); //$NON-NLS-1$
        hotelWidget = new ContainerDisplayWidget(hotelComposite);
        hotelWidget.initDisplayFromType(true);
        toolkit.adapt(hotelWidget);
        hotelWidget.setDisplaySize(100,
            ScanPalletDisplay.PALLET_HEIGHT_AND_LEGEND);

        Composite palletComposite = toolkit
            .createComposite(containersComposite);
        palletComposite.setLayout(getNeutralGridLayout());
        palletComposite.setLayoutData(new GridData());
        palletLabel = toolkit.createLabel(palletComposite, "Pallet"); //$NON-NLS-1$
        palletWidget = new ScanPalletWidget(palletComposite,
            CellStatus.DEFAULT_PALLET_SCAN_ASSIGN_STATUS_LIST);
        toolkit.adapt(palletWidget);
        palletWidget.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                manageDoubleClick(e);
            }
        });
        showOnlyPallet(true);

        containersScroll.setMinSize(containersComposite.computeSize(
            SWT.DEFAULT, SWT.DEFAULT));
        createScanTubeAloneButton(containersComposite);
    }

    protected void manageDoubleClick(MouseEvent e) {
        if (isScanTubeAloneMode()) {
            scanTubeAlone(e);
        } else {
            PalletCell cell = (PalletCell) ((ScanPalletWidget) e.widget)
                .getObjectAtCoordinates(e.x, e.y);
            if (cell != null) {
                switch (cell.getStatus()) {
                case ERROR:
                    // do something ?
                    break;
                case MISSING:
                    SessionManager.openViewForm(cell.getExpectedAliquot());
                    break;
                }
            }
        }
    }

    @Override
    protected boolean canScanTubeAlone(PalletCell cell) {
        return super.canScanTubeAlone(cell)
            || cell.getStatus() == CellStatus.MISSING;
    }

    @Override
    protected void postprocessScanTubeAlone(PalletCell cell) throws Exception {
        processCellStatus(cell);
        currentScanState = currentScanState.mergeWith(cell.getStatus());
        setScanValid(currentScanState != CellStatus.ERROR);
        palletWidget.redraw();
    }

    private GridLayout getNeutralGridLayout() {
        GridLayout layout = new GridLayout(1, false);
        layout.horizontalSpacing = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 0;
        return layout;
    }

    /**
     * @return true if a pallet already exists with this product barcode
     */
    private boolean getExistingPalletFromProductBarcode() throws Exception {
        ContainerWrapper palletFoundWithProductBarcode = null;
        palletFoundWithProductBarcodeLabel = null;
        if (siteCombo.getSelectedSite() != null)
            palletFoundWithProductBarcode = ContainerWrapper
                .getContainerWithProductBarcodeInSite(appService,
                    siteCombo.getSelectedSite(),
                    currentPalletWrapper.getProductBarcode());
        if (palletFoundWithProductBarcode == null) {
            // no pallet found with this barcode
            setTypes(palletContainerTypes, true);
            palletTypesViewer.getCombo().setEnabled(true);
            return false;
        } else {
            // a pallet has been found
            palletFoundWithProductBarcodeLabel = palletFoundWithProductBarcode
                .getLabel();
            String currentLabel = palletPositionText.getText();
            currentPalletWrapper.initObjectWith(palletFoundWithProductBarcode);
            currentPalletWrapper.reset();
            palletPositionText.selectAll();
            palletLabelValidator.validate(palletPositionText.getText());
            palletTypesViewer.getCombo().setEnabled(false);
            palletTypesViewer.setSelection(new StructuredSelection(
                palletFoundWithProductBarcode.getContainerType()));
            appendLogNLS("ScanAssign.activitylog.pallet.productBarcode.exists",
                currentPalletWrapper.getProductBarcode(),
                palletFoundWithProductBarcode.getLabel(), siteCombo
                    .getSelectedSite().getNameShort(),
                palletFoundWithProductBarcode.getContainerType().getName());
            if (!currentLabel.isEmpty()
                && !currentLabel.equals(palletFoundWithProductBarcodeLabel)) {
                currentPalletWrapper.setLabel(currentLabel);
                return false; // we still want to check the new label
            }
            return true;
        }
    }

    private void setTypes(List<ContainerTypeWrapper> types,
        boolean keepCurrentSelection) {
        IStructuredSelection selection = null;
        if (keepCurrentSelection) {
            selection = (IStructuredSelection) palletTypesViewer.getSelection();
        }
        palletTypesViewer.setInput(types);
        if (selection != null) {
            palletTypesViewer.setSelection(selection);
        }
    }

    @Override
    protected void launchScanAndProcessResult() {
        super.launchScanAndProcessResult();
        page.layout(true, true);
        book.reflow(true);
        cancelConfirmWidget.setFocus();
    }

    @Override
    protected void beforeScanThreadStart() {
        showOnlyPallet(false, false);
        currentPalletWrapper
            .setContainerType((ContainerTypeWrapper) ((IStructuredSelection) palletTypesViewer
                .getSelection()).getFirstElement());
        isFakeScanLinkedOnly = fakeScanLinkedOnlyButton != null
            && fakeScanLinkedOnlyButton.getSelection();
        currentSiteSelected = siteCombo.getSelectedSite();
    }

    @Override
    protected void afterScanAndProcess() {
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                cancelConfirmWidget.setFocus();
                displayPalletPositions();
                palletWidget.setCells(getCells());
                setDirty(true);
                setRescanMode();
                page.layout(true, true);
                form.reflow(true);
            }
        });
    }

    @Override
    protected Map<RowColPos, PalletCell> getFakeScanCells() throws Exception {
        if (palletFoundWithProductBarcodeLabel != null) {
            Map<RowColPos, PalletCell> palletScanned = new HashMap<RowColPos, PalletCell>();
            for (RowColPos pos : currentPalletWrapper.getSpecimens().keySet()) {
                if (pos.row != 0 && pos.col != 2) {
                    palletScanned.put(pos, new PalletCell(new ScanCell(pos.row,
                        pos.col, currentPalletWrapper.getSpecimens().get(pos)
                            .getInventoryId())));
                }
            }
            return palletScanned;
        } else {
            if (isFakeScanLinkedOnly) {
                return PalletCell.getRandomAliquotsNotAssigned(appService,
                    currentPalletWrapper.getSite().getId());
            }
            return PalletCell.getRandomAliquotsAlreadyAssigned(appService,
                currentPalletWrapper.getSite().getId());
        }
    }

    /**
     * go through cells retrieved from scan, set status of aliquots to be added
     * to the current pallet
     */
    @Override
    protected void processScanResult(IProgressMonitor monitor) throws Exception {
        Map<RowColPos, SpecimenWrapper> expectedAliquots = currentPalletWrapper
            .getSpecimens();
        currentScanState = CellStatus.EMPTY;
        for (int row = 0; row < currentPalletWrapper.getRowCapacity(); row++) {
            for (int col = 0; col < currentPalletWrapper.getColCapacity(); col++) {
                RowColPos rcp = new RowColPos(row, col);
                monitor.subTask("Processing position "
                    + ContainerLabelingSchemeWrapper.rowColToSbs(rcp));
                PalletCell cell = getCells().get(rcp);
                if (!isRescanMode() || cell == null || cell.getStatus() == null
                    || cell.getStatus() == CellStatus.EMPTY
                    || cell.getStatus() == CellStatus.ERROR
                    || cell.getStatus() == CellStatus.MISSING) {
                    SpecimenWrapper expectedAliquot = null;
                    if (expectedAliquots != null) {
                        expectedAliquot = expectedAliquots.get(rcp);
                        if (expectedAliquot != null) {
                            if (cell == null) {
                                cell = new PalletCell(new ScanCell(rcp.row,
                                    rcp.col, null));
                                getCells().put(rcp, cell);
                            }
                            cell.setExpectedAliquot(expectedAliquot);
                        }
                    }
                    if (cell != null) {
                        processCellStatus(cell);
                    }
                }
                CellStatus newStatus = CellStatus.EMPTY;
                if (cell != null) {
                    newStatus = cell.getStatus();
                }
                currentScanState = currentScanState.mergeWith(newStatus);
            }
        }
        setScanValid(!getCells().isEmpty()
            && currentScanState != CellStatus.ERROR);
    }

    private void showOnlyPallet(boolean show) {
        freezerLabel.getParent().setVisible(!show);
        ((GridData) freezerLabel.getParent().getLayoutData()).exclude = show;
        hotelLabel.getParent().setVisible(!show);
        ((GridData) hotelLabel.getParent().getLayoutData()).exclude = show;
    }

    private void showOnlyPallet(final boolean show, boolean async) {
        if (async) {
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    showOnlyPallet(show);
                }
            });
        } else {
            showOnlyPallet(show);
        }
    }

    protected void displayPalletPositions() {
        if (currentPalletWrapper.hasParent()) {
            ContainerWrapper hotelContainer = currentPalletWrapper.getParent();
            ContainerWrapper freezerContainer = hotelContainer.getParent();

            if (freezerContainer != null) {
                freezerLabel.setText(freezerContainer.getFullInfoLabel());
                freezerWidget.setContainerType(freezerContainer
                    .getContainerType());
                freezerWidget
                    .setSelection(hotelContainer.getPositionAsRowCol());
                freezerWidget.redraw();
            }

            hotelLabel.setText(hotelContainer.getFullInfoLabel());
            hotelWidget.setContainerType(hotelContainer.getContainerType());
            hotelWidget
                .setSelection(currentPalletWrapper.getPositionAsRowCol());
            hotelWidget.redraw();

            palletLabel.setText(currentPalletWrapper.getLabel());
        }
    }

    /**
     * set the status of the cell
     */
    protected void processCellStatus(PalletCell scanCell) throws Exception {
        SpecimenWrapper expectedAliquot = scanCell.getExpectedAliquot();
        String value = scanCell.getValue();
        String positionString = currentPalletWrapper.getLabel()
            + ContainerLabelingSchemeWrapper.rowColToSbs(new RowColPos(scanCell
                .getRow(), scanCell.getCol()));
        if (value == null) { // no aliquot scanned
            updateCellAsMissing(positionString, scanCell, expectedAliquot);
        } else {
            // FIXME test what happen if can't read site
            SpecimenWrapper foundAliquot = SpecimenWrapper.getSpecimen(appService,
                value, SessionManager.getUser());
            if (foundAliquot == null) {
                updateCellAsNotLinked(positionString, scanCell);
            } else if (expectedAliquot != null
                && !foundAliquot.equals(expectedAliquot)) {
                updateCellAsPositionAlreadyTaken(positionString, scanCell,
                    expectedAliquot, foundAliquot);
            } else {
                scanCell.setSpecimen(foundAliquot);
                if (expectedAliquot != null) {
                    // aliquot scanned is already registered at this
                    // position (everything is ok !)
                    scanCell.setStatus(CellStatus.FILLED);
                    scanCell.setTitle(foundAliquot.getProcessingEvent()
                        .getPatient().getPnumber());
                    scanCell.setSpecimen(expectedAliquot);
                } else {
                    if (currentPalletWrapper.canHoldAliquot(foundAliquot)) {
                        if (foundAliquot.hasParent()) { // moved
                            processCellWithPreviousPosition(scanCell,
                                positionString, foundAliquot);
                        } else { // new in pallet
                            if (foundAliquot.isUsedInDispatch()) {
                                updateCellAsDispatchedError(positionString,
                                    scanCell, foundAliquot);
                            } else {
                                scanCell.setStatus(CellStatus.NEW);
                                scanCell.setTitle(foundAliquot
                                    .getProcessingEvent().getPatient()
                                    .getPnumber());
                            }
                        }
                    } else {
                        // pallet can't hold this aliquot type
                        updateCellAsTypeError(positionString, scanCell,
                            foundAliquot);
                    }
                }
            }
        }
    }

    private void updateCellAsDispatchedError(String positionString,
        PalletCell scanCell, SpecimenWrapper foundAliquot) {
        scanCell.setTitle(foundAliquot.getProcessingEvent().getPatient()
            .getPnumber());
        scanCell.setStatus(CellStatus.ERROR);
        scanCell
            .setInformation(Messages
                .getFormattedString("ScanAssign.scanStatus.aliquot.dispatchedError")); //$NON-NLS-1$
        appendLogNLS("ScanAssign.activitylog.aliquot.dispatchedError",
            positionString);

    }

    /**
     * this cell has already a position. Check if it was on the pallet or not
     */
    private void processCellWithPreviousPosition(PalletCell scanCell,
        String positionString, SpecimenWrapper foundAliquot) {
        if (foundAliquot.getParent().getSite()
            .equals(siteCombo.getSelectedSite())) {
            if (foundAliquot.getParent().equals(currentPalletWrapper)) {
                // same pallet
                RowColPos rcp = new RowColPos(scanCell.getRow(),
                    scanCell.getCol());
                if (!foundAliquot.getPosition().equals(rcp)) {
                    // moved inside the same pallet
                    updateCellAsMoved(positionString, scanCell, foundAliquot);
                    RowColPos movedFromPosition = foundAliquot.getPosition();
                    PalletCell missingAliquot = movedAndMissingAliquotsFromPallet
                        .get(movedFromPosition);
                    if (missingAliquot == null) {
                        // missing position has not yet been processed
                        movedAndMissingAliquotsFromPallet.put(
                            movedFromPosition, scanCell);
                    } else {
                        // missing position has already been processed: remove
                        // the
                        // MISSING flag
                        missingAliquot.setStatus(CellStatus.EMPTY);
                        missingAliquot.setTitle("");
                        movedAndMissingAliquotsFromPallet
                            .remove(movedFromPosition);
                    }
                }
            } else {
                // old position was on another pallet
                updateCellAsMoved(positionString, scanCell, foundAliquot);
            }
        } else {
            updateCellAsInOtherSite(positionString, scanCell, foundAliquot);
        }
    }

    private void updateCellAsTypeError(String position, PalletCell scanCell,
        SpecimenWrapper foundAliquot) {
        String palletType = currentPalletWrapper.getContainerType().getName();
        String sampleType = foundAliquot.getSpecimenType().getName();

        scanCell.setTitle(foundAliquot.getProcessingEvent().getPatient()
            .getPnumber());
        scanCell.setStatus(CellStatus.ERROR);
        scanCell.setInformation(Messages.getFormattedString(
            "ScanAssign.scanStatus.aliquot.typeError", palletType, sampleType)); //$NON-NLS-1$
        appendLogNLS(
            "ScanAssign.activitylog.aliquot.typeError", position, palletType, //$NON-NLS-1$
            sampleType);
    }

    private void updateCellAsMoved(String position, PalletCell scanCell,
        SpecimenWrapper foundAliquot) {
        String expectedPosition = foundAliquot.getPositionString(true, false);
        if (expectedPosition == null) {
            expectedPosition = "none"; //$NON-NLS-1$
        }

        scanCell.setStatus(CellStatus.MOVED);
        scanCell.setTitle(foundAliquot.getProcessingEvent().getPatient()
            .getPnumber());
        scanCell.setInformation(Messages.getFormattedString(
            "ScanAssign.scanStatus.aliquot.moved", expectedPosition)); //$NON-NLS-1$

        appendLogNLS(
            "ScanAssign.activitylog.aliquot.moved", position, scanCell.getValue(), //$NON-NLS-1$
            expectedPosition);
    }

    private void updateCellAsInOtherSite(String position, PalletCell scanCell,
        SpecimenWrapper foundAliquot) {
        String currentPosition = foundAliquot.getPositionString(true, false);
        if (currentPosition == null) {
            currentPosition = "none"; //$NON-NLS-1$
        }
        String siteName = foundAliquot.getParent().getSite().getNameShort();
        scanCell.setStatus(CellStatus.ERROR);
        scanCell.setTitle(foundAliquot.getProcessingEvent().getPatient()
            .getPnumber());
        scanCell.setInformation(Messages.getFormattedString(
            "ScanAssign.scanStatus.aliquot.otherSite", siteName)); //$NON-NLS-1$

        appendLogNLS(
            "ScanAssign.activitylog.aliquot.otherSite", position, scanCell.getValue(), //$NON-NLS-1$
            siteName, currentPosition);
    }

    /**
     * aliquot found but another aliquot already at this position
     */
    private void updateCellAsPositionAlreadyTaken(String position,
        PalletCell scanCell, SpecimenWrapper expectedAliquot,
        SpecimenWrapper foundAliquot) {
        scanCell.setStatus(CellStatus.ERROR);
        scanCell.setInformation(Messages
            .getString("ScanAssign.scanStatus.aliquot.positionTakenError")); //$NON-NLS-1$
        scanCell.setTitle("!"); //$NON-NLS-1$
        appendLogNLS(
            "ScanAssign.activitylog.aliquot.positionTaken", position, expectedAliquot //$NON-NLS-1$
                .getInventoryId(), expectedAliquot.getProcessingEvent()
                .getPatient().getPnumber(), foundAliquot.getInventoryId(),
            foundAliquot.getProcessingEvent().getPatient().getPnumber());
    }

    /**
     * aliquot not found in site (not yet linked ?)
     */
    private void updateCellAsNotLinked(String position, PalletCell scanCell) {
        scanCell.setStatus(CellStatus.ERROR);
        scanCell.setInformation(Messages
            .getString("ScanAssign.scanStatus.aliquot.notlinked")); //$NON-NLS-1$
        appendLogNLS(
            "ScanAssign.activitylog.aliquot.notlinked", position, scanCell.getValue()); //$NON-NLS-1$
    }

    /**
     * aliquot missing
     */
    private void updateCellAsMissing(String position, PalletCell scanCell,
        SpecimenWrapper missingAliquot) {
        RowColPos rcp = new RowColPos(scanCell.getRow(), scanCell.getCol());
        PalletCell movedAliquot = movedAndMissingAliquotsFromPallet.get(rcp);
        if (movedAliquot == null) {
            scanCell.setStatus(CellStatus.MISSING);
            scanCell
                .setInformation(Messages
                    .getFormattedString(
                        "ScanAssign.scanStatus.aliquot.missing", missingAliquot.getInventoryId())); //$NON-NLS-1$
            scanCell.setTitle("?"); //$NON-NLS-1$
            appendLogNLS(
                "ScanAssign.activitylog.aliquot.missing", position, missingAliquot //$NON-NLS-1$
                    .getInventoryId(), missingAliquot.getProcessingEvent()
                    .getFormattedDateProcessed(), missingAliquot
                    .getProcessingEvent().getPatient().getPnumber());
            movedAndMissingAliquotsFromPallet.put(rcp, scanCell);
        } else {
            movedAndMissingAliquotsFromPallet.remove(rcp);
            scanCell.setStatus(CellStatus.EMPTY);
        }
    }

    @Override
    protected void doBeforeSave() throws Exception {
        saveEvenIfMissing = saveEvenIfAliquotsMissing();
    }

    @Override
    protected void saveForm() throws Exception {
        if (saveEvenIfMissing) {
            if (containerToRemove != null) {
                containerToRemove.delete();
            }
            currentPalletWrapper.persist();
            displayPalletPositionInfo();
            int totalNb = 0;
            StringBuffer sb = new StringBuffer("ALIQUOTS ASSIGNED:\n"); //$NON-NLS-1$
            try {
                Map<RowColPos, PalletCell> cells = getCells();
                for (RowColPos rcp : cells.keySet()) {
                    PalletCell cell = cells.get(rcp);
                    if (cell != null
                        && (cell.getStatus() == CellStatus.NEW || cell
                            .getStatus() == CellStatus.MOVED)) {
                        SpecimenWrapper aliquot = cell.getSpecimen();
                        if (aliquot != null) {
                            aliquot.setPosition(rcp);
                            aliquot.setParent(currentPalletWrapper);
                            aliquot.persist();
                            String posStr = aliquot.getPositionString(true,
                                false);
                            if (posStr == null) {
                                posStr = "none"; //$NON-NLS-1$
                            }
                            computeActivityLogMessage(sb, cell, aliquot, posStr);
                            totalNb++;
                        }
                    }
                }
            } catch (Exception ex) {
                setScanHasBeenLauched(false);
                throw ex;
            }
            appendLog(sb.toString());
            appendLogNLS("ScanAssign.activitylog.save.summary", totalNb, //$NON-NLS-1$
                currentPalletWrapper.getLabel(),
                currentSiteSelected.getNameShort());
            setFinished(false);
        }
    }

    private void computeActivityLogMessage(StringBuffer sb, PalletCell cell,
        SpecimenWrapper aliquot, String posStr) {
        ProcessingEventWrapper visit = aliquot.getProcessingEvent();
        sb.append(Messages.getFormattedString(
            "ScanAssign.activitylog.aliquot.assigned", //$NON-NLS-1$
            posStr, currentSiteSelected.getNameShort(), cell.getValue(),
            aliquot.getSpecimenType().getName(), visit.getPatient().getPnumber(),
            visit.getFormattedDateDrawn(), visit.getCollectionEvent().getClinic()
                .getName()));
    }

    private boolean saveEvenIfAliquotsMissing() {
        if (currentScanState == CellStatus.MISSING
            && movedAndMissingAliquotsFromPallet.size() > 0) {
            boolean save = BioBankPlugin.openConfirm(
                Messages.getString("ScanAssign.dialog.reallySave.title"), //$NON-NLS-1$
                Messages.getString("ScanAssign.dialog.saveWithMissing.msg")); //$NON-NLS-1$
            if (save) {
                return true;
            } else {
                setDirty(true);
                return false;
            }
        }
        return true;
    }

    private void displayPalletPositionInfo() {
        String productBarcode = currentPalletWrapper.getProductBarcode();
        String containerType = currentPalletWrapper.getContainerType()
            .getName();
        String palletLabel = currentPalletWrapper.getLabel();
        String siteName = currentSiteSelected.getNameShort();
        if (palletFoundWithProductBarcodeLabel == null)
            appendLogNLS("ScanAssign.activitylog.pallet.added", //$NON-NLS-1$
                productBarcode, containerType, palletLabel, siteName);
        else if (!palletLabel.equals(palletFoundWithProductBarcodeLabel))
            appendLogNLS(
                "ScanAssign.activitylog.pallet.moved", //$NON-NLS-1$
                productBarcode, containerType,
                palletFoundWithProductBarcodeLabel, palletLabel, siteName);
    }

    @Override
    public void reset() throws Exception {
        super.reset();
        reset(false);
        fieldsComposite.setEnabled(true);
        showOnlyPallet(true);
        form.layout(true, true);
        palletproductBarcodeText.setFocus();
        setCanLaunchScan(false);
    }

    public void reset(boolean beforeScan) {
        String productBarcode = ""; //$NON-NLS-1$
        String label = ""; //$NON-NLS-1$
        ContainerTypeWrapper type = null;

        if (beforeScan) { // keep fields values
            productBarcode = palletproductBarcodeText.getText();
            label = palletPositionText.getText();
            type = currentPalletWrapper.getContainerType();
        } else {
            if (palletTypesViewer != null) {
                palletTypesViewer.getCombo().deselectAll();
            }
            setScanHasBeenLauched(false);
            removeRescanMode();
            freezerWidget.setSelection(null);
            hotelWidget.setSelection(null);
            palletWidget.setCells(null);
        }
        movedAndMissingAliquotsFromPallet.clear();
        setScanHasBeenLauched(false);
        initPalletValues();

        currentPalletWrapper.setProductBarcode(productBarcode);
        productBarcodeValidator.validate(productBarcode);
        currentPalletWrapper.setLabel(label);
        palletLabelValidator.validate(label);
        currentPalletWrapper.setContainerType(type);
        currentPalletWrapper.setSite(siteCombo.getSelectedSite());
        if (!beforeScan) {
            setDirty(false);
            setFocus();
            useNewProductBarcode = false;
        }
    }

    private void initPalletValues() {
        try {
            currentPalletWrapper
                .initObjectWith(new ContainerWrapper(appService));
            currentPalletWrapper.reset();
            currentPalletWrapper.setActivityStatus(ActivityStatusWrapper
                .getActiveActivityStatus(appService));
        } catch (Exception e) {
            logger.error("Error while reseting pallet values", e); //$NON-NLS-1$
        }
    }

    @Override
    protected String getOkMessage() {
        return Messages.getString("ScanAssign.okMessage"); //$NON-NLS-1$
    }

    /**
     * From the pallet product barcode, get existing information from database
     * and set the position. Set only the position if the product barcode
     * doesn't yet exist
     */
    private boolean checkPallet() throws Exception {
        boolean canContinue = true;
        boolean needToCheckPosition = true;
        ContainerTypeWrapper type = currentPalletWrapper.getContainerType();
        if (palletFoundWithProductBarcodeLabel != null) {
            // a pallet with this product barcode already exists in the
            // database.
            appendLogNLS(
                "ScanAssign.activitylog.pallet.checkLabelForProductBarcode", //$NON-NLS-1$
                currentPalletWrapper.getLabel(),
                currentPalletWrapper.getProductBarcode(), currentPalletWrapper
                    .getSite().getNameShort());
            // need to compare with this value, in case the container has
            // been copied to the current pallet
            if (palletFoundWithProductBarcodeLabel.equals(currentPalletWrapper
                .getLabel())) {
                // The position already contains this pallet. Don't need to
                // check it. Need to use exact same retrieved wrappedObject.
                // currentPalletWrapper
                // .initObjectWith(palletFoundWithProductBarcode);
                // currentPalletWrapper.reset();
                needToCheckPosition = false;
            } else {
                canContinue = openDialogPalletMoved();
                if (canContinue) {
                    // Move the pallet.
                    type = currentPalletWrapper.getContainerType();
                    appendLogNLS(
                        "ScanAssign.activitylog.pallet.moveInfo", //$NON-NLS-1$
                        currentPalletWrapper.getProductBarcode(),
                        palletFoundWithProductBarcodeLabel,
                        currentPalletWrapper.getLabel());
                } else {
                    return false;
                }
            }
            if (type != null) {
                appendLogNLS("ScanAssign.activitylog.pallet.typeUsed", //$NON-NLS-1$
                    type.getName());
            }
        }
        if (needToCheckPosition) {
            canContinue = checkAndSetPosition(type);
        }
        return canContinue;
    }

    private boolean openDialogPalletMoved() {
        return MessageDialog.openConfirm(PlatformUI.getWorkbench()
            .getActiveWorkbenchWindow().getShell(), "Pallet product barcode", //$NON-NLS-1$
            Messages.getFormattedString(
                "ScanAssign.dialog.checkPallet.otherPosition", //$NON-NLS-1$
                palletFoundWithProductBarcodeLabel,
                currentPalletWrapper.getLabel()));
    }

    /**
     * Check if position is available and set the ContainerPosition if it is
     * free
     * 
     * @return true if was able to create the ContainerPosition
     */
    private boolean checkAndSetPosition(ContainerTypeWrapper typeFixed)
        throws Exception {
        containerToRemove = null;
        List<ContainerTypeWrapper> palletTypes = palletContainerTypes;
        if (typeFixed != null) {
            palletTypes = Arrays.asList(typeFixed);
        }
        // search for containers at this position, with type in one of the type
        // listed
        List<ContainerWrapper> containersAtPosition;
        if (siteCombo.getSelectedSite() == null)
            containersAtPosition = new ArrayList<ContainerWrapper>();
        else
            containersAtPosition = currentPalletWrapper
                .getContainersWithSameLabelWithType(palletContainerTypes);
        String palletLabel = currentPalletWrapper.getLabel();
        if (containersAtPosition.size() == 0) {
            currentPalletWrapper.setPositionAndParentFromLabel(palletLabel,
                palletTypes);
            palletTypes = palletContainerTypes;
            typeFixed = null;
        } else if (containersAtPosition.size() == 1) {
            // One container found
            ContainerWrapper containerAtPosition = containersAtPosition.get(0);
            String barcode = containerAtPosition.getProductBarcode();
            if ((barcode != null && !barcode.isEmpty())
                || containerAtPosition.hasAliquots()) {
                // Position already physically used
                boolean ok = openDialogPositionUsed(barcode);
                if (!ok) {
                    appendLogNLS(
                        "ScanAssign.activitylog.pallet.positionUsedMsg", barcode, //$NON-NLS-1$
                        currentPalletWrapper.getLabel(), siteCombo
                            .getSelectedSite().getNameShort()); //$NON-NLS-1$
                    return false;
                }
            }
            if (useNewProductBarcode) {
                // Position exists but no product barcode set before
                appendLogNLS(
                    "ScanAssign.activitylog.pallet.positionUsedWithNoProductBarcode",
                    palletLabel, containerAtPosition.getContainerType()
                        .getName(), currentPalletWrapper.getProductBarcode());
            } else {
                // Position initialised but not physically used
                appendLogNLS(
                    "ScanAssign.activitylog.pallet.positionInitialized",
                    palletLabel, containerAtPosition.getContainerType()
                        .getName());
            }

            palletTypes = Arrays.asList(containerAtPosition.getContainerType());
            typeFixed = containerAtPosition.getContainerType();
            if (palletFoundWithProductBarcodeLabel != null) {
                containerToRemove = containerAtPosition;
                // pallet already exists. Need to remove the initialisation to
                // replace it.
                currentPalletWrapper.setParent(containerAtPosition.getParent());
                currentPalletWrapper.setPosition(containerAtPosition
                    .getPosition());
            } else {
                // new pallet or only new product barcode. Can use the
                // initialised one
                String productBarcode = currentPalletWrapper
                    .getProductBarcode();
                currentPalletWrapper.initObjectWith(containerAtPosition);
                currentPalletWrapper.reset();
                currentPalletWrapper.setProductBarcode(productBarcode);
            }
        } else {
            BioBankPlugin.openError("Check position",
                "Found more than one pallet with position " + palletLabel);
            nextFocusWidget = palletPositionText;
            return false;
        }
        ContainerTypeWrapper oldSelection = currentPalletWrapper
            .getContainerType();
        palletTypesViewer.setInput(palletTypes);
        if (oldSelection != null) {
            palletTypesViewer
                .setSelection(new StructuredSelection(oldSelection));
        }
        if (typeFixed != null) {
            palletTypesViewer.setSelection(new StructuredSelection(typeFixed));
        }
        if (palletTypes.size() == 1) {
            palletTypesViewer.setSelection(new StructuredSelection(palletTypes
                .get(0)));
        }
        palletTypesViewer.getCombo().setEnabled(typeFixed == null);
        return true;
    }

    private boolean openDialogPositionUsed(String barcode) {
        if (barcode == null || barcode.isEmpty()) {
            // Position already use but the barcode was not set.
            if (!useNewProductBarcode) {
                useNewProductBarcode = MessageDialog
                    .openQuestion(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                            .getShell(),
                        Messages
                            .getString("ScanAssign.dialog.positionUsed.noBarcode.title"),
                        Messages
                            .getString("ScanAssign.dialog.positionUsed.noBarcode.question"));
            }
            return useNewProductBarcode;
        } else {
            // Position already use with a different barcode
            BioBankPlugin
                .openError(
                    Messages
                        .getString("ScanAssign.dialog.positionUsed.error.title"), //$NON-NLS-1$
                    Messages
                        .getFormattedString(
                            "ScanAssign.dialog.positionUsed.error.msg", barcode, siteCombo.getSelectedSite().getNameShort())); //$NON-NLS-1$
            nextFocusWidget = palletPositionText;
            return false;
        }
    }

    @Override
    public String getNextOpenedFormID() {
        return ID;
    }

    @Override
    protected String getActivityTitle() {
        return "Scan assign activity"; //$NON-NLS-1$
    }

    @Override
    protected void disableFields() {
        fieldsComposite.setEnabled(false);
    }

    @Override
    public BiobankLogger getErrorLogger() {
        return logger;
    }

    @Override
    protected void siteComboSelectionChanged(SiteWrapper currentSelection) {
        currentPalletWrapper.setSite(currentSelection);
        currentPalletWrapper.setContainerType(null);
        try {
            palletContainerTypes = getPalletContainerTypes();
        } catch (ApplicationException e) {
            BioBankPlugin.openAsyncError("Error retrieving container types", e);
        }
        // if (palletTypesViewer != null) {
        // palletTypesViewer.setInput(palletContainerTypes);
        // palletTypesViewer.getCombo().deselectAll();
        // if (palletContainerTypes.size() == 1) {
        // palletTypesViewer.setSelection(new StructuredSelection(
        // palletContainerTypes.get(0)));
        // }
        // }
        validateValues();
    }
}
