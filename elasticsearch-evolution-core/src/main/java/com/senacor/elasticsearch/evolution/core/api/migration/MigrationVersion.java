/*
 * Copyright 2010-2018 Boxfuse GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.senacor.elasticsearch.evolution.core.api.migration;

import com.senacor.elasticsearch.evolution.core.api.MigrationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.senacor.elasticsearch.evolution.core.internal.utils.AssertionUtils.requireNotBlank;

/**
 * A version of a migration.
 */
public final class MigrationVersion implements Comparable<MigrationVersion> {
    /**
     * Compiled pattern for matching proper version format
     */
    private static final Pattern splitPattern = Pattern.compile("\\.(?=\\d)");

    /**
     * The individual parts this version string is composed of. Ex. 1.2.3.4.0 -> [1, 2, 3, 4, 0]
     */
    private final List<Integer> versionParts;

    /**
     * The printable text to represent the version.
     */
    private final String displayText;

    /**
     * Factory for creating a MigrationVersion from a version String
     *
     * @param version The version String like
     * @return The MigrationVersion
     */
    @SuppressWarnings("ConstantConditions")
    public static MigrationVersion fromVersion(String version) {
        return new MigrationVersion(version);
    }

    /**
     * Creates a Version using this version string.
     *
     * @param version The version in one of the following formats: 6, 6.0, 005, 1.2.3.4, 201004200021, 6_0.
     */
    public MigrationVersion(String version) {
        String normalizedVersion = requireNotBlank(version, "version must not be blank")
                .replace('_', '.');
        this.versionParts = tokenize(normalizedVersion);
        this.displayText = versionParts.stream()
                .map(Object::toString)
                .collect(Collectors.joining("."));
    }

    /**
     * @return The textual representation of the version.
     */
    @Override
    public String toString() {
        return displayText;
    }

    /**
     * @return Numeric version as String
     */
    public String getVersion() {
        return displayText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MigrationVersion version1 = (MigrationVersion) o;

        return compareTo(version1) == 0;
    }

    @Override
    public int hashCode() {
        return versionParts == null ? 0 : versionParts.hashCode();
    }

    /**
     * Convenience method for quickly checking whether this version is at least as new as this other version.
     *
     * @param otherVersion The other version.
     * @return {@code true} if this version is equal or newer, {@code false} if it is older.
     */
    public boolean isAtLeast(String otherVersion) {
        return compareTo(MigrationVersion.fromVersion(otherVersion)) >= 0;
    }

    /**
     * Convenience method for quickly checking whether this version is newer than this other version.
     *
     * @param otherVersion The other version.
     * @return {@code true} if this version is newer, {@code false} if it is not.
     */
    public boolean isNewerThan(String otherVersion) {
        return compareTo(MigrationVersion.fromVersion(otherVersion)) > 0;
    }

    /**
     * Convenience method for quickly checking whether this major version is newer than this other major version.
     *
     * @param otherVersion The other version.
     * @return {@code true} if this major version is newer, {@code false} if it is not.
     */
    public boolean isMajorNewerThan(String otherVersion) {
        return isMajorNewerThan(MigrationVersion.fromVersion(otherVersion));
    }

    /**
     * Convenience method for quickly checking whether this major version is newer than this other major version.
     *
     * @param otherVersion The other version.
     * @return {@code true} if this major version is newer, {@code false} if it is not.
     */
    public boolean isMajorNewerThan(MigrationVersion otherVersion) {
        return getMajor().compareTo(otherVersion.getMajor()) > 0;
    }

    /**
     * @return The major version.
     */
    public Integer getMajor() {
        return versionParts.get(0);
    }

    /**
     * @return The major version as a string.
     */
    public String getMajorAsString() {
        return versionParts.get(0).toString();
    }

    /**
     * @return The minor version as a string.
     */
    public String getMinorAsString() {
        if (versionParts.size() == 1) {
            return "0";
        }
        return versionParts.get(1).toString();
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(MigrationVersion o) {
        if (o == null) {
            return 1;
        }

        final List<Integer> parts1 = versionParts;
        final List<Integer> parts2 = o.versionParts;
        int largestNumberOfParts = Math.max(parts1.size(), parts2.size());
        for (int i = 0; i < largestNumberOfParts; i++) {
            final int compared = getOrZero(parts1, i).compareTo(getOrZero(parts2, i));
            if (compared != 0) {
                return compared;
            }
        }
        return 0;
    }

    private Integer getOrZero(List<Integer> elements, int i) {
        return i < elements.size() ? elements.get(i) : 0;
    }

    /**
     * Splits this string into list of Long
     *
     * @param str The string to split.
     * @return The resulting array.
     */
    private List<Integer> tokenize(String str) {
        List<Integer> parts = new ArrayList<>();
        try {
            for (String part : splitPattern.split(str)) {
                parts.add(Integer.valueOf(part));
            }
        } catch (NumberFormatException e) {
            throw new MigrationException(
                    "Invalid version containing non-numeric characters. Only 0..9 and . are allowed. Invalid version: "
                            + str);
        }
        for (int i = parts.size() - 1; i > 0; i--) {
            if (!parts.get(i).equals(0)) {
                break;
            }
            parts.remove(i);
        }
        return parts;
    }
}