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
   * Returns the extension of the given file.
   *
   * @param file the file to get the extension of
   * @return the extension of the file, or an empty string if it has no extension
   */
  static String extension(File file) {
    // when upgrade to Groovy 5, this can be replaced with `File#getExtension()`.
    def name = file.name
    def index = name.lastIndexOf('.')
    index < 0 ? '' : name.substring(index + 1).toLowerCase()
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
