package edu.ualberta.med.biobank.forms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Section;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteConnectFailureException;

import edu.ualberta.med.biobank.SessionManager;
import edu.ualberta.med.biobank.forms.input.FormInput;
import edu.ualberta.med.biobank.model.Clinic;
import edu.ualberta.med.biobank.model.Sdata;
import edu.ualberta.med.biobank.model.SdataType;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.model.Study;
import edu.ualberta.med.biobank.model.Worksheet;
import edu.ualberta.med.biobank.treeview.Node;
import edu.ualberta.med.biobank.treeview.SiteAdapter;
import edu.ualberta.med.biobank.treeview.StudyAdapter;
import edu.ualberta.med.biobank.validators.NonEmptyString;
import edu.ualberta.med.biobank.widgets.MultiSelect;
import edu.ualberta.med.biobank.widgets.SdataWidget;
import gov.nih.nci.system.query.SDKQuery;
import gov.nih.nci.system.query.SDKQueryResult;
import gov.nih.nci.system.query.example.InsertExampleQuery;
import gov.nih.nci.system.query.example.UpdateExampleQuery;

@SuppressWarnings("serial")
public class StudyEntryForm extends BiobankEditForm {
	public static final String ID =
	      "edu.ualberta.med.biobank.forms.StudyEntryForm";
	
	private static final String NEW_STUDY_OK_MESSAGE 
		= "Creating a new study.";
	
	private static final String STUDY_OK_MESSAGE = "Editing an existing study.";

	public static final String[] ORDERED_FIELDS = new String[] {
		"name",
		"nameShort",
		"activityStatus",
		"comment"
	};
	
	public static final HashMap<String, FieldInfo> FIELDS = 
	    new HashMap<String, FieldInfo>() {{
	        put("name", new FieldInfo(
	            "Name", Text.class, SWT.NONE, null, 
	            NonEmptyString.class, "Study name cannot be blank"));
	        put("nameShort", new FieldInfo(
	            "Short Name", Text.class, SWT.NONE, null,
	            NonEmptyString.class, "Study short name cannot be blank"));
	        put("activityStatus",  new FieldInfo(
	            "Activity Status", Combo.class, SWT.NONE, 
	            FormConstants.ACTIVITY_STATUS, null, null));
	        put("comment", new FieldInfo(
	            "Comments", Text.class, SWT.NONE, null, null, null));
	    }
	};
	
	private MultiSelect clinicsMultiSelect;
	
	private StudyAdapter studyAdapter;
	
	private Study study;
	
	private Site site;
	
	private Collection<Clinic> allClinics;
	
	private Collection<SdataType> allSdataTypes;
	
	private Button submit;
        
    private HashMap<String, SdataWidget> sdataWidgets;
	
	public StudyEntryForm() {
		super();
		sdataWidgets = new HashMap<String, SdataWidget>();
	}

	@Override
	public void init(IEditorSite editorSite, IEditorInput input)
			throws PartInitException {
		if ( !(input instanceof FormInput)) 
			throw new PartInitException("Invalid editor input");
		
		super.init(editorSite, input);
		
		Node node = ((FormInput) input).getNode();
		Assert.isNotNull(node, "Null editor input");
		
		Assert.isTrue((node instanceof StudyAdapter), 
				"Invalid editor input: object of type "
				+ node.getClass().getName());
		
		studyAdapter = (StudyAdapter) node;
		study = studyAdapter.getStudy();
		study.setWorksheet(new Worksheet());
        site = ((SiteAdapter) studyAdapter.getParent().getParent()).getSite();        
		
		if (study.getId() == null) {
			setPartName("New Study");
		}
		else {
			setPartName("Study " + study.getName());
		}
	}

    @Override
    protected void createFormContent() {
		form.setText("Study Information");
		form.setMessage(getOkMessage(), IMessageProvider.NONE);
		
		GridLayout layout = new GridLayout(1, false);
		form.getBody().setLayout(layout);
		
		Composite client = toolkit.createComposite(form.getBody());
		layout = new GridLayout(2, false);
		layout.horizontalSpacing = 10;
		client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		toolkit.paintBordersFor(client);
		
        createWidgetsFromHashMap(FIELDS, ORDERED_FIELDS, study, client);
        Text comments = (Text) controls.get("comment");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.heightHint = 40;
        comments.setLayoutData(gd);
		
		createClinicSection();
		createSdataSection();

        client = toolkit.createComposite(form.getBody());
        layout = new GridLayout(2, false);
        layout.horizontalSpacing = 10;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);

        submit = toolkit.createButton(client, "Submit", SWT.PUSH);
        submit.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                doSaveInternal();
            }
        });
	}
    
    private void createClinicSection() {
        Section section = toolkit.createSection(form.getBody(), 
                Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Clinic Selection");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        clinicsMultiSelect = new MultiSelect(section, SWT.NONE, 
                "Selected Clinics", "Available Clinics", 100);
        section.setClient(clinicsMultiSelect);
        clinicsMultiSelect.adaptToToolkit(toolkit);
        
        Collection<Clinic> studyClinics = study.getClinicCollection(); 
        allClinics = site.getClinicCollection();

        HashMap<Integer, String> availClinics = new HashMap<Integer, String>();
        List<Integer> selClinics = new ArrayList<Integer>();

        if (studyClinics != null) {
            for (Clinic clinic : studyClinics) {
                selClinics.add(clinic.getId());
            }
        }
        
        for (Clinic clinic : allClinics) {
            availClinics.put(clinic.getId(), clinic.getName());
        }
        clinicsMultiSelect.addSelections(availClinics, selClinics);
    }
    
    private void createSdataSection() {
        Collection<Sdata> studySdata = study.getSdataCollection();
        HashMap<Integer, Sdata> selected = new HashMap<Integer, Sdata>();
        
        if (studySdata != null) {
            for (Sdata sdata : studySdata) {
                selected.put(sdata.getSdataType().getId(), sdata);
            }
        }
            
        allSdataTypes = getAllSdataTypes();

        Section section = toolkit.createSection(form.getBody(), 
                Section.TWISTIE | Section.TITLE_BAR | Section.EXPANDED);
        section.setText("Study Information Selection");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        Composite client = toolkit.createComposite(section);
        section.setClient(client);        
        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        layout.marginHeight = 0;
        client.setLayout(layout);
        client.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        toolkit.paintBordersFor(client);
        
        for (SdataType sdataType : allSdataTypes) {
            String value = "";
            Sdata sdata = selected.get(sdataType.getId());
            boolean itemSelected = false;
            if (sdata != null) {
                itemSelected = true;
                value = sdata.getValue(); 
            }
            
            SdataWidget w = 
                new SdataWidget(client, SWT.NONE, sdataType, itemSelected, value); 
            w.adaptToToolkit(toolkit);
            sdataWidgets.put(sdataType.getType(), w);
        }
        
    }
	
	private String getOkMessage() {
		if (study.getId() == null) {
			return NEW_STUDY_OK_MESSAGE;
		}
		return STUDY_OK_MESSAGE;
	}
    
    protected void handleStatusChanged(IStatus status) {
		if (status.getSeverity() == IStatus.OK) {
			form.setMessage(getOkMessage(), IMessageProvider.NONE);
	    	submit.setEnabled(true);
		}
		else {
			form.setMessage(status.getMessage(), IMessageProvider.ERROR);
	    	submit.setEnabled(false);
		}
    }
    
    protected void saveForm() {
    	// get the selected clinics from widget
    	List<Integer> selClinicIds = clinicsMultiSelect.getSelected();
    	Set<Clinic> selClinics = new HashSet<Clinic>();
    	for (Clinic clinic : allClinics) {
    	    int id = clinic.getId();
    		if (selClinicIds.indexOf(id) >= 0) {
    			selClinics.add(clinic);
    		}
    		
    	}
    	Assert.isTrue(selClinics.size() == selClinicIds.size(), 
    			"problem with clinic selections");
		study.setClinicCollection(selClinics);
        
		List<Sdata> sdataList = new ArrayList<Sdata>();
        for (SdataType sdataType : allSdataTypes) {
            String type = sdataType.getType();
    	    String value =  sdataWidgets.get(type).getResult();
    	    if ((value.length() == 0) || value.equals("no")) continue;
    	    Sdata sdata = new Sdata();
    	    sdata.setSdataType(sdataType);
            if (value.equals("yes")) {
                value = "";
            }
            sdata.setValue(value);
    	    sdataList.add(sdata);
    	}
        study.setSdataCollection(sdataList);
        
        saveStudy(study);        
        SessionManager.getInstance().updateStudies(studyAdapter);    	
		getSite().getPage().closeEditor(StudyEntryForm.this, false);    	
    }
    
    private void saveStudy(Study study) {
        try {
            SDKQuery query;
            SDKQueryResult result;
            Set<Sdata> savedSdataList = new HashSet<Sdata>();
            
            study.setSite(site);
            study.setWorksheet(null);

            if (study.getSdataCollection().size() > 0) {
                for (Sdata sdata : study.getSdataCollection()) {
                    if ((sdata.getId() == null) || (sdata.getId() == 0)) {
                        query = new InsertExampleQuery(sdata);
                    }
                    else {
                        query = new UpdateExampleQuery(sdata);
                    }                  

                    result = studyAdapter.getAppService().executeQuery(query);
                    savedSdataList.add((Sdata) result.getObjectResult());
                }
            }
            study.setSdataCollection(savedSdataList);

            if ((study.getId() == null) || (study.getId() == 0)) {
                query = new InsertExampleQuery(study);
            }
            else { 
                query = new UpdateExampleQuery(study);
            }
            
            result = studyAdapter.getAppService().executeQuery(query);
            study = (Study) result.getObjectResult();
        }
        catch (final RemoteAccessException exp) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    MessageDialog.openError(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
                            "Connection Attempt Failed", 
                            "Could not perform database operation. Make sure server is running correct version.");
                }
            });
        }
        catch (Exception exp) {
            exp.printStackTrace();
        }
    }
    
    private List<SdataType> getAllSdataTypes() {        
        SdataType criteria = new SdataType();

        try {
            return studyAdapter.getAppService().search(SdataType.class, criteria);
        }
        catch (final RemoteConnectFailureException exp) {
            Display.getDefault().asyncExec(new Runnable() {
                public void run() {
                    MessageDialog.openError(
                            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
                            "Connection Attempt Failed", 
                    "Could not connect to server. Make sure server is running.");
                }
            });
        }
        catch (Exception exp) {
            exp.printStackTrace();
        }
        return null;
    }
}
