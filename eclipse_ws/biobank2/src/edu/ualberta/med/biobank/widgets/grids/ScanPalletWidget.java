package edu.ualberta.med.biobank.widgets.grids;

import java.util.List;

import org.eclipse.swt.widgets.Composite;

import edu.ualberta.med.biobank.widgets.grids.cell.AbstractUICell;
import edu.ualberta.med.biobank.widgets.grids.cell.PalletCell;
import edu.ualberta.med.biobank.widgets.grids.cell.UICellStatus;
import edu.ualberta.med.scannerconfig.preferences.scanner.profiles.ProfileManager;
import edu.ualberta.med.scannerconfig.preferences.scanner.profiles.ProfileSettings;

public class ScanPalletWidget extends ContainerDisplayWidget {

    private ScanPalletDisplay defaultDisplay;

    public ScanPalletWidget(Composite parent) {
        this(parent, null);
    }

    public ScanPalletWidget(Composite parent, List<UICellStatus> cellStatus) {
        super(parent, cellStatus);
        defaultDisplay = new ScanPalletDisplay(this);
        setContainerDisplay(defaultDisplay);
    }

    public boolean isEverythingTyped() {
        if (cells != null) {
            for (AbstractUICell cell : cells.values()) {
                PalletCell pCell = (PalletCell) cell;
                if (PalletCell.hasValue(pCell) && pCell.getType() == null) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    public void loadProfile(String profileName) {
        ProfileSettings profile = ProfileManager.instance().getProfile(
            profileName);
        ((ScanPalletDisplay) getContainerDisplay()).setProfile(profile);
        this.redraw();
    }

    public void setDefaultDisplay() {
        setContainerDisplay(defaultDisplay);
    }

}
