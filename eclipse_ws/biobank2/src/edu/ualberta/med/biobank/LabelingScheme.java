package edu.ualberta.med.biobank;

import java.text.DecimalFormat;

import edu.ualberta.med.biobank.model.ContainerType;

public class LabelingScheme {

    private static final String posAlpha = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    public static int pos2Int(String alpha) throws Exception {
        if (alpha.length() != 1) {
            throw new Exception("binPos has an invalid length: " + alpha);
        }
        return posAlpha.indexOf(alpha.charAt(0));
    }

    public static char int2pos(int pos) {
        return posAlpha.charAt(pos);
    }

    public static int sbsToInt(String pos) throws Exception {
        if (pos.length() != 2) {
            throw new Exception("binPos has an invalid length: " + pos);
        }
        return posAlpha.indexOf(pos.charAt(0)) * 12
            + Integer.parseInt(pos.substring(1)) - 1;
    }

    public static int twoCharAlphaToInt(String label) {
        int len = label.length();
        return posAlpha.indexOf(label.charAt(len - 2)) * 24
            + posAlpha.indexOf(label.charAt(len - 1));
    }

    public static RowColPos twoCharAlphaToRowCol(ContainerType container,
        String label) throws Exception {
        Integer rowCap = container.getCapacity().getDimensionOneCapacity();
        Integer colCap = container.getCapacity().getDimensionTwoCapacity();

        int pos = twoCharAlphaToInt(label);
        if (pos >= rowCap * colCap) {
            throw new Exception("position out of bounds: containerType/"
                + container.getName() + " pos/" + pos + " rowCap/" + rowCap
                + " colCap/" + colCap);
        }
        RowColPos rowColPos = new RowColPos();
        rowColPos.row = pos % rowCap;
        rowColPos.col = pos / rowCap;
        return rowColPos;

    }

    public static RowColPos twoCharNumericToRowCol(ContainerType container,
        String label) throws Exception {
        int pos = Integer.parseInt(label.substring(label.length() - 1));
        Integer rowCap = container.getCapacity().getDimensionOneCapacity();
        RowColPos rowColPos = new RowColPos();
        rowColPos.row = pos % rowCap;
        rowColPos.col = pos / rowCap;
        return rowColPos;
    }

    /**
     * convert a position in row*column to two letter (in the cbsr way)
     */
    public static String rowColToTwoCharAlpha(RowColPos rcp,
        ContainerType containerType) {
        int pos1, pos2, index;
        int totalRows = containerType.getCapacity().getDimensionOneCapacity();
        int totalCols = containerType.getCapacity().getDimensionTwoCapacity();

        if (totalRows == 1) {
            index = rcp.col;
        } else if (totalCols == 1) {
            index = rcp.row;
        } else {
            index = totalRows * rcp.col + rcp.row;
        }

        pos1 = index / 24;
        pos2 = index % 24;

        return String.valueOf(int2pos(pos1)) + String.valueOf(int2pos(pos2));
    }

    public static String rowColToTwoCharNumeric(RowColPos rcp) {
        DecimalFormat df1 = new DecimalFormat("00");
        return df1.format(rcp.row + 1);
    }

    public static String rowColToInt(RowColPos rcp, ContainerType containerType) {
        int totalRows = containerType.getCapacity().getDimensionOneCapacity();
        // int totalColumns = containerType.getCapacity()
        // .getDimensionTwoCapacity();

        char letter1 = 'A';
        char letter2 = 'A';

        int total1 = totalRows * rcp.col + rcp.row;
        letter1 = (char) (letter1 + (total1 / 24));
        letter1 = correctPositionLetter(letter1);

        // int total2 = (row + 1) * totalRows * column + row; // + 1 because
        // start at zero
        letter2 = (char) (letter2 + (total1 % 24));
        letter2 = correctPositionLetter(letter2);

        return String.valueOf(letter1) + String.valueOf(letter2);
    }

    public static char correctPositionLetter(char letter) {
        if (letter == ':')
            return (char) (letter - 10);
        if (letter < 'I') {
            return letter;
        }
        if (letter >= 'I' && letter < 'O') {
            return (char) (letter + 1);
        }
        return (char) (letter + 2);
    }
}
