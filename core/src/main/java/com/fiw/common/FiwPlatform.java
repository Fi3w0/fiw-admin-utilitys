package com.fiw.common;

import java.nio.file.Path;

public interface FiwPlatform {
    String loaderName();

    Path configDirectory();

    void info(String message);

    void warn(String message);
}
