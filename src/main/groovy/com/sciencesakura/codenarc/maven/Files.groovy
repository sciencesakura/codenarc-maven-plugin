// SPDX-License-Identifier: Apache-2.0

package com.sciencesakura.codenarc.maven

import groovy.transform.CompileStatic

/**
 * Utility class for file operations.
 *
 * @author sciencesakura
 */
@CompileStatic
final class Files {

  private Files() {
  }

  /**
   * Tests whether the given path is a directory.
   *
   * @param path the path to check
   * @return {@code true} if the path is a directory, {@code false} otherwise
   */
  static boolean isDirectory(String path) {
    new File(path).directory
  }
}
