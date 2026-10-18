package DSA1.src.daisyday.ui.components;

/** Picks whole-number axis ticks (0, step, 2*step ... top) for a chart. */
final class ChartAxis {

    final int step;
    final int top;

    ChartAxis(int maxValue) {
        int max = Math.max(3, maxValue);
        this.step = Math.max(1, (int) Math.ceil(max / 4.0));
        this.top = step * (int) Math.ceil(max / (double) step);
    }
}
