package com.khudim.protobufdbviewer;

import javax.swing.JTable;

record SelectedCell(byte[] bytes, JTable grid, int viewColumn) {}
