package com.sonatype.nexus.repository.nuget.internal;

/**
 * Indicates a malformed Nuget package.
 *
 * @since 3.0
 */
public class NugetPackageException
    extends Exception
{
  public NugetPackageException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
