package ru.hh.school.homework;

import java.nio.file.Path;
import java.util.List;

// Изначально использовал Map.Entry сознательно,
// чтобы дополнительно поработать со встоенной библиотекой
public record DirectoryInfo(Path path, List<String> fileNames) {
    public boolean isEmpty() {
        return fileNames.isEmpty();
    }
}
