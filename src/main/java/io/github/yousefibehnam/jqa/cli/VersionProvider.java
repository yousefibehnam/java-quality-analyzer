package io.github.yousefibehnam.jqa.cli;

import picocli.CommandLine.IVersionProvider;

/** Reads the version from the jar manifest; "dev" when running from classes. */
final class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        String version = Main.class.getPackage().getImplementationVersion();
        return new String[] {"jqa " + (version == null ? "dev" : version)};
    }
}
