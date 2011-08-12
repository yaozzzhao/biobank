package edu.ualberta.med.biobank.dialogs.dispatch;

import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.observable.value.WritableValue;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import edu.ualberta.med.biobank.gui.common.dialogs.BgcBaseDialog;
import edu.ualberta.med.biobank.gui.common.validators.NonEmptyStringValidator;
import edu.ualberta.med.biobank.gui.common.widgets.BgcBaseText;

public class PalletBarcodeDialog extends BgcBaseDialog {

    public PalletBarcodeDialog(Shell parentShell) {
        super(parentShell);
    }

    IObservableValue barcode = new WritableValue("", String.class); //$NON-NLS-1$

    @Override
    protected String getTitleAreaMessage() {
        return Messages.PalletBarcodeDialog_0;
    }

    @Override
    protected String getTitleAreaTitle() {
        return Messages.PalletBarcodeDialog_1;
    }

    @Override
    protected String getDialogShellTitle() {
        return Messages.PalletBarcodeDialog_2;
    }

    @Override
    protected void createDialogAreaInternal(Composite parent) throws Exception {
        widgetCreator.createBoundWidgetWithLabel(parent, BgcBaseText.class,
            SWT.NONE, Messages.PalletBarcodeDialog_3, new String[] {}, barcode,
            new NonEmptyStringValidator(Messages.PalletBarcodeDialog_4));
    }

    public String getBarcode() {
        return (String) this.barcode.getValue();
    }

};
