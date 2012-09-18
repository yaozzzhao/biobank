package edu.ualberta.med.biobank.common.action.batchoperation.shipment;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.supercsv.cellprocessor.ParseDate;
import org.supercsv.cellprocessor.constraint.StrNotNullOrEmpty;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.exception.SuperCSVException;
import org.supercsv.io.CsvBeanReader;
import org.supercsv.io.ICsvBeanReader;
import org.supercsv.prefs.CsvPreference;
import org.xnap.commons.i18n.I18n;
import org.xnap.commons.i18n.I18nFactory;

import edu.ualberta.med.biobank.CommonBundle;
import edu.ualberta.med.biobank.common.action.Action;
import edu.ualberta.med.biobank.common.action.ActionContext;
import edu.ualberta.med.biobank.common.action.BooleanResult;
import edu.ualberta.med.biobank.common.action.batchoperation.BatchOpActionUtil;
import edu.ualberta.med.biobank.common.action.batchoperation.BatchOpInputErrorList;
import edu.ualberta.med.biobank.common.action.batchoperation.specimen.SpecimenBatchOpAction;
import edu.ualberta.med.biobank.common.action.exception.ActionException;
import edu.ualberta.med.biobank.common.action.exception.CsvImportException;
import edu.ualberta.med.biobank.i18n.Bundle;
import edu.ualberta.med.biobank.i18n.LocalizedException;
import edu.ualberta.med.biobank.i18n.Tr;
import edu.ualberta.med.biobank.model.Center;
import edu.ualberta.med.biobank.model.OriginInfo;
import edu.ualberta.med.biobank.model.PermissionEnum;
import edu.ualberta.med.biobank.model.ShippingMethod;
import edu.ualberta.med.biobank.model.Site;
import edu.ualberta.med.biobank.util.CompressedReference;

/**
 * 
 * @author loyola
 * 
 */
@SuppressWarnings("nls")
public class ShipmentBatchOpAction implements Action<BooleanResult> {
    private static final long serialVersionUID = 1L;

    private static final Bundle bundle = new CommonBundle();

    private static final I18n i18n = I18nFactory
        .getI18n(SpecimenBatchOpAction.class);

    public static final Tr CSV_SENDING_CENTER_ERROR =
        bundle.tr("sending center with name \"{0}\" does not exist");

    public static final Tr CSV_RECEIVING_CENTER_ERROR =
        bundle.tr("receiving center with name \"{0}\" does not exist");

    public static final Tr CSV_PNUMBER_ERROR =
        bundle.tr("patient number \"{0}\" does not exist");

    public static final Tr CSV_INVENTORY_ID_ERROR =
        bundle.tr("inventory id \"{0}\" does not exist");

    public static final Tr CSV_SHIPPING_METHOD_ERROR =
        bundle.tr("shipping method \"{0}\" does not exist");

    public static final Tr CSV_DATA_DECOMPRESSION_ERROR =
        bundle.tr("Unable to decompress CSV data: {0}");

    // @formatter:off
    private static final CellProcessor[] PROCESSORS = new CellProcessor[] {
        new ParseDate("yyyy-MM-dd HH:mm"), // dateReceived
        new StrNotNullOrEmpty(),           // sendingCenter
        new StrNotNullOrEmpty(),           // receivingCenter
        new StrNotNullOrEmpty(),           // shippingMethod
        new StrNotNullOrEmpty(),           // waybill
        null                               // comment
    }; 
    // @formatter:on    

    private final BatchOpInputErrorList errorList = new BatchOpInputErrorList();

    private CompressedReference<ArrayList<ShipmentBatchOpInputRow>> compressedList =
        null;

    private final Set<ShipmentBatchOpHelper> shipmentImportInfos =
        new HashSet<ShipmentBatchOpHelper>(0);

    public ShipmentBatchOpAction(String filename) throws IOException {
        setCsvFile(filename);
    }

    private void setCsvFile(String filename) throws IOException {
        ICsvBeanReader reader = new CsvBeanReader(
            new FileReader(filename), CsvPreference.EXCEL_PREFERENCE);

        final String[] header = new String[] {
            "dateReceived",
            "sendingCenter",
            "receivingCenter",
            "shippingMethod",
            "waybill",
            "comment"
        };

        try {
            ArrayList<ShipmentBatchOpInputRow> csvInfos =
                new ArrayList<ShipmentBatchOpInputRow>(0);

            ShipmentBatchOpInputRow csvInfo;
            reader.getCSVHeader(true);
            while ((csvInfo =
                reader.read(ShipmentBatchOpInputRow.class, header, PROCESSORS)) != null) {

                csvInfo.setLineNumber(reader.getLineNumber());
                csvInfos.add(csvInfo);
            }

            if (!errorList.isEmpty()) {
                throw new CsvImportException(errorList.getErrors());
            }

            compressedList =
                new CompressedReference<ArrayList<ShipmentBatchOpInputRow>>(
                    csvInfos);

        } catch (SuperCSVException e) {
            throw new IllegalStateException(
                i18n.tr(BatchOpActionUtil.CSV_PARSE_ERROR, e.getMessage(),
                    e.getCsvContext()));
        } finally {
            reader.close();
        }
    }

    @Override
    public boolean isAllowed(ActionContext context) throws ActionException {
        return PermissionEnum.BATCH_OPERATIONS.isAllowed(context.getUser());
    }

    @Override
    public BooleanResult run(ActionContext context) throws ActionException {
        if (compressedList == null) {
            throw new LocalizedException(BatchOpActionUtil.CSV_FILE_ERROR);
        }

        boolean result = false;

        ArrayList<ShipmentBatchOpInputRow> csvInfos;
        try {
            csvInfos = compressedList.get();
        } catch (Exception e) {
            throw new LocalizedException(CSV_DATA_DECOMPRESSION_ERROR.format(e
                .getMessage()));
        }
        context.getSession().getTransaction();

        for (ShipmentBatchOpInputRow csvInfo : csvInfos) {
            ShipmentBatchOpHelper info = getDbInfo(context, csvInfo);
            shipmentImportInfos.add(info);
        }

        if (!errorList.isEmpty()) {
            throw new CsvImportException(errorList.getErrors());
        }

        for (ShipmentBatchOpHelper info : shipmentImportInfos) {
            OriginInfo originInfo = info.getNewOriginInfo();

            context.getSession().save(
                originInfo.getComments().iterator().next());
            context.getSession().save(originInfo.getShipmentInfo());
            context.getSession().save(originInfo);
        }

        result = true;
        return new BooleanResult(result);
    }

    private ShipmentBatchOpHelper getDbInfo(ActionContext context,
        ShipmentBatchOpInputRow csvInfo) {
        ShipmentBatchOpHelper info = new ShipmentBatchOpHelper(csvInfo);

        info.setUser(context.getUser());

        Center sendingCenter =
            BatchOpActionUtil.getCenter(context, csvInfo.getSendingCenter());
        if (sendingCenter == null) {
            errorList.addError(csvInfo.getLineNumber(),
                CSV_SENDING_CENTER_ERROR.format(csvInfo.getSendingCenter()));
        } else {
            info.setOriginCenter(sendingCenter);
        }

        Site receivingSite =
            BatchOpActionUtil.getSite(context, csvInfo.getReceivingCenter());
        if (receivingSite == null) {
            errorList.addError(csvInfo.getLineNumber(),
                CSV_RECEIVING_CENTER_ERROR.format(csvInfo
                    .getReceivingCenter()));
        } else {
            info.setCurrentSite(receivingSite);
        }

        ShippingMethod shippingMethod =
            BatchOpActionUtil.getShippingMethod(context,
                csvInfo.getShippingMethod());
        if (shippingMethod == null) {
            errorList.addError(csvInfo.getLineNumber(),
                CSV_SHIPPING_METHOD_ERROR.format(csvInfo.getShippingMethod()));
        } else {
            info.setShippingMethod(shippingMethod);
        }

        return info;
    }

}
