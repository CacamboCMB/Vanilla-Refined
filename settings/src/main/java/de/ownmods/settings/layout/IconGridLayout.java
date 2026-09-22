package de.ownmods.settings.layout;

import de.ownmods.settings.api.OptionIcon;
import de.ownmods.settings.api.ToggleOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Pure layout math, shared by the icon screen and dependency-free regression tests. */
public final class IconGridLayout {
    public static final int SIZE = 36;
    public static final int GAP = 6;
    public static final int ROW_HEIGHT = 58;
    public static final int TOP = 50;
    private IconGridLayout() { }
    public record Row(String groupKey, List<ToggleOption> options) {
        public Row { options = List.copyOf(options); }
    }
    public record Layout(int columns, int rowsPerPage, int page, int pageCount, List<Row> rows) {
        public Layout { rows = List.copyOf(rows); }
        public List<Row> visibleRows() {
            int start = page * rowsPerPage;
            return rows.subList(start, Math.min(rows.size(), start + rowsPerPage));
        }
    }
    public static Layout create(List<ToggleOption> options, Map<String, OptionIcon> icons,
                                int contentWidth, int screenHeight, int requestedPage) {
        int columns = Math.max(1, Math.min(7, (contentWidth + GAP) / (SIZE + GAP)));
        int rowsPerPage = Math.max(1, (screenHeight - TOP - 72) / ROW_HEIGHT);
        var groups = new LinkedHashMap<String, List<ToggleOption>>();
        for (var option : options) {
            var icon = icons.get(option.key());
            if (icon == null) throw new IllegalArgumentException("No icon for " + option.key());
            groups.computeIfAbsent(icon.groupKey(), unused -> new ArrayList<>()).add(option);
        }
        var rows = new ArrayList<Row>();
        groups.forEach((group, members) -> {
            for (int start = 0; start < members.size(); start += columns)
                rows.add(new Row(group, members.subList(start, Math.min(members.size(), start + columns))));
        });
        int pageCount = Math.max(1, (rows.size() + rowsPerPage - 1) / rowsPerPage);
        int page = Math.max(0, Math.min(requestedPage, pageCount - 1));
        return new Layout(columns, rowsPerPage, page, pageCount, rows);
    }
}
