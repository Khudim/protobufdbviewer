package com.khudim.protobufdbviewer;

import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.sql.Blob;

final class SelectedCellReader {
    private SelectedCellReader() {}

    static SelectedCell read(Component contextComponent) throws Exception {
        JTable grid = findTable(contextComponent);
        if (grid == null) {
            grid = findTable(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
        }
        if (grid == null) {
            throw new IllegalStateException("Cannot find a database result grid. Open the context menu on a result cell.");
        }

        return read(grid, grid.getSelectedColumn());
    }

    static SelectedCell read(JTable grid, int preferredViewColumn) throws Exception {
        int viewRow = grid.getSelectedRow();
        int viewColumn = preferredViewColumn >= 0 ? preferredViewColumn : grid.getSelectedColumn();
        if (viewRow < 0 || viewColumn < 0) {
            throw new IllegalStateException("Select one binary cell first.");
        }

        Object value = grid.getModel().getValueAt(
                grid.convertRowIndexToModel(viewRow),
                grid.convertColumnIndexToModel(viewColumn)
        );

        byte[] bytes = toBytes(value, 0);
        if (bytes == null) {
            throw new IllegalStateException("The selected cell is not exposed as binary data. Value type: " +
                    (value == null ? "null" : value.getClass().getName()));
        }
        return new SelectedCell(bytes, grid, viewColumn);
    }

    static JTable findTable(Component component) {
        if (component == null) return null;
        for (Component current = component; current != null; current = current.getParent()) {
            if (current instanceof JTable table) return table;
        }
        if (component instanceof Container container) return findTableInside(container, 0);
        return null;
    }

    private static JTable findTableInside(Container container, int depth) {
        if (depth > 6) return null;
        for (Component child : container.getComponents()) {
            if (child instanceof JTable table) return table;
            if (child instanceof Container nested) {
                JTable result = findTableInside(nested, depth + 1);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static byte[] toBytes(Object value, int depth) throws Exception {
        if (value == null || depth > 8) return null;
        if (value instanceof byte[] bytes) return bytes;
        if (value instanceof Blob blob) {
            try (InputStream input = blob.getBinaryStream()) {
                return input.readAllBytes();
            }
        }
        if (value instanceof ByteBuffer buffer) {
            ByteBuffer copy = buffer.slice();
            byte[] bytes = new byte[copy.remaining()];
            copy.get(bytes);
            return bytes;
        }
        if (value instanceof InputStream input) {
            return input.readAllBytes();
        }

        for (String methodName : new String[]{
                "getBytes", "asBytes", "getData", "getContent", "getValue", "getRawValue",
                "value", "getObject", "getBinaryStream", "getInputStream"
        }) {
            try {
                Method method = findZeroArgMethod(value.getClass(), methodName);
                if (method == null) continue;
                method.setAccessible(true);
                Object nested = method.invoke(value);
                if (nested != null && nested != value) {
                    byte[] result = toBytes(nested, depth + 1);
                    if (result != null) return result;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }

        for (String fieldName : new String[]{"value", "bytes", "data", "content", "rawValue"}) {
            try {
                Field field = findField(value.getClass(), fieldName);
                if (field == null) continue;
                field.setAccessible(true);
                Object nested = field.get(value);
                if (nested != null && nested != value) {
                    byte[] result = toBytes(nested, depth + 1);
                    if (result != null) return result;
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
        return null;
    }

    private static Method findZeroArgMethod(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == 0) {
                    return method;
                }
            }
        }
        return null;
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }
}
