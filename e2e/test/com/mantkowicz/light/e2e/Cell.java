package com.mantkowicz.light.e2e;

/** A map cell (column, row) as in the Tiled editor: the stable way for tests to talk about tiles. */
public record Cell(int col, int row) implements Comparable<Cell> {
    @Override
    public int compareTo(Cell other) {
        return col != other.col ? Integer.compare(col, other.col) : Integer.compare(row, other.row);
    }

    @Override
    public String toString() {
        return "(" + col + "," + row + ")";
    }
}
