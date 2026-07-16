package com.khudim.protobufdbviewer;

import javax.swing.JTable;
import javax.swing.table.TableColumn;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class GridIdentity {
    private GridIdentity() {}

    static String signature(JTable grid) {
        StringBuilder source = new StringBuilder();
        source.append(grid.getModel().getClass().getName()).append('|');
        for (int viewIndex = 0; viewIndex < grid.getColumnCount(); viewIndex++) {
            TableColumn column = grid.getColumnModel().getColumn(viewIndex);
            source.append(column.getHeaderValue()).append('|');
        }
        return sha256(source.toString());
    }

    static String columnName(JTable grid, int viewColumn) {
        if (viewColumn < 0 || viewColumn >= grid.getColumnCount()) return "";
        Object header = grid.getColumnModel().getColumn(viewColumn).getHeaderValue();
        return header == null ? "" : header.toString();
    }

    static int findViewColumn(JTable grid, String columnName) {
        for (int viewIndex = 0; viewIndex < grid.getColumnCount(); viewIndex++) {
            if (columnName.equals(columnName(grid, viewIndex))) return viewIndex;
        }
        return -1;
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : digest) result.append(String.format("%02x", item));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
